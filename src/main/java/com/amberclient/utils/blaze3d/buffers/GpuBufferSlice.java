package com.amberclient.utils.blaze3d.buffers;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record GpuBufferSlice(GpuBuffer buffer, int offset, int length) {
    public GpuBufferSlice slice(int offset, int length) {
        if (offset >= 0 && length >= 0 && offset + length < this.length) {
            return new GpuBufferSlice(this.buffer, this.offset + offset, length);
        } else {
            throw new IllegalArgumentException(
                    "Offset of " + offset + " and length " + length + " would put new slice outside existing slice's range (of " + offset + "," + length + ")"
            );
        }
    }
}
