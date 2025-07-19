package com.amberclient.utils.blaze3d.textures;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class OpenGLTextureView extends GpuTextureView {
    private boolean closed = false;

    public OpenGLTextureView(OpenGLTexture texture, int baseMipLevel, int mipLevels) {
        super(texture, baseMipLevel, mipLevels);
    }

    public OpenGLTexture getOpenGLTexture() {
        return (OpenGLTexture) texture();
    }

    public int getTextureId() {
        return getOpenGLTexture().getTextureId();
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