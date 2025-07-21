package com.amberclient.mixins.features.murdererfinder;

import com.amberclient.utils.minecraft.MinecraftUtils;
import com.amberclient.modules.minigames.murdererfinder.MurdererFinder;
import com.amberclient.modules.minigames.murdererfinder.config.ConfigManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Inject(at = @At("HEAD"), method = "setStack")
    private void onSetStack(int slot, ItemStack stack, CallbackInfo info) {
        if (MurdererFinder.isActive() && !MurdererFinder.clientIsMurder)
            if (ConfigManager.getConfig().getMm().isMurderItem(stack.getItem())) {
                MurdererFinder.clientIsMurder = true;
                MinecraftUtils.sendChatMessage("§4[AmberClient] " + Text.translatable("message.mm.client_is_murder", Formatting.RED).getString());
            }
    }
}
