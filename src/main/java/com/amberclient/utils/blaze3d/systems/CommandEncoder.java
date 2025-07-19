package com.amberclient.utils.blaze3d.systems;

import com.amberclient.utils.blaze3d.buffers.GpuBuffer;
import com.amberclient.utils.blaze3d.buffers.GpuBufferSlice;
import com.amberclient.utils.blaze3d.buffers.GpuFence;
import com.amberclient.utils.blaze3d.textures.GpuTexture;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

import com.amberclient.utils.blaze3d.textures.GpuTextureView;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderPass;
import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface CommandEncoder {
    RenderPass createRenderPass(Supplier<String> labelGetter, GpuTextureView colorAttachment, OptionalInt clearColor);

    RenderPass createRenderPass(
            Supplier<String> labelGetter, GpuTextureView colorAttachment, OptionalInt clearColor, @Nullable GpuTextureView depthAttachment, OptionalDouble clearDepth
    );

    void clearColorTexture(GpuTexture texture, int color);

    void clearColorAndDepthTextures(GpuTexture colorAttachment, int color, GpuTexture depthAttachment, double depth);

    void clearColorAndDepthTextures(
            GpuTexture colorAttachment, int color, GpuTexture depthAttachment, double depth, int scissorX, int scissorY, int scissorWidth, int scissorHeight
    );

    void clearDepthTexture(GpuTexture texture, double depth);

    void writeToBuffer(GpuBufferSlice slice, ByteBuffer source);

    GpuBuffer.MappedView mapBuffer(GpuBuffer buffer, boolean read, boolean write);

    GpuBuffer.MappedView mapBuffer(GpuBufferSlice slice, boolean read, boolean write);

    void copyToBuffer(GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2);

    void writeToTexture(GpuTexture target, NativeImage source);

    void writeToTexture(
            GpuTexture target, NativeImage source, int mipLevel, int depth, int offsetX, int offsetY, int width, int height, int skipPixels, int skipRows
    );

    void writeToTexture(GpuTexture target, IntBuffer source, NativeImage.Format format, int mipLevel, int depth, int offsetX, int offsetY, int width, int height);

    void copyTextureToBuffer(GpuTexture source, GpuBuffer target, int offset, Runnable dataUploadedCallback, int mipLevel);

    void copyTextureToBuffer(
            GpuTexture source, GpuBuffer target, int offset, Runnable dataUploadedCallback, int mipLevel, int intoX, int intoY, int width, int height
    );

    void copyTextureToTexture(GpuTexture source, GpuTexture target, int mipLevel, int intoX, int intoY, int sourceX, int sourceY, int width, int height);

    void presentTexture(GpuTextureView texture);

    GpuFence createFence();
}
