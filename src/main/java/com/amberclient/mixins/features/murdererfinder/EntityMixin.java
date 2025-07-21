package com.amberclient.mixins.features.murdererfinder;

import com.amberclient.modules.minigames.murdererfinder.MurdererFinder;
import com.amberclient.utils.accessors.EntityMixinAccessor;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements EntityMixinAccessor {
    private int glowColor = -1;

    @Unique @Override
    public void setGlowColor(int color) {
        glowColor = color;
    }

    @Inject(at = @At("HEAD"), method = "getTeamColorValue", cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> info) {
        if (MurdererFinder.isActive() && glowColor >= 0)
            info.setReturnValue(glowColor);
    }
}
