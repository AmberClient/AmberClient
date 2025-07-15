package com.amberclient.modules.render.xray

import com.amberclient.utils.core.Color
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import net.minecraft.block.BlockState
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ScanTask(
    private val centerChunk: ChunkPos,
    private val range: Int
) : Runnable {

    companion object {
        val renderQueue: MutableSet<BlockPosWithColor> = Collections.synchronizedSet(HashSet())
        private val isScanning = AtomicBoolean(false)
        private var playerLastChunk: ChunkPos? = null
        private var forceNextScan = false

        fun runTask(centerChunk: ChunkPos, range: Int) {
            runTask(centerChunk, range, false)
        }

        fun runTask(centerChunk: ChunkPos, range: Int, forceScan: Boolean) {
            val client = MinecraftClient.getInstance()
            if (client.player == null || client.world == null || !SettingsStore.getInstance().get().isActive) {
                return
            }

            if (forceScan || forceNextScan || playerLocationChanged(client.player!!, centerChunk, range) || playerLastChunk == null) {
                playerLastChunk = centerChunk
                forceNextScan = false
                client.execute(ScanTask(centerChunk, range))
            }
        }

        fun requestForcedScan() {
            forceNextScan = true
        }

        fun resetLocationTracking() {
            playerLastChunk = null
            forceNextScan = true
        }

        fun blockBroken(world: World, player: PlayerEntity, blockPos: BlockPos, blockState: BlockState, blockEntity: BlockEntity?) {
            if (player !is ClientPlayerEntity) return
            if (!SettingsStore.getInstance().get().isActive) return
            if (renderQueue.any { it.pos == blockPos }) {
                runTask(player.chunkPos, SettingsStore.getInstance().get().halfRange, true)
            }
        }

        private fun playerLocationChanged(player: ClientPlayerEntity, currentChunk: ChunkPos, range: Int): Boolean {
            val lastChunk = playerLastChunk ?: return true
            return currentChunk.x > lastChunk.x + range || currentChunk.x < lastChunk.x - range ||
                    currentChunk.z > lastChunk.z + range || currentChunk.z < lastChunk.z - range
        }
    }

    override fun run() {
        if (isScanning.get()) {
            return
        }

        isScanning.set(true)
        val blocks = collectBlocks()
        renderQueue.clear()
        renderQueue.addAll(blocks)
        isScanning.set(false)
        RenderOutlines.requestedRefresh.set(true)
    }

    private fun collectBlocks(): Set<BlockPosWithColor> {
        val blocks = BlockStore.getInstance().getCache().get()
        if (blocks.isEmpty()) {
            return HashSet()
        }

        val instance = MinecraftClient.getInstance()
        val world = instance.world ?: return HashSet()
        val player = instance.player ?: return HashSet()

        val renderQueue = HashSet<BlockPosWithColor>()
        val cX = centerChunk.x
        val cZ = centerChunk.z
        val exposedOnly = SettingsStore.getInstance().get().isExposedOnly

        for (i in (cX - range)..(cX + range)) {
            for (j in (cZ - range)..(cZ + range)) {
                if (!world.isChunkLoaded(i, j)) {
                    continue
                }

                val chunkStartX = i shl 4
                val chunkStartZ = j shl 4

                for (k in chunkStartX until chunkStartX + 16) {
                    for (l in chunkStartZ until chunkStartZ + 16) {
                        val topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, k, l)
                        for (m in world.bottomY until topY) {
                            val pos = BlockPos(k, m, l)
                            val color = isValidBlock(pos, world, blocks)
                            if (color != null) {
                                if (!exposedOnly || isBlockExposed(pos, world)) {
                                    renderQueue.add(BlockPosWithColor(pos, color))
                                }
                            }
                        }
                    }
                }
            }
        }

        return renderQueue
    }

    private fun isValidBlock(pos: BlockPos, world: World, blocks: Set<BlockSearchEntry>): Color? {
        val state = world.getBlockState(pos)
        if (state.isAir) {
            return null
        }

        if (SettingsStore.getInstance().get().isShowLava && state.block == Blocks.LAVA) {
            return Color(255, 69, 0, 255)
        }

        val defaultState = state.block.defaultState

        return blocks.firstOrNull { entry ->
            (entry.isDefault && defaultState == entry.state) ||
                    (!entry.isDefault && state == entry.state)
        }?.color
    }

    private fun isBlockExposed(pos: BlockPos, world: World): Boolean {
        val adjacentPositions = arrayOf(
            pos.up(),    // Y+1
            pos.down(),  // Y-1
            pos.north(), // Z-1
            pos.south(), // Z+1
            pos.east(),  // X+1
            pos.west()   // X-1
        )

        for (adjacentPos in adjacentPositions) {
            val adjacentState = world.getBlockState(adjacentPos)

            if (adjacentState.isAir ||
                !adjacentState.isOpaque ||
                !adjacentState.fluidState.isEmpty ||
                isNaturalCavity(adjacentPos, world)) {
                return true
            }
        }

        return false
    }

    private fun isNaturalCavity(pos: BlockPos, world: World): Boolean {
        val state = world.getBlockState(pos)

        if (state.isAir) {
            var airCount = 0
            val checkRadius = 2

            for (x in -checkRadius..checkRadius) {
                for (y in -checkRadius..checkRadius) {
                    for (z in -checkRadius..checkRadius) {
                        if (x == 0 && y == 0 && z == 0) continue

                        val checkPos = pos.add(x, y, z)
                        if (world.getBlockState(checkPos).isAir) {
                            airCount++
                        }
                    }
                }
            }

            val totalBlocks = (checkRadius * 2 + 1).let { it * it * it }
            return airCount > totalBlocks * 0.3
        }

        return false
    }

    private fun isBlockNearSurface(pos: BlockPos, world: World): Boolean {
        val surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.x, pos.z)
        val maxDepth = 10

        return pos.y >= (surfaceY - maxDepth)
    }

    private fun isBlockExposedAndNearSurface(pos: BlockPos, world: World): Boolean {
        if (!isBlockExposed(pos, world)) {
            return false
        }

        val surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, pos.x, pos.z)
        val maxDepthFromSurface = 15

        return pos.y >= (surfaceY - maxDepthFromSurface)
    }

    private fun isBlockInLargeAirPocket(pos: BlockPos, world: World): Boolean {
        var airBlockCount = 0
        var totalBlocksChecked = 0
        val radius = 2

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    if (x == 0 && y == 0 && z == 0) continue

                    val checkPos = pos.add(x, y, z)
                    val state = world.getBlockState(checkPos)

                    if (state.isAir) {
                        airBlockCount++
                    }
                    totalBlocksChecked++
                }
            }
        }

        return (airBlockCount.toDouble() / totalBlocksChecked) > 0.3
    }
}