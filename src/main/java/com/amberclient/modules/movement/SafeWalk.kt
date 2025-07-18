package com.amberclient.modules.movement

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import com.amberclient.utils.module.ModuleSettings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.util.math.BlockPos
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.math.floor
import kotlin.random.Random

class SafeWalk : Module("SafeWalk", "Prevents falling off edges", ModuleCategory.MOVEMENT), ConfigurableModule {

    companion object {
        const val MOD_ID = "amberclient-safewalk"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        @JvmField
        var safewalk = false

        @JvmField
        var currentEdgeDistance = 1.0
    }

    // Settings
    private val inAir = ModuleSettings("InAir", "Enable SafeWalk in air", true)
    private val randomEdgeDistance = ModuleSettings("Random Edge Distance", "Randomize edge detection distance", false)

    // Internal state
    private val mc = MinecraftClient.getInstance()
    private var currentBlock: BlockPos? = null
    private val random = Random
    private var distanceUpdateTicks = 0

    init {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (isEnabled()) {
                onTick()
            }
        }
    }

    override fun getSettings(): List<ModuleSettings> {
        return listOf(inAir, randomEdgeDistance)
    }

    override fun onEnable() {
        currentBlock = null
        safewalk = true
        currentEdgeDistance = 1.0
        distanceUpdateTicks = 0
    }

    override fun onDisable() {
        safewalk = false
        currentEdgeDistance = 1.0
    }

    override fun onTick() {
        val player = mc.player ?: return

        var shouldEnableSafeWalk = true

        if (!(inAir.value as Boolean) && !player.isOnGround) {
            shouldEnableSafeWalk = false
        }

        if ((randomEdgeDistance.value as Boolean) && shouldEnableSafeWalk) {
            distanceUpdateTicks++
            val updateInterval = 40 + random.nextInt(80)
            if (distanceUpdateTicks >= updateInterval) {
                currentEdgeDistance = 0.5 + (random.nextDouble() * 0.1)
                distanceUpdateTicks = 0
            }
        } else {
            currentEdgeDistance = 1.0
        }

        safewalk = shouldEnableSafeWalk

        val newBlock = BlockPos(
            floor(player.x).toInt(),
            floor(player.y - 0.2).toInt(),
            floor(player.z).toInt()
        )

        if (newBlock != currentBlock) {
            currentBlock = newBlock
        }
    }
}