package com.amberclient.modules.minigames.murdererfinder

import com.amberclient.modules.minigames.murdererfinder.config.ConfigManager
import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import com.amberclient.utils.module.ModuleSettings
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/*
      Mod originally created by https://github.com/thatDudo for 1.18.1
      Remastered in 1.21.4 and improved by https://github.com/gqdThinky
      All credits go to thatDudo.
*/

class MMFinder : Module("MurdererFinder", "Find the murderer in Hypixel's Murder Mystery", ModuleCategory.MINIGAMES),
    ConfigurableModule {

    companion object {
        const val MOD_ID = "amberclient-murderfinder"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        @JvmStatic
        val instance = MMFinder()
    }

    private val settings: MutableList<ModuleSettings>
    private val highlightMurders: ModuleSettings
    private val highlightGold: ModuleSettings
    private val highlightBows: ModuleSettings
    private val showNameTags: ModuleSettings
    private val highlightSpectators: ModuleSettings

    init {
        // Initialize settings with values from Config
        val config = ConfigManager.getConfig()
        highlightMurders = ModuleSettings(
            "Highlight Murderers",
            "Highlight players identified as murderers",
            config.mm.shouldHighlightMurders()
        )
        highlightGold =
            ModuleSettings("Highlight Gold", "Highlight gold items in the game", config.mm.shouldHighlightGold())
        highlightBows = ModuleSettings("Highlight Bows", "Highlight bows in the game", config.mm.shouldHighlightBows())
        showNameTags = ModuleSettings("Show Name Tags", "Display name tags for players", config.mm.shouldShowNameTags())
        highlightSpectators =
            ModuleSettings("Highlight Spectators", "Highlight spectator players", config.mm.shouldHighlightSpectators())

        settings = mutableListOf(
            highlightMurders,
            highlightGold,
            highlightBows,
            showNameTags,
            highlightSpectators
        )
    }

    override fun onEnable() {
        try {
            MurdererFinder.setModEnabled(true)
            println("MurdererFinder mod has been activated!")
        } catch (e: Exception) {
            System.err.println("Failed to activate MurdererFinder mod: ${e.message}")
        }
    }

    override fun onDisable() {
        try {
            MurdererFinder.setModEnabled(false)
            println("MurdererFinder mod has been deactivated!")
        } catch (e: Exception) {
            System.err.println("Failed to deactivate MurdererFinder mod: ${e.message}")
        }
    }

    override fun getSettings(): List<ModuleSettings> = settings

    override fun onSettingChanged(setting: ModuleSettings) {
        val client = MinecraftClient.getInstance()
        if (client.player == null) return

        val config = ConfigManager.getConfig()
        val status: String

        when (setting) {
            highlightMurders -> {
                status = if (highlightMurders.booleanValue) "enabled" else "disabled"
                config.mm.highlightMurders = highlightMurders.booleanValue
                client.player!!.sendMessage(
                    Text.literal("§6MMFinder settings updated: Highlight Murderers=$status"),
                    true
                )
                LOGGER.info("MMFinder settings updated: Highlight Murderers={}", status)
            }
            highlightGold -> {
                status = if (highlightGold.booleanValue) "enabled" else "disabled"
                config.mm.highlightGold = highlightGold.booleanValue
                client.player!!.sendMessage(
                    Text.literal("§6MMFinder settings updated: Highlight Gold=$status"),
                    true
                )
                LOGGER.info("MMFinder settings updated: Highlight Gold={}", status)
            }
            highlightBows -> {
                status = if (highlightBows.booleanValue) "enabled" else "disabled"
                config.mm.highlightBows = highlightBows.booleanValue
                client.player!!.sendMessage(
                    Text.literal("§6MMFinder settings updated: Highlight Bows=$status"),
                    true
                )
                LOGGER.info("MMFinder settings updated: Highlight Bows={}", status)
            }
            showNameTags -> {
                status = if (showNameTags.booleanValue) "enabled" else "disabled"
                config.mm.showNameTags = showNameTags.booleanValue
                client.player!!.sendMessage(
                    Text.literal("§6MMFinder settings updated: Show Name Tags=$status"),
                    true
                )
                LOGGER.info("MMFinder settings updated: Show Name Tags={}", status)
            }
            highlightSpectators -> {
                status = if (highlightSpectators.booleanValue) "enabled" else "disabled"
                config.mm.highlightSpectators = highlightSpectators.booleanValue
                client.player!!.sendMessage(
                    Text.literal("§6MMFinder settings updated: Highlight Spectators=$status"),
                    true
                )
                LOGGER.info("MMFinder settings updated: Highlight Spectators={}", status)
            }
        }

        // Save the updated configuration
        ConfigManager.writeConfig()
    }
}