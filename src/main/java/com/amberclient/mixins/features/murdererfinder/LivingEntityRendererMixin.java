package com.amberclient.mixins.features.murdererfinder;

import com.amberclient.modules.minigames.murdererfinder.MurdererFinder;
import com.amberclient.utils.accessors.PlayerEntityMixinAccessor;
import com.amberclient.modules.minigames.murdererfinder.config.ConfigManager;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(at = @At("HEAD"), method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", cancellable = true)
    public void onShouldRenderName(LivingEntity livingEntity, double distance, CallbackInfoReturnable<Boolean> info) {
        if (MurdererFinder.isActive() && ConfigManager.getConfig().getMm().shouldShowNameTags()) {
            if (livingEntity instanceof PlayerEntity && ((PlayerEntityMixinAccessor)livingEntity).amberClient$isRealPlayer()) {
                info.setReturnValue(true);
            }
        }
    }
}