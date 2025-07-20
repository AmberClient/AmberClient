package com.amberclient.modules.render

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import com.amberclient.utils.module.ModuleSettings
import com.mojang.blaze3d.systems.ProjectionType
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
import org.joml.Matrix4f
import org.joml.Vector4f
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
            ModuleSettings("Tracer Origin", "Where tracers start from", tracerOrigin)
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

        when (tracerOrigin) {
            TracerOrigin.BODY -> render3DTracers(matrixStack, player, camera, entities, tickDelta)
            TracerOrigin.OFFSCREEN, TracerOrigin.CURSOR -> render2DTracers(matrixStack, player, camera, entities, tickDelta)
        }
    }

    private fun render3DTracers(matrixStack: MatrixStack?, player: PlayerEntity, camera: net.minecraft.client.render.Camera, entities: List<Entity>, tickDelta: Float) {
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

        val startPos = get3DTracerStartPosition(player, camera, tickDelta)

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

    private fun render2DTracers(matrixStack: MatrixStack?, player: PlayerEntity, camera: net.minecraft.client.render.Camera, entities: List<Entity>, tickDelta: Float) {
        val mc = MinecraftClient.getInstance()
        val window = mc.window
        val screenWidth = window.scaledWidth.toDouble()
        val screenHeight = window.scaledHeight.toDouble()

        val originalModelView = Matrix4f(RenderSystem.getModelViewMatrix())
        val originalProjection = Matrix4f(RenderSystem.getProjectionMatrix())

        matrixStack?.push()
        matrixStack?.loadIdentity()

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()
        RenderSystem.disableDepthTest()
        RenderSystem.lineWidth(lineWidth)

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)

        val orthoMatrix = Matrix4f().setOrtho(0.0f, screenWidth.toFloat(), screenHeight.toFloat(), 0.0f, -1000.0f, 1000.0f)
        RenderSystem.setProjectionMatrix(orthoMatrix, ProjectionType.ORTHOGRAPHIC)

        RenderSystem.getModelViewMatrix().identity()

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

        val startPos = get2DTracerStartPosition(screenWidth, screenHeight)

        entities.forEach { entity ->
            if (entity is LivingEntity && entity != player) {
                val distance = player.distanceTo(entity)
                val color = getEntityColor(entity, distance)
                val entityPos = getInterpolatedEntityPosition(entity, tickDelta)

                val entityCenter = Vec3d(
                    entityPos.x,
                    entityPos.y + entity.height / 2.0,
                    entityPos.z
                )

                val screenPos = worldToScreen(entityCenter, camera, screenWidth, screenHeight, originalModelView, originalProjection)
                if (screenPos != null) {
                    draw2DLine(bufferBuilder, startPos, screenPos, color)
                }
            }
        }

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())

        RenderSystem.setProjectionMatrix(originalProjection, ProjectionType.PERSPECTIVE)
        RenderSystem.getModelViewMatrix().set(originalModelView)

        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        RenderSystem.enableCull()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.lineWidth(1.0f)

        matrixStack?.pop()
    }

    private fun get3DTracerStartPosition(player: PlayerEntity, camera: net.minecraft.client.render.Camera, tickDelta: Float): Vec3d {
        val cameraPos = camera.pos
        val playerPos = Vec3d(
            MathHelper.lerp(tickDelta.toDouble(), player.lastRenderX, player.x),
            MathHelper.lerp(tickDelta.toDouble(), player.lastRenderY, player.y),
            MathHelper.lerp(tickDelta.toDouble(), player.lastRenderZ, player.z)
        )

        return Vec3d(
            playerPos.x - cameraPos.x + 0.1,
            playerPos.y + player.height * 0.5 - cameraPos.y,
            playerPos.z - cameraPos.z + 0.1
        )
    }

    private fun get2DTracerStartPosition(screenWidth: Double, screenHeight: Double): Vec3d {
        return when (tracerOrigin) {
            TracerOrigin.OFFSCREEN -> Vec3d(screenWidth / 2.0, 0.0, 0.0)
            TracerOrigin.CURSOR -> Vec3d(screenWidth / 2.0, screenHeight / 2.0, 0.0)
            else -> Vec3d(screenWidth / 2.0, 0.0, 0.0)
        }
    }

    private fun worldToScreen(worldPos: Vec3d, camera: Camera, screenWidth: Double, screenHeight: Double, modelViewMatrix: Matrix4f, projectionMatrix: Matrix4f): Vec3d? {
        val cameraPos = camera.pos
        val relativePos = worldPos.subtract(cameraPos)

        val worldVec = Vector4f(
            relativePos.x.toFloat(),
            relativePos.y.toFloat(),
            relativePos.z.toFloat(),
            1.0f
        )

        worldVec.mul(modelViewMatrix)
        worldVec.mul(projectionMatrix)

        if (worldVec.w <= 0.0f) return null

        worldVec.div(worldVec.w)

        val screenX = (worldVec.x + 1.0f) * 0.5f * screenWidth.toFloat()
        val screenY = (1.0f - worldVec.y) * 0.5f * screenHeight.toFloat()

        if (screenX.isNaN() || screenY.isNaN() || screenX.isInfinite() || screenY.isInfinite()) {
            return null
        }

        return Vec3d(screenX.toDouble(), screenY.toDouble(), 0.0)
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

    private fun draw2DLine(bufferBuilder: BufferBuilder, start: Vec3d, end: Vec3d, color: Color) {
        val red = color.red / 255.0f
        val green = color.green / 255.0f
        val blue = color.blue / 255.0f
        val alpha = color.alpha / 255.0f

        bufferBuilder.vertex(start.x.toFloat(), start.y.toFloat(), 0.0f)
            .color(red, green, blue, alpha)

        bufferBuilder.vertex(end.x.toFloat(), end.y.toFloat(), 0.0f)
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
        val baseColor = when (entity) {
            is PlayerEntity -> playerColor
            is Monster -> hostileColor
            is PassiveEntity -> passiveColor
            else -> defaultColor
        }

        return if (useDistanceTransparency) {
            val normalizedDistance = (distance / maxDistance).coerceIn(0.0f, 1.0f)
            val alpha = (255 * normalizedDistance).toInt().coerceIn(51, 255)

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