package com.amberclient.modules.player

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleSettings
import com.amberclient.events.player.SendMovementPacketsEvent
import com.amberclient.events.network.PacketEvent
import com.amberclient.mixins.accessors.PlayerMoveC2SPacketAccessor
import com.amberclient.events.core.EventListener
import com.amberclient.utils.module.ModuleCategory
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.text.Text

class AntiHunger : Module("AntiHunger", "Reduces (does NOT remove) hunger consumption.", ModuleCategory.PLAYER), ConfigurableModule {

    private val sprintSetting = ModuleSettings("Sprint", "Spoofs sprinting packets.", true)
    private val onGroundSetting = ModuleSettings("On Ground", "Spoofs the onGround flag.", true)

    private var lastOnGround = false
    private var ignorePacket = false

    override fun getSettings(): List<ModuleSettings> {
        return listOf(sprintSetting, onGroundSetting)
    }

    override fun onEnable() {
        super.onEnable()
        client.player?.let { player ->
            lastOnGround = player.isOnGround
        }
    }

    @EventListener
    private fun onSendPacket(event: PacketEvent.Send) {
        if (ignorePacket && event.packet is PlayerMoveC2SPacket) {
            ignorePacket = false
            return
        }

        val player = client.player ?: return

        if (player.hasVehicle() || player.isTouchingWater || player.isSubmergedInWater()) return

        when (event.packet) {
            is ClientCommandC2SPacket -> {
                if (sprintSetting.booleanValue && (event.packet as ClientCommandC2SPacket).mode == ClientCommandC2SPacket.Mode.START_SPRINTING) {
                    event.cancel()
                }
            }
            is PlayerMoveC2SPacket -> {
                if (onGroundSetting.booleanValue &&
                    player.isOnGround &&
                    player.fallDistance <= 0.0f &&
                    client.interactionManager?.isBreakingBlock != true) {
                    (event.packet as PlayerMoveC2SPacketAccessor).setOnGround(false)
                }
            }
        }
    }

    @EventListener
    private fun onTick(event: SendMovementPacketsEvent.Pre) {
        val player = client.player ?: return

        if (player.isOnGround && !lastOnGround && onGroundSetting.booleanValue) {
            ignorePacket = true
        }

        val mc = MinecraftClient.getInstance()
        mc.player?.sendMessage(Text.literal("§4test"), true)

        lastOnGround = player.isOnGround
    }
}