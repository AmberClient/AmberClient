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

    enum class LineOrigin() {
        LEFT(),
        AUTO(),
        RIGHT()
    }

    private var lineVisibility = true
    private var boxVisibility = true
    private var approxBoxVisibility = true
    private var arrowTrajectory = true
    private var lineOrigin = LineOrigin.AUTO
    private var lineWidth = 2.0f
    private var maxIterations = 100

    private val simpleItems = setOf(Items.SNOWBALL, Items.ENDER_PEARL, Items.EGG)
    private val complexItems = setOf(Items.BOW, Items.CROSSBOW)

    private val trajectoryColor = Color(255, 0, 0, 150)
    private val hitBoxColor = Color(255, 255, 0, 100)
    private val approxBoxColor = Color(255, 165, 0, 80)

    private var renderCallback: WorldRenderEvents.AfterEntities? = null

    override fun getSettings(): List<ModuleSettings> {
        return listOf(
            ModuleSettings("Line Visibility", "Show the trajectory line", lineVisibility),
            ModuleSettings("Box Visibility", "Show the hit box", boxVisibility),
            ModuleSettings("Approx Box Visibility", "Show the approximate hit box", approxBoxVisibility),
            ModuleSettings("Max Iterations", "Maximum calculation iterations", maxIterations.toDouble(), 50.0, 200.0, 10.0),
            ModuleSettings("Line Origin", "Origin of the trajectory line", lineOrigin),
        )
    }

    override fun onSettingChanged(setting: ModuleSettings) {
        when (setting.name) {
            "Line Visibility" -> lineVisibility = setting.getBooleanValue()
            "Box Visibility" -> boxVisibility = setting.getBooleanValue()
            "Approx Box Visibility" -> approxBoxVisibility = setting.getBooleanValue()
            "Max Iterations" -> maxIterations = setting.getDoubleValue().toInt()
            "Line Origin" -> lineOrigin = setting.getEnumValue<LineOrigin>()
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

        renderTrajectoryPath(
            bufferBuilder,
            player,
            world,
            trajectoryParams,
            cameraPos,
            mainHand
        )

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end())

        resetRendering()
        matrixStack?.pop()
    }

    private fun isProjectileItem(item: Item): Boolean {
        return simpleItems.contains(item) || (complexItems.contains(item) && arrowTrajectory)
    }

    private fun calculateTrajectoryParams(player: PlayerEntity, item: Item, mainHand: Boolean): TrajectoryParams? {
        val speed: Float
        val gravity: Float
        val drag: Float

        when {
            simpleItems.contains(item) -> {
                speed = 1.5f
                gravity = 0.03f
                drag = 0.99f
            }
            complexItems.contains(item) -> {
                if (!arrowTrajectory) return null
                val bowMultiplier = calculateBowPower(player)
                speed = bowMultiplier * 3.0f
                gravity = 0.05f
                drag = 0.99f
            }
            else -> return null
        }

        return TrajectoryParams(speed, gravity, drag, mainHand)
    }

    private fun calculateBowPower(player: PlayerEntity): Float {
        val useTime = 72000 - player.itemUseTimeLeft
        var power = useTime / 20.0f
        power = (power * power + power * 2.0f) / 3.0f
        return power.coerceAtMost(1.0f)
    }

    private fun renderTrajectoryPath(
        bufferBuilder: BufferBuilder,
        player: PlayerEntity,
        world: net.minecraft.world.World,
        params: TrajectoryParams,
        cameraPos: Vec3d,
        usingMainHand: Boolean
    ) {
        var entityPosition = getStartPosition(player, usingMainHand)
        var entityVelocity = getInitialVelocity(player, params.speed)

        val tempEntity = SnowballEntity(world, player, ItemStack(Items.SNOWBALL))

        var prevPosition = entityPosition

        var iterations = 0
        while (iterations < maxIterations) {
            val hitResult = world.raycast(
                RaycastContext(
                    entityPosition,
                    entityPosition.add(entityVelocity),
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    tempEntity
                )
            )

            if (hitResult.type != HitResult.Type.MISS) {
                val hitPos = hitResult.pos

                if (lineVisibility) {
                    drawTrajectoryLine(
                        bufferBuilder,
                        prevPosition.subtract(cameraPos),
                        hitPos.subtract(cameraPos),
                        trajectoryColor
                    )
                }

                renderHitBoxes(bufferBuilder, hitPos, cameraPos, player.pos.distanceTo(hitPos))
                break
            }

            if (lineVisibility) {
                drawTrajectoryLine(
                    bufferBuilder,
                    prevPosition.subtract(cameraPos),
                    entityPosition.subtract(cameraPos),
                    trajectoryColor
                )
            }

            prevPosition = entityPosition
            entityPosition = entityPosition.add(entityVelocity)
            entityVelocity = entityVelocity.multiply(params.drag.toDouble())
            entityVelocity = Vec3d(entityVelocity.x, entityVelocity.y - params.gravity, entityVelocity.z)

            iterations++
        }
    }

    private fun getStartPosition(player: PlayerEntity, usingMainHand: Boolean): Vec3d {
        val playerPos = Vec3d(player.x, player.y + 1.5, player.z)

        val playerSide = when (lineOrigin) {
            LineOrigin.LEFT -> -1
            LineOrigin.RIGHT -> 1
            LineOrigin.AUTO -> if (usingMainHand) 1 else -1
        }

        val adjustedSide = if (player.mainArm.id == 0) -playerSide else playerSide

        val offsetX = adjustedSide * 0.3 * MathHelper.sin(Math.toRadians((player.yaw + 90).toDouble()).toFloat())
        val offsetZ = adjustedSide * 0.3 * MathHelper.cos(Math.toRadians((player.yaw + 90).toDouble()).toFloat())

        return playerPos.add(offsetX, 0.0, offsetZ)
    }

    private fun getInitialVelocity(player: PlayerEntity, speed: Float): Vec3d {
        val pitch = player.pitch
        val yaw = player.yaw

        val entityVelX = -MathHelper.sin(yaw * 0.017453292f) * MathHelper.cos(pitch * 0.017453292f)
        val entityVelY = -MathHelper.sin(pitch * 0.017453292f)
        val entityVelZ = MathHelper.cos(yaw * 0.017453292f) * MathHelper.cos(pitch * 0.017453292f)

        return Vec3d(entityVelX.toDouble(), entityVelY.toDouble(), entityVelZ.toDouble())
            .normalize()
            .multiply(speed.toDouble())
    }

    private fun renderHitBoxes(bufferBuilder: BufferBuilder, hitPos: Vec3d, cameraPos: Vec3d, distance: Double) {
        val relativePos = hitPos.subtract(cameraPos)

        if (boxVisibility) {
            val defaultSize = 0.5
            drawBox(
                bufferBuilder,
                relativePos.x - defaultSize,
                relativePos.y - defaultSize,
                relativePos.z - defaultSize,
                relativePos.x + defaultSize,
                relativePos.y + defaultSize,
                relativePos.z + defaultSize,
                hitBoxColor
            )
        }

        if (approxBoxVisibility) {
            val approxSize = if (distance > 30) distance / 70 else 0.5
            drawBox(
                bufferBuilder,
                relativePos.x - approxSize,
                relativePos.y - approxSize,
                relativePos.z - approxSize,
                relativePos.x + approxSize,
                relativePos.y + approxSize,
                relativePos.z + approxSize,
                approxBoxColor
            )
        }
    }

    private fun drawTrajectoryLine(bufferBuilder: BufferBuilder, start: Vec3d, end: Vec3d, color: Color) {
        val red = color.red / 255.0f
        val green = color.green / 255.0f
        val blue = color.blue / 255.0f
        val alpha = color.alpha / 255.0f

        bufferBuilder.vertex(start.x.toFloat(), start.y.toFloat(), start.z.toFloat())
            .color(red, green, blue, alpha)

        bufferBuilder.vertex(end.x.toFloat(), end.y.toFloat(), end.z.toFloat())
            .color(red, green, blue, alpha)
    }

    private fun drawBox(
        bufferBuilder: BufferBuilder,
        x1: Double, y1: Double, z1: Double,
        x2: Double, y2: Double, z2: Double,
        color: Color
    ) {
        val red = color.red / 255.0f
        val green = color.green / 255.0f
        val blue = color.blue / 255.0f
        val alpha = color.alpha / 255.0f

        val fx1 = x1.toFloat()
        val fy1 = y1.toFloat()
        val fz1 = z1.toFloat()
        val fx2 = x2.toFloat()
        val fy2 = y2.toFloat()
        val fz2 = z2.toFloat()

        // Bottom side
        bufferBuilder.vertex(fx1, fy1, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy1, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy1, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy1, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy1, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy1, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy1, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy1, fz1).color(red, green, blue, alpha)

        // Top side
        bufferBuilder.vertex(fx1, fy2, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy2, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy2, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy2, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy2, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy2, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy2, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy2, fz1).color(red, green, blue, alpha)

        // Vertical edges
        bufferBuilder.vertex(fx1, fy1, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy2, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy1, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy2, fz1).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy1, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx2, fy2, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy1, fz2).color(red, green, blue, alpha)
        bufferBuilder.vertex(fx1, fy2, fz2).color(red, green, blue, alpha)
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

    private data class TrajectoryParams(
        val speed: Float,
        val gravity: Float,
        val drag: Float,
        val mainHand: Boolean
    )
}