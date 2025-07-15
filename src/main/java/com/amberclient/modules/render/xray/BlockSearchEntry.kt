package com.amberclient.modules.render.xray

import com.amberclient.utils.core.Color
import net.minecraft.block.BlockState

data class BlockSearchEntry(
    val state: BlockState,
    val color: Color,
    val isDefault: Boolean,
    val minY: Int = -64,
    val maxY: Int = 320,
    val size: Int = 1,
    val count: Int = 1,
    val rarity: Float = 1.0f
) {
    constructor(state: BlockState, color: Color, isDefault: Boolean) : this(state, color, isDefault, -64, 320, 1, 1, 1.0f)

    fun getName(): String = state.block.translationKey
}