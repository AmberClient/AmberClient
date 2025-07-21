package com.amberclient.modules.minigames.murdererfinder

import com.amberclient.utils.minecraft.MinecraftUtils
import com.amberclient.modules.minigames.murdererfinder.config.Config
import com.amberclient.modules.minigames.murdererfinder.config.ConfigManager
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

object MurdererFinder {
    @JvmField val logger: Logger = LoggerFactory.getLogger("mm-finder")

    @JvmField var onHypixel = false

    enum class HypixelLobbies {
        None,
        MurderMystery,
        MurderMysteryLobby
    }

    @JvmField var lobby: HypixelLobbies = HypixelLobbies.None
    @JvmField var roundEnded = false
    @JvmField var clientIsMurder = false
    @JvmField var clientIsDead = false

    @JvmStatic
    fun isEnabled(): Boolean {
        return try {
            val config = ConfigManager.getConfig()
            config.enabled
        } catch (e: Exception) {
            logger.error("Error checking if mod is enabled: ${e.message}")
            false
        }
    }

    @JvmStatic
    fun isActive(): Boolean {
        return isEnabled() && lobby == HypixelLobbies.MurderMystery
    }

    @JvmField val markedMurders: MutableSet<UUID> = HashSet()
    @JvmField val markedDetectives: MutableSet<UUID> = HashSet()

    fun showMarkedPlayers(client: MinecraftClient) {
        if (!isActive()) return

        var murderersList = ""
        var detectivesList = ""

        client.world?.let { world ->
            for (player in world.players) {
                val uuid = player.gameProfile.id
                if (markedMurders.contains(uuid))
                    murderersList += "${player.gameProfile.name} "
                if (markedDetectives.contains(uuid))
                    detectivesList += "${player.gameProfile.name} "
            }
        }

        if (murderersList.isEmpty()) murderersList = "None"
        if (detectivesList.isEmpty()) detectivesList = "None"

        MinecraftUtils.sendChatMessage("")
        MinecraftUtils.sendChatMessage("§4[AmberClient]")
        MinecraftUtils.sendChatMessage(Text.translatable("message.show_murderers").string + Formatting.RED + murderersList)
        MinecraftUtils.sendChatMessage(Text.translatable("message.show_detectives").string + Formatting.AQUA + detectivesList)
        MinecraftUtils.sendChatMessage("")
    }

    @JvmStatic
    fun setModEnabled(state: Boolean) {
        try {
            val config = ConfigManager.getConfig()
            if (state != config.enabled) {
                config.enabled = state
                ConfigManager.writeConfig()
            }
        } catch (e: Exception) {
            logger.error("Error setting mod enabled state: ${e.message}")
        }
    }

    @JvmStatic
    fun setHighlightMurders(state: Boolean) {
        try {
            val config = ConfigManager.getConfig()
            if (config.mm.highlightMurders != state) {
                config.mm.highlightMurders = state
                if (!state)
                    markedMurders.clear()
                ConfigManager.writeConfig()
            }
        } catch (e: Exception) {
            logger.error("Error setting highlight murders: ${e.message}")
        }
    }

    @JvmStatic
    fun setDetectiveHighlightOptions(state: Config.MurderMystery.DetectiveHighlightOptions) {
        try {
            val config = ConfigManager.getConfig()
            if (config.mm.detectiveHighlightOptions != state) {
                config.mm.detectiveHighlightOptions = state
                if (!config.mm.shouldHighlightDetectives(clientIsMurder))
                    markedDetectives.clear()
                ConfigManager.writeConfig()
            }
        } catch (e: Exception) {
            logger.error("Error setting detective highlight options: ${e.message}")
        }
    }

    @JvmStatic
    fun setCurrentLobby(slobby: HypixelLobbies) {
        resetLobby(lobby)
        lobby = slobby
    }

    @JvmStatic
    fun resetLobby(lobby: HypixelLobbies) {
        if (lobby == HypixelLobbies.MurderMystery) {
            roundEnded = false
            clientIsMurder = false
            clientIsDead = false
            markedMurders.clear()
            markedDetectives.clear()
        }
    }
}