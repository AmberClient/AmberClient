package com.amberclient.utils.blaze3d.pipeline;

import com.amberclient.utils.blaze3d.platform.DestFactor;
import com.amberclient.utils.blaze3d.platform.SourceFactor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public record BlendFunction(SourceFactor sourceColor, DestFactor destColor, SourceFactor sourceAlpha, DestFactor destAlpha) {
    public static final BlendFunction LIGHTNING = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE);
    public static final BlendFunction GLINT = new BlendFunction(SourceFactor.SRC_COLOR, DestFactor.ONE, SourceFactor.ZERO, DestFactor.ONE);
    public static final BlendFunction OVERLAY = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
    public static final BlendFunction TRANSLUCENT = new BlendFunction(
            SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA
    );
    public static final BlendFunction ADDITIVE = new BlendFunction(SourceFactor.ONE, DestFactor.ONE);
    public static final BlendFunction PANORAMA = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
    public static final BlendFunction ENTITY_OUTLINE_BLIT = new BlendFunction(
            SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ZERO, DestFactor.ONE
    );

    public BlendFunction(SourceFactor source, DestFactor dest) {
        this(source, dest, source, dest);
    }
}
