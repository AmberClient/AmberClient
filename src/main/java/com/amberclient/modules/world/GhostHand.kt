package com.amberclient.modules.world

import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.World
import kotlin.math.ceil

class GhostHand : Module("GhostHand", "Allows the player to open containers through walls", ModuleCategory.WORLD) {

    companion object {
        private const val MAX_REACH_DISTANCE = 6.0
    }

    private val mc = MinecraftClient.getInstance()

    override fun onTick() {
        if (mc.player == null || mc.world == null) return

        if (mc.options.useKey.wasPressed()) {
            handleGhostInteraction()
        }
    }

    private fun handleGhostInteraction() {
        val player = mc.player ?: return
        val world = mc.world ?: return

        val eyePos = player.eyePos
        val lookVec = player.getRotationVec(1.0f)
        val endPos = eyePos.add(lookVec.multiply(MAX_REACH_DISTANCE))

        val targetPos = findContainerInRange(world, eyePos, endPos)

        targetPos?.let { pos ->
            val blockState = world.getBlockState(pos)
            val block = blockState.block

            if (isContainer(block)) {
                interactWithContainer(world, pos, blockState)
            }
        }
    }

    private fun findContainerInRange(world: World, start: Vec3d, end: Vec3d): BlockPos? {
        val direction = end.subtract(start).normalize()
        val distance = start.distanceTo(end)

        var d = 0.0
        while (d <= distance) {
            val currentPos = start.add(direction.multiply(d))
            val blockPos = BlockPos.ofFloored(currentPos)
            val blockState = world.getBlockState(blockPos)

            if (isContainer(blockState.block)) {
                return blockPos
            }
            d += 0.1
        }

        return null
    }

    private fun isContainer(block: Block): Boolean {
        val blockName = block.translationKey
        return blockName.contains("chest") ||
                blockName.contains("furnace") ||
                blockName.contains("dispenser") ||
                blockName.contains("dropper") ||
                blockName.contains("hopper") ||
                blockName.contains("barrel") ||
                blockName.contains("shulker_box") ||
                blockName.contains("ender_chest") ||
                blockName.contains("brewing_stand") ||
                blockName.contains("enchanting_table") ||
                blockName.contains("crafting_table") ||
                blockName.contains("smithing_table") ||
                blockName.contains("stonecutter") ||
                blockName.contains("loom") ||
                blockName.contains("grindstone") ||
                blockName.contains("cartography_table") ||
                blockName.contains("fletching_table") ||
                blockName.contains("anvil")
    }

    private fun interactWithContainer(world: World, pos: BlockPos, blockState: BlockState) {
        val player = mc.player ?: return
        val interactionManager = mc.interactionManager ?: return

        try {
            val hitResult = BlockHitResult(
                Vec3d.ofCenter(pos),
                Direction.UP,
                pos,
                false
            )

            val result = interactionManager.interactBlock(
                player,
                Hand.MAIN_HAND,
                hitResult
            )

            if (result.isAccepted) {
                player.swingHand(Hand.MAIN_HAND)
            }
        } catch (ignored: Exception) { }
    }

    fun isContainerOpen(): Boolean {
        return mc.player?.let { player ->
            player.currentScreenHandler != null &&
                    player.currentScreenHandler !is PlayerScreenHandler
        } ?: false
    }

    fun getReachDistance(): Double {
        return if (isEnabled()) MAX_REACH_DISTANCE else 4.5
    }

    private fun hasLineOfSight(world: World, start: Vec3d, end: Vec3d): Boolean {
        if (isEnabled()) return true

        val player = mc.player ?: return false
        val result = world.raycast(
            RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        )

        return result.type == HitResult.Type.MISS
    }

    fun getContainersInRange(): List<BlockPos> {
        val containers = mutableListOf<BlockPos>()

        val player = mc.player ?: return containers
        val world = mc.world ?: return containers

        val playerPos = player.eyePos
        val range = ceil(MAX_REACH_DISTANCE).toInt()

        for (x in -range..range) {
            for (y in -range..range) {
                for (z in -range..range) {
                    val pos = player.blockPos.add(x, y, z)

                    if (playerPos.distanceTo(Vec3d.ofCenter(pos)) <= MAX_REACH_DISTANCE) {
                        val state = world.getBlockState(pos)
                        if (isContainer(state.block)) {
                            containers.add(pos)
                        }
                    }
                }
            }
        }

        return containers
    }
}