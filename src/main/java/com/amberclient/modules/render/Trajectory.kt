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
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.thrown.SnowballEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import net.minecraft.world.RaycastContext
import org.lwjgl.opengl.GL11
import java.awt.Color

class Trajectory : Module("Trajectory", "Renders the trajectory of projectiles", ModuleCategory.RENDER), ConfigurableModule {

    enum class LineOrigin {
        LEFT, AUTO, RIGHT
    }

    private var lineVisibility = true
    private var boxVisibility = true
    private var approxBoxVisibility = true
    private var arrowTrajectory = true
    private var lineOrigin = LineOrigin.AUTO
    private var lineWidth = 2.0f
    private var maxIterations = 100

    // Color settings
    private var trajectoryColor = Color(255, 0, 0, 150)
    private var hitBoxColor = Color(255, 255, 0, 100)
    private var approxBoxColor = Color(255, 165, 0, 80)

    private val simpleItems = setOf(Items.SNOWBALL, Items.ENDER_PEARL, Items.EGG)
    private val complexItems = setOf(Items.BOW, Items.CROSSBOW)

    private var renderCallback: WorldRenderEvents.AfterEntities? = null
    private var tempEntity: SnowballEntity? = null

    private data class ColorCache(val r: Float, val g: Float, val b: Float, val a: Float) {
        constructor(color: Color) : this(
            color.red / 255.0f,
            color.green / 255.0f,
            color.blue / 255.0f,
            color.alpha / 255.0f
        )
    }

    private data class TrajectoryParams(
        val speed: Float,
        val gravity: Float,
        val drag: Float,
        val mainHand: Boolean
    )

    override fun getSettings(): List<ModuleSettings> {
        return listOf(
            ModuleSettings("Line Visibility", "Show the trajectory line", lineVisibility),
            ModuleSettings("Box Visibility", "Show the hit box", boxVisibility),
            ModuleSettings("Approx Box Visibility", "Show the approximate hit box", approxBoxVisibility),
            ModuleSettings("Max Iterations", "Maximum calculation iterations", maxIterations.toDouble(), 50.0, 200.0, 10.0),
            ModuleSettings("Line Width", "Width of the trajectory line", lineWidth.toDouble(), 1.0, 10.0, 0.5),
            ModuleSettings("Line Origin", "Origin of the trajectory line", lineOrigin),
            ModuleSettings("Trajectory Color", "Color of the trajectory line", trajectoryColor),
            ModuleSettings("Hit Box Color", "Color of the hit box", hitBoxColor),
            ModuleSettings("Approx Box Color", "Color of the approximate hit box", approxBoxColor)
        )
    }

    override fun onSettingChanged(setting: ModuleSettings) {
        when (setting.name) {
            "Line Visibility" -> lineVisibility = setting.getBooleanValue()
            "Box Visibility" -> boxVisibility = setting.getBooleanValue()
            "Approx Box Visibility" -> approxBoxVisibility = setting.getBooleanValue()
            "Max Iterations" -> maxIterations = setting.getDoubleValue().toInt()
            "Line Width" -> lineWidth = setting.getDoubleValue().toFloat()
            "Line Origin" -> lineOrigin = setting.getEnumValue<LineOrigin>()
            "Trajectory Color" -> trajectoryColor = setting.getColorValue()
            "Hit Box Color" -> hitBoxColor = setting.getColorValue()
            "Approx Box Color" -> approxBoxColor = setting.getColorValue()
        }
    }

    override fun onEnable() {
        if (renderCallback == null) {
            renderCallback = WorldRenderEvents.AfterEntities { context ->
                if (enabled && client.player != null && client.world != null) {
                    renderTrajectory(context.matrixStack())
                }
            }
            WorldRenderEvents.AFTER_ENTITIES.register(renderCallback!!)
        }
    }

