package com.amberclient.utils.module

import com.amberclient.utils.input.keybinds.CustomKeybindManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text

abstract class Module(
    public val name: String,
    public val description: String,
    public val category: ModuleCategory
) {
    @JvmField
    protected var enabled: Boolean = false

    @JvmField
    protected val client: MinecraftClient = MinecraftClient.getInstance()

    private var keyBinding: KeyBinding? = null
    private var customKeyCode: Int = -1

    fun isEnabled(): Boolean = enabled

    fun toggle() {
        if (enabled) {
            disable()
        } else {
            enable()
        }
    }

    protected fun enable() {
        enabled = true

        val mc = MinecraftClient.getInstance()
        mc.player?.sendMessage(
            Text.literal("§4[§cAmberClient§4] §c§l$name §r§cmodule enabled"),
            true
        )

        onEnable()
    }

    protected fun disable() {
        enabled = false

        val mc = MinecraftClient.getInstance()
        mc.player?.sendMessage(
            Text.literal("§4[§cAmberClient§4] §c§l$name §r§cmodule disabled"),
            true
        )

        onDisable()
    }

    fun setKeyBinding(keyBinding: KeyBinding?) {
        this.keyBinding = keyBinding
    }

    fun getKeyBinding(): KeyBinding? = keyBinding

    fun setCustomKeyCode(keyCode: Int) {
        this.customKeyCode = keyCode
    }

    fun getCustomKeyCode(): Int = customKeyCode

    fun hasCustomKeybind(): Boolean = customKeyCode != -1

    fun getKeybindInfo(): String {
        return when {
            hasCustomKeybind() -> CustomKeybindManager.getKeyName(customKeyCode)
            keyBinding != null -> keyBinding!!.boundKeyLocalizedText.string
            else -> "Not bound"
        }
    }

    // Called when the module is enabled
    protected open fun onEnable() {
        // Override in subclasses to implement enable logic
    }

    // Called when the module is disabled
    protected open fun onDisable() {
        // Override in subclasses to implement disable logic
    }

    // Called every client tick when the module is enabled
    open fun onTick() {
        // Override in subclasses to implement per-tick logic
    }

    // Called every client tick to handle key inputs (even if module is disabled)
    open fun handleKeyInput() {
        // Override in subclasses if needed
    }

    // Called during world rendering for enabled modules
    open fun render(stack: MatrixStack) {
        // Override in subclasses to implement render logic
    }

    protected fun getClient(): MinecraftClient = client
}