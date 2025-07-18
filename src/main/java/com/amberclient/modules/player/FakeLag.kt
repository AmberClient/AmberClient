package com.amberclient.modules.player

import com.amberclient.events.core.EventListener
import com.amberclient.events.core.EventManager
import com.amberclient.events.network.PacketEvent
import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import com.amberclient.utils.module.ModuleSettings
import net.minecraft.client.MinecraftClient
import net.minecraft.network.ClientConnection
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import java.util.concurrent.ConcurrentLinkedQueue

class FakeLag : Module("FakeLag", "Holds back packets to prevent you from being hit", ModuleCategory.PLAYER),
    ConfigurableModule {

    private val delay = ModuleSettings("Delay (ms)", "Delay time for packet delay", 200.0, 50.0, 1000.0, 10.0)
    private val onlyMovement = ModuleSettings("Only Movement", "Delays motion packets only", true)
    private val maxPackets =
        ModuleSettings("Max Packets", "Maximum number of packages to retain", 20.0, 5.0, 100.0, 1.0)
    private val pulse = ModuleSettings("Pulse", "Sends packets in bursts", false)
    private val pulseDelay = ModuleSettings("Pulse Delay (ms)", "Delay between bursts", 1000.0, 500.0, 5000.0, 50.0)

    private val settings: MutableList<ModuleSettings> = mutableListOf<ModuleSettings>().apply {
        add(delay)
        add(onlyMovement)
        add(maxPackets)
        add(pulse)
        add(pulseDelay)
    }

    private val packetQueue = ConcurrentLinkedQueue<QueuedPacket>()
    private var lastPulseTime = 0L
    private var tickCounter = 0

    companion object {
        @JvmStatic
        private var bypassPacketSend = false

        @JvmStatic
        fun isBypassing(): Boolean = bypassPacketSend
    }

    data class QueuedPacket(
        val packet: Any,
        val timestamp: Long,
        val connection: ClientConnection
    )

    override fun onEnable() {
        super.onEnable()
        EventManager.getInstance().register(this)
        packetQueue.clear()
        lastPulseTime = System.currentTimeMillis()
        tickCounter = 0
    }

    override fun onDisable() {
        super.onDisable()
        EventManager.getInstance().unregister(this)

        flushAllPackets()
        packetQueue.clear()
    }

    override fun onTick() {
        super.onTick()

        val mc = MinecraftClient.getInstance()
        if (mc.player == null || mc.world == null) return

        tickCounter++

        if (tickCounter % 20 == 0) {
            cleanupOldPackets()
        }

        if (tickCounter % 3 == 0) {
            processPackets()
        }
    }

    @EventListener
    fun onPacketSend(event: PacketEvent.Send) {
        if (!isEnabled()) return

        val mc = MinecraftClient.getInstance()
        if (mc.player == null || mc.world == null) return

        val packet = event.packet

        if (shouldDelayPacket(packet)) {
            if (packetQueue.size >= maxPackets.integerValue) {
                packetQueue.poll()
            }

            packetQueue.offer(QueuedPacket(packet, System.currentTimeMillis(), event.connection))

            event.cancel()
        }
    }

    private fun shouldDelayPacket(packet: Any): Boolean {
        if (onlyMovement.booleanValue) {
            return packet is PlayerMoveC2SPacket
        }

        return packet is PlayerMoveC2SPacket ||
                packet is PlayerActionC2SPacket ||
                packet is PlayerInteractBlockC2SPacket ||
                packet is PlayerInteractEntityC2SPacket ||
                packet is PlayerInteractItemC2SPacket ||
                packet is HandSwingC2SPacket ||
                packet is PlayerInputC2SPacket
    }

    private fun processPackets() {
        if (!isEnabled()) return

        val mc = MinecraftClient.getInstance()
        if (mc.player == null || mc.world == null) return

        val currentTime = System.currentTimeMillis()

        if (pulse.booleanValue) {
            if (currentTime - lastPulseTime >= pulseDelay.integerValue) {
                flushAllPackets()
                lastPulseTime = currentTime
            }
        } else {
            val packetsToSend = mutableListOf<QueuedPacket>()

            val iterator = packetQueue.iterator()
            while (iterator.hasNext()) {
                val queuedPacket = iterator.next()
                if (currentTime - queuedPacket.timestamp >= delay.integerValue) {
                    packetsToSend.add(queuedPacket)
                    iterator.remove()
                }
            }

            packetsToSend.forEach { queuedPacket ->
                sendPacketDirectly(queuedPacket)
            }
        }
    }

    private fun flushAllPackets() {
        val packetsToSend = mutableListOf<QueuedPacket>()

        while (packetQueue.isNotEmpty()) {
            packetQueue.poll()?.let { packetsToSend.add(it) }
        }

        packetsToSend.forEach { queuedPacket ->
            sendPacketDirectly(queuedPacket)
        }
    }

    private fun sendPacketDirectly(queuedPacket: QueuedPacket) {
        try {
            bypassPacketSend = true

            val connection = queuedPacket.connection
            val packet = queuedPacket.packet as Packet<*>

            connection.send(packet)

        } catch (_: Exception) {
            try {
                val mc = MinecraftClient.getInstance()
                mc.networkHandler?.sendPacket(queuedPacket.packet as Packet<*>)
            } catch (_: Exception) { }
        } finally {
            bypassPacketSend = false
        }
    }

    private fun cleanupOldPackets() {
        val currentTime = System.currentTimeMillis()
        val maxAge = 5000L

        val iterator = packetQueue.iterator()
        while (iterator.hasNext()) {
            val queuedPacket = iterator.next()
            if (currentTime - queuedPacket.timestamp > maxAge) {
                iterator.remove()
            }
        }
    }

    fun getQueueSize(): Int = packetQueue.size

    fun getDelayMs(): Int = delay.integerValue

    override fun getSettings(): List<ModuleSettings> = settings

    override fun toString(): String {
        return "${name}: Queue: ${getQueueSize()}, Delay: ${getDelayMs()}ms"
    }
}