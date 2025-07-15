package com.amberclient.modules.render.xray

import com.amberclient.utils.core.Color
import net.minecraft.block.Blocks
import java.util.Collections

object BlockStore {
    private var instance: BlockStore? = null
    val cache: MutableSet<BlockSearchEntry> = HashSet()

    init {
        // Default blocks to scan
        cache.add(BlockSearchEntry(Blocks.DIAMOND_ORE.defaultState, Color(51, 236, 255, 255), true))
        cache.add(BlockSearchEntry(Blocks.DEEPSLATE_DIAMOND_ORE.defaultState, Color(51, 236, 255, 255), true))
        cache.add(BlockSearchEntry(Blocks.GOLD_ORE.defaultState, Color(255, 252, 51, 255), true))
        cache.add(BlockSearchEntry(Blocks.DEEPSLATE_GOLD_ORE.defaultState, Color(255, 252, 51, 255), true))
        cache.add(BlockSearchEntry(Blocks.IRON_ORE.defaultState, Color(201, 201, 183, 255), true))
        cache.add(BlockSearchEntry(Blocks.DEEPSLATE_IRON_ORE.defaultState, Color(201, 201, 183, 255), true))
    }

    fun getInstance(): BlockStore {
        if (instance == null) {
            instance = BlockStore
        }
        return instance!!
    }

    fun getCache(): Cache<Set<BlockSearchEntry>> {
        return Cache(Collections.unmodifiableSet(cache))
    }

    data class Cache<T>(val cache: T) {
        fun get(): T = cache
    }
}