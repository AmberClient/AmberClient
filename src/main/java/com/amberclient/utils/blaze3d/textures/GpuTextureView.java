package com.amberclient.utils.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class GpuTextureView implements AutoCloseable {
    private final GpuTexture texture;
    private final int baseMipLevel;
    private final int mipLevels;

    public GpuTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        this.texture = texture;
        this.baseMipLevel = baseMipLevel;
        this.mipLevels = mipLevels;
    }

    public abstract void close();

    public GpuTexture texture() {
        return this.texture;
    }

    public int baseMipLevel() {
        return this.baseMipLevel;
    }

    public int mipLevels() {
        return this.mipLevels;
    }

    public int getWidth(int mipLevel) {
        return this.texture.getWidth(mipLevel + this.baseMipLevel);
    }

    public int getHeight(int mipLevel) {
        return this.texture.getHeight(mipLevel + this.baseMipLevel);
    }

    public abstract boolean isClosed();
}
