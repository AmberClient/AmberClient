package com.amberclient.utils.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum AddressMode {
    REPEAT,
    CLAMP_TO_EDGE;
}