    private fun renderTrajectory(matrixStack: MatrixStack?) {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return
        val world = mc.world ?: return
        val camera = mc.gameRenderer.camera

        val mainHandStack = player.mainHandStack
        val offHandStack = player.offHandStack
        val mainHand = isProjectileItem(mainHandStack.item)
        val offHand = isProjectileItem(offHandStack.item)

        if (!mainHand && !offHand) return

        val currentStack = if (mainHand) mainHandStack else offHandStack
        val trajectoryParams = calculateTrajectoryParams(player, currentStack.item, mainHand) ?: return

        matrixStack?.push()
        val cameraPos = camera.pos
        matrixStack?.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)

        setupRendering()

        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

        renderTrajectoryPath(bufferBuilder, player, world, trajectoryParams, cameraPos, mainHand)

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())
        resetRendering()
        matrixStack?.pop()
    }

    private fun isProjectileItem(item: Item): Boolean =
        item in simpleItems || (item in complexItems && arrowTrajectory)

    private fun calculateTrajectoryParams(player: PlayerEntity, item: Item, mainHand: Boolean): TrajectoryParams? {
        return when {
            item in simpleItems -> TrajectoryParams(1.5f, 0.03f, 0.99f, mainHand)
            item in complexItems && arrowTrajectory -> {
                val bowMultiplier = calculateBowPower(player)
                TrajectoryParams(bowMultiplier * 3.0f, 0.05f, 0.99f, mainHand)
            }
            else -> null
        }
    }

    private fun calculateBowPower(player: PlayerEntity): Float {
        val useTime = 72000 - player.itemUseTimeLeft
        val power = useTime / 20.0f
        return ((power * power + power * 2.0f) / 3.0f).coerceAtMost(1.0f)
    }

    private fun renderTrajectoryPath(
        bufferBuilder: BufferBuilder,
        player: PlayerEntity,
        world: net.minecraft.world.World,
        params: TrajectoryParams,
        cameraPos: Vec3d,
        usingMainHand: Boolean
    ) {
        if (tempEntity == null) {
            tempEntity = SnowballEntity(world, player, ItemStack(Items.SNOWBALL))
        }

        var entityPosition = getStartPosition(player, usingMainHand)
        var entityVelocity = getInitialVelocity(player, params.speed)
        var prevPosition = entityPosition

        val dragDouble = params.drag.toDouble()
        val gravityDouble = params.gravity.toDouble()

        repeat(maxIterations) { _ ->
            val nextPosition = entityPosition.add(entityVelocity)

            val hitResult = world.raycast(
                RaycastContext(
                    entityPosition,
                    nextPosition,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    tempEntity!!
                )
            )

            if (hitResult.type != HitResult.Type.MISS) {
                val hitPos = hitResult.pos
                if (lineVisibility) {
                    addLineVertices(bufferBuilder, prevPosition, hitPos, cameraPos, ColorCache(trajectoryColor))
                }
                renderHitBoxes(bufferBuilder, hitPos, cameraPos, player.pos.distanceTo(hitPos))
                return
            }

            if (lineVisibility) {
                addLineVertices(bufferBuilder, prevPosition, entityPosition, cameraPos, ColorCache(trajectoryColor))
            }

            prevPosition = entityPosition
            entityPosition = nextPosition
            entityVelocity = Vec3d(
                entityVelocity.x * dragDouble,
                entityVelocity.y * dragDouble - gravityDouble,
                entityVelocity.z * dragDouble
            )
        }
    }

    private fun getStartPosition(player: PlayerEntity, usingMainHand: Boolean): Vec3d {
        val playerPos = Vec3d(player.x, player.y + 1.5, player.z)

        val playerSide = when (lineOrigin) {
            LineOrigin.LEFT -> 1
            LineOrigin.RIGHT -> -1
            LineOrigin.AUTO -> if (usingMainHand) 1 else -1
        }

        val adjustedSide = if (player.mainArm.id == 0) -playerSide else playerSide
        val yawRadians = Math.toRadians((player.yaw + 90).toDouble())

        val offsetX = adjustedSide * 0.3 * MathHelper.sin(yawRadians.toFloat())
        val offsetZ = adjustedSide * 0.3 * MathHelper.cos(yawRadians.toFloat())

        return playerPos.add(offsetX, 0.0, offsetZ)
    }

    private fun getInitialVelocity(player: PlayerEntity, speed: Float): Vec3d {
        val pitchRad = player.pitch * 0.017453292f
        val yawRad = player.yaw * 0.017453292f

        val cosPitch = MathHelper.cos(pitchRad)

        val entityVelX = -MathHelper.sin(yawRad) * cosPitch
        val entityVelY = -MathHelper.sin(pitchRad)
        val entityVelZ = MathHelper.cos(yawRad) * cosPitch

        return Vec3d(entityVelX.toDouble(), entityVelY.toDouble(), entityVelZ.toDouble())
            .normalize()
            .multiply(speed.toDouble())
    }

    private fun renderHitBoxes(bufferBuilder: BufferBuilder, hitPos: Vec3d, cameraPos: Vec3d, distance: Double) {
        val relativePos = hitPos.subtract(cameraPos)

        if (boxVisibility) {
            addBoxVertices(bufferBuilder, relativePos, 0.5, ColorCache(hitBoxColor))
        }

        if (approxBoxVisibility) {
            val approxSize = if (distance > 30) distance / 70 else 0.5
            addBoxVertices(bufferBuilder, relativePos, approxSize, ColorCache(approxBoxColor))
        }
    }

    private fun addLineVertices(
        bufferBuilder: BufferBuilder,
        start: Vec3d,
        end: Vec3d,
        cameraPos: Vec3d,
        color: ColorCache
    ) {
        val startRel = start.subtract(cameraPos)
        val endRel = end.subtract(cameraPos)

        bufferBuilder.vertex(startRel.x.toFloat(), startRel.y.toFloat(), startRel.z.toFloat())
            .color(color.r, color.g, color.b, color.a)
        bufferBuilder.vertex(endRel.x.toFloat(), endRel.y.toFloat(), endRel.z.toFloat())
            .color(color.r, color.g, color.b, color.a)
    }

    private fun addBoxVertices(
        bufferBuilder: BufferBuilder,
        center: Vec3d,
        size: Double,
        color: ColorCache
    ) {
        val x1 = (center.x - size).toFloat()
        val y1 = (center.y - size).toFloat()
        val z1 = (center.z - size).toFloat()
        val x2 = (center.x + size).toFloat()
        val y2 = (center.y + size).toFloat()
        val z2 = (center.z + size).toFloat()

        val r = color.r
        val g = color.g
        val b = color.b
        val a = color.a

        // Bottom face
        addQuadEdges(bufferBuilder, x1, y1, z1, x2, z2, r, g, b, a)
        // Top face
        addQuadEdges(bufferBuilder, x1, y2, z1, x2, z2, r, g, b, a)
        // Vertical edges
        addVerticalEdges(bufferBuilder, x1, y1, z1, x2, y2, z2, r, g, b, a)
    }

    private fun addQuadEdges(
        bufferBuilder: BufferBuilder,
        x1: Float, y: Float, z1: Float,
        x2: Float, z2: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val vertices = floatArrayOf(x1, y, z1, x2, y, z1, x2, y, z2, x1, y, z2)
        for (i in vertices.indices step 3) {
            val nextI = if (i + 3 < vertices.size) i + 3 else 0
            bufferBuilder.vertex(vertices[i], vertices[i + 1], vertices[i + 2]).color(r, g, b, a)
            bufferBuilder.vertex(vertices[nextI], vertices[nextI + 1], vertices[nextI + 2]).color(r, g, b, a)
        }
    }

    private fun addVerticalEdges(
        bufferBuilder: BufferBuilder,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        val corners = floatArrayOf(x1, z1, x2, z1, x2, z2, x1, z2)
        for (i in corners.indices step 2) {
            bufferBuilder.vertex(corners[i], y1, corners[i + 1]).color(r, g, b, a)
            bufferBuilder.vertex(corners[i], y2, corners[i + 1]).color(r, g, b, a)
        }
    }

    private fun setupRendering() {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()
        RenderSystem.disableDepthTest()
        RenderSystem.lineWidth(lineWidth)

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
    }

    private fun resetRendering() {
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        RenderSystem.enableCull()
        RenderSystem.enableDepthTest()
        RenderSystem.disableBlend()
        RenderSystem.lineWidth(1.0f)
    }
}