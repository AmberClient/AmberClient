package com.amberclient.modules.render

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import com.amberclient.utils.module.ModuleSettings
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color

class Tracers : Module("Tracers", "Draws lines towards entities", ModuleCategory.RENDER), ConfigurableModule {

    enum class TracerOrigin(val displayName: String) {
        BODY("Body"),
        OFFSCREEN("Offscreen"),
        CURSOR("Cursor")
    }

    private var showPlayers = true
    private var showHostileMobs = true
    private var showPassiveMobs = false
    private var useDistanceTransparency = true
    private var maxDistance = 128.0f
    private var lineWidth = 2.0f
    private var tracerOrigin = TracerOrigin.BODY

    private val playerColor = Color(255, 165, 0)
    private val hostileColor = Color(207, 48, 48)
    private val passiveColor = Color(207, 48, 48)
    private val defaultColor = Color(255, 255, 255)

    private var renderCallback: WorldRenderEvents.AfterEntities? = null

    override fun getSettings(): List<ModuleSettings> {
        return listOf(
            ModuleSettings("Show Players", "Show tracers to players", showPlayers),
            ModuleSettings("Show Hostile Mobs", "Show tracers to hostile mobs", showHostileMobs),
            ModuleSettings("Show Passive Mobs", "Show tracers to passive mobs", showPassiveMobs),
            ModuleSettings("Use Distance Transparency", "Make tracers more transparent with distance", useDistanceTransparency),
            ModuleSettings("Max Distance", "Maximum distance for tracers", maxDistance.toDouble(), 1.0, 512.0, 1.0),
            ModuleSettings("Line Width", "Width of tracer lines", lineWidth.toDouble(), 0.5, 10.0, 0.1),
            ModuleSettings("Tracer Origin", "Where tracers start from (cursor doesn't work)", tracerOrigin)
        )
    }

    override fun onSettingChanged(setting: ModuleSettings) {
        when (setting.name) {
            "Show Players" -> showPlayers = setting.getBooleanValue()
            "Show Hostile Mobs" -> showHostileMobs = setting.getBooleanValue()
            "Show Passive Mobs" -> showPassiveMobs = setting.getBooleanValue()
            "Use Distance Transparency" -> useDistanceTransparency = setting.getBooleanValue()
            "Max Distance" -> maxDistance = setting.getDoubleValue().toFloat()
            "Line Width" -> lineWidth = setting.getDoubleValue().toFloat()
            "Tracer Origin" -> tracerOrigin = setting.getEnumValue<TracerOrigin>()
        }
    }

    override fun onEnable() {
        renderCallback = WorldRenderEvents.AfterEntities { context ->
            if (client.player != null && client.world != null) {
                renderTracers(context.matrixStack(), context.tickCounter().getTickDelta(true))
            }
        }
        WorldRenderEvents.AFTER_ENTITIES.register(renderCallback!!)
    }

    override fun onDisable() {
        renderCallback = null
    }

    private fun renderTracers(matrixStack: MatrixStack?, tickDelta: Float) {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return
        val world = mc.world ?: return
        val camera = mc.gameRenderer.camera

        val entities = world.entities.filter { entity ->
            shouldRenderTrace(entity) && player.distanceTo(entity) <= maxDistance
        }

        if (entities.isEmpty()) return

        matrixStack?.push()

        val cameraPos = camera.pos
        matrixStack?.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()
        RenderSystem.disableDepthTest()
        RenderSystem.lineWidth(lineWidth)

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)

        val startPos = getTracerStartPosition(player, camera, tickDelta)

        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

        entities.forEach { entity ->
            if (entity is LivingEntity && entity != player) {
                val distance = player.distanceTo(entity)
                val color = getEntityColor(entity, distance)
                val entityPos = getInterpolatedEntityPosition(entity, tickDelta)

                val entityCenter = Vec3d(
                    entityPos.x - cameraPos.x,
                    entityPos.y + entity.height / 2.0 - cameraPos.y,
                    entityPos.z - cameraPos.z
                )

                drawLine(bufferBuilder, startPos, entityCenter, color)
            }
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())

        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        RenderSystem.enableCull()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.lineWidth(1.0f)

        matrixStack?.pop()
    }

    private fun getTracerStartPosition(player: PlayerEntity, camera: net.minecraft.client.render.Camera, tickDelta: Float): Vec3d {
        val cameraPos = camera.pos

        return when (tracerOrigin) {
            TracerOrigin.BODY -> {
                val playerPos = Vec3d(
                    MathHelper.lerp(tickDelta.toDouble(), player.lastRenderX, player.x),
                    MathHelper.lerp(tickDelta.toDouble(), player.lastRenderY, player.y),
                    MathHelper.lerp(tickDelta.toDouble(), player.lastRenderZ, player.z)
                )
                Vec3d(
                    playerPos.x - cameraPos.x + 0.1,
                    playerPos.y + player.height * 0.5 - cameraPos.y,
                    playerPos.z - cameraPos.z + 0.1
                )
            }
            TracerOrigin.OFFSCREEN -> {
                Vec3d(0.0, -500.0, 0.0)
            }
            TracerOrigin.CURSOR -> {
                Vec3d(0.0, 0.0, 0.0)
            }
        }
    }

    private fun drawLine(bufferBuilder: BufferBuilder, start: Vec3d, end: Vec3d, color: Color) {
        val red = color.red / 255.0f
        val green = color.green / 255.0f
        val blue = color.blue / 255.0f
        val alpha = color.alpha / 255.0f

        bufferBuilder.vertex(start.x.toFloat(), start.y.toFloat(), start.z.toFloat())
            .color(red, green, blue, alpha)

        bufferBuilder.vertex(end.x.toFloat(), end.y.toFloat(), end.z.toFloat())
            .color(red, green, blue, alpha)
    }

    private fun getInterpolatedEntityPosition(entity: Entity, tickDelta: Float): Vec3d {
        return Vec3d(
            MathHelper.lerp(tickDelta.toDouble(), entity.lastRenderX, entity.x),
            MathHelper.lerp(tickDelta.toDouble(), entity.lastRenderY, entity.y),
            MathHelper.lerp(tickDelta.toDouble(), entity.lastRenderZ, entity.z)
        )
    }

    private fun getEntityColor(entity: LivingEntity, distance: Float): Color {
        val baseColor = when {
            entity is PlayerEntity -> playerColor
            entity is Monster -> hostileColor
            entity is PassiveEntity -> passiveColor
            else -> defaultColor
        }

        return if (useDistanceTransparency) {
            val normalizedDistance = (distance / maxDistance).coerceIn(0.0f, 1.0f)
            val alpha = (255 * (1.0f - normalizedDistance * 0.8f)).toInt().coerceIn(51, 255)

            Color(baseColor.red, baseColor.green, baseColor.blue, alpha)
        } else {
            baseColor
        }
    }

    private fun shouldRenderTrace(entity: Entity): Boolean {
        return when {
            entity is PlayerEntity && entity != client.player -> showPlayers
            entity is Monster -> showHostileMobs
            entity is PassiveEntity -> showPassiveMobs
            else -> false
        }
    }
}