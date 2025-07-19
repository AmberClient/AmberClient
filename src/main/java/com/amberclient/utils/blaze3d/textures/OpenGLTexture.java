package com.amberclient.utils.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class OpenGLTexture extends GpuTexture {
    private final int textureId;
    private boolean closed = false;

    public OpenGLTexture(String label, int textureId, int width, int height, TextureFormat format) {
        super(label, format, width, height, 1);
        this.textureId = textureId;
    }

    public int getTextureId() {
        return textureId;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}