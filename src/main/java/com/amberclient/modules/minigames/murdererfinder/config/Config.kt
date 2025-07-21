package com.amberclient.modules.minigames.murdererfinder.config

import com.google.gson.annotations.Expose
import net.minecraft.item.*
import net.minecraft.text.Text
import net.minecraft.text.MutableText

class Config {
    class MurderMystery {
        enum class InnocentHighlightOptions(private val text: MutableText) {
            NEVER(Text.translatable("config.generic.hypixel.mm.highlight.innocent.option.never")),
            AS_MURDER(Text.translatable("config.generic.hypixel.mm.highlight.innocent.option.as_murder")),
            ALWAYS(Text.translatable("config.generic.hypixel.mm.highlight.innocent.option.always"));

            fun getText(): MutableText = text
        }

        enum class DetectiveHighlightOptions(private val text: MutableText) {
            NEVER(Text.translatable("config.generic.hypixel.mm.highlight.detective.option.never")),
            AS_MURDER(Text.translatable("config.generic.hypixel.mm.highlight.detective.option.as_murder")),
            ALWAYS(Text.translatable("config.generic.hypixel.mm.highlight.detective.option.always"));

            fun getText(): MutableText = text
        }

        @Expose
        var highlightMurders = true

        @Expose
        var innocentHighlightOptions: InnocentHighlightOptions? = InnocentHighlightOptions.AS_MURDER

        @Expose
        var detectiveHighlightOptions: DetectiveHighlightOptions? = DetectiveHighlightOptions.ALWAYS

        @Expose
        var highlightGold = true

        @Expose
        var highlightBows = true

        @Expose
        var showNameTags = false

        @Expose
        var highlightSpectators = false



        fun shouldHighlightInnocents(clientIsMurder: Boolean): Boolean {
            return innocentHighlightOptions == InnocentHighlightOptions.ALWAYS ||
                    (innocentHighlightOptions == InnocentHighlightOptions.AS_MURDER && clientIsMurder)
        }

        fun shouldHighlightDetectives(clientIsMurder: Boolean): Boolean {
            return detectiveHighlightOptions == DetectiveHighlightOptions.ALWAYS ||
                    (detectiveHighlightOptions == DetectiveHighlightOptions.AS_MURDER && clientIsMurder)
        }

        fun shouldHighlightMurders(): Boolean = highlightMurders

        fun shouldHighlightGold(): Boolean = highlightGold

        fun shouldHighlightBows(): Boolean = highlightBows

        fun shouldShowNameTags(): Boolean = showNameTags

        fun shouldHighlightSpectators(): Boolean = highlightSpectators

        fun isMurderItem(item: Item): Boolean {
            return item is SwordItem ||
                    murderItems.contains(Item.getRawId(item)) ||
                    (item is ShovelItem && item != Items.WOODEN_SHOVEL)
        }

        fun validate(): Boolean {
            var valid = true

            if (innocentHighlightOptions == null) {
                innocentHighlightOptions = InnocentHighlightOptions.NEVER
                valid = false
            }
            if (detectiveHighlightOptions == null) {
                detectiveHighlightOptions = DetectiveHighlightOptions.NEVER
                valid = false
            }

            return valid
        }

        companion object {
            const val murderTeamColorValue = 0xFF1111
            const val detectiveTeamColorValue = 0x15BFD6
            const val goldTeamColorValue = 0xFFF126
            const val bowTeamColorValue = 0x21E808

            val defaultMurderItems = listOf(
                Items.IRON_SWORD, Items.ENDER_CHEST, Items.COOKED_CHICKEN, Items.BONE, Items.BLAZE_ROD,
                Items.NETHER_BRICK, Items.CARROT_ON_A_STICK, Items.STONE_SWORD, Items.SPONGE, Items.DEAD_BUSH,
                Items.OAK_BOAT, Items.GLISTERING_MELON_SLICE, Items.GOLDEN_PICKAXE, Items.COOKED_BEEF,
                Items.BOOK, Items.APPLE, Items.PRISMARINE_SHARD, Items.QUARTZ, Items.DIAMOND_SWORD,
                Items.NAME_TAG, Items.DIAMOND_SHOVEL, Items.ROSE_BUSH, Items.PUMPKIN_PIE, Items.DIAMOND_HOE,
                Items.CARROT, Items.RED_DYE, Items.SALMON, Items.SHEARS, Items.IRON_SHOVEL, Items.GOLDEN_CARROT,
                Items.WOODEN_SWORD, Items.STICK, Items.STONE_SHOVEL, Items.COOKIE, Items.DIAMOND_AXE,
                Items.GOLDEN_SWORD, Items.WOODEN_AXE, Items.SUGAR_CANE, Items.LEATHER, Items.CHARCOAL,
                Items.FLINT, Items.MUSIC_DISC_BLOCKS, Items.GOLDEN_HOE, Items.LAPIS_LAZULI, Items.BREAD,
                Items.JUNGLE_SAPLING, Items.GOLDEN_AXE, Items.DIAMOND_PICKAXE, Items.GOLDEN_SHOVEL
            )

            val murderItems: Set<Int> = computeMurderItems()

            private fun computeMurderItems(): Set<Int> {
                return defaultMurderItems.map { Item.getRawId(it) }.toSet()
            }
        }
    }

    @Expose
    var enabled = false

    @Expose
    var mm = MurderMystery()

    @Expose
    var checkForUpdates = false

    @Expose
    var shownUpdateNotif = false

    fun validate(): Boolean {
        return mm.validate()
    }
}