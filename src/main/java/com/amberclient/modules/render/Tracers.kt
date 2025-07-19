package com.amberclient.modules.render

import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
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

class Tracers : Module("Tracers", "Draws lines towards entities", ModuleCategory.RENDER) {

    private var showPlayers = true
    private var showHostileMobs = true
    private var showPassiveMobs = false
    private var useDistanceColor = true
    private var maxDistance = 128.0f
    private var lineWidth = 2.0f

    private val playerColor = Color(0, 160, 255)
    private val hostileColor = Color(255, 0, 0)
    private val passiveColor = Color(0, 255, 0)
    private val defaultColor = Color(255, 255, 255)

    private var renderCallback: WorldRenderEvents.AfterEntities? = null

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

        val playerPos = Vec3d(
            MathHelper.lerp(tickDelta.toDouble(), player.lastRenderX, player.x),
            MathHelper.lerp(tickDelta.toDouble(), player.lastRenderY, player.y),
            MathHelper.lerp(tickDelta.toDouble(), player.lastRenderZ, player.z)
        )
        val startPos = Vec3d(
            playerPos.x - cameraPos.x + 0.1,
            playerPos.y + player.height * 0.5 - cameraPos.y,
            playerPos.z - cameraPos.z + 0.1
        )


        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

        entities.forEach { entity ->
            if (entity is LivingEntity && entity != player) {
                val color = getEntityColor(entity, player.distanceTo(entity))
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

    private fun drawLine(bufferBuilder: BufferBuilder, start: Vec3d, end: Vec3d, color: Color) {
        val red = color.red / 255.0f
        val green = color.green / 255.0f
        val blue = color.blue / 255.0f
        val alpha = color.alpha / 255.0f

        // Point de départ
        bufferBuilder.vertex(start.x.toFloat(), start.y.toFloat(), start.z.toFloat())
            .color(red, green, blue, alpha)

        // Point d'arrivée
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
        return when {
            useDistanceColor -> {
                val normalizedDistance = (distance / maxDistance).coerceIn(0.0f, 1.0f)
                val hue = (1.0f - normalizedDistance) * 120.0f / 360.0f
                Color(Color.HSBtoRGB(hue, 1.0f, 1.0f))
            }
            entity is PlayerEntity -> playerColor
            entity is Monster -> hostileColor
            entity is PassiveEntity -> passiveColor
            else -> defaultColor
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

    fun setShowPlayers(show: Boolean) { showPlayers = show }
    fun setShowHostileMobs(show: Boolean) { showHostileMobs = show }
    fun setShowPassiveMobs(show: Boolean) { showPassiveMobs = show }
    fun setUseDistanceColor(use: Boolean) { useDistanceColor = use }
    fun setMaxDistance(distance: Float) { maxDistance = distance.coerceIn(1.0f, 512.0f) }
    fun setLineWidth(width: Float) { lineWidth = width.coerceIn(0.5f, 10.0f) }
}