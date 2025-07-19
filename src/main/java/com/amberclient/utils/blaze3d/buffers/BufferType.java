package com.amberclient.utils.blaze3d.buffers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum BufferType {
    VERTICES,
    INDICES,
    PIXEL_PACK,
    COPY_READ,
    COPY_WRITE,
    PIXEL_UNPACK,
    UNIFORM;
}
