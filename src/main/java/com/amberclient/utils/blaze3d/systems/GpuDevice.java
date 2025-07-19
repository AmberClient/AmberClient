package com.amberclient.utils.blaze3d.systems;

import com.amberclient.utils.blaze3d.buffers.BufferType;
import com.amberclient.utils.blaze3d.buffers.BufferUsage;
import com.amberclient.utils.blaze3d.buffers.GpuBuffer;
import com.amberclient.utils.blaze3d.pipeline.CompiledRenderPipeline;
import com.amberclient.utils.blaze3d.pipeline.RenderPipeline;
import com.amberclient.utils.blaze3d.shaders.ShaderType;
import com.amberclient.utils.blaze3d.textures.GpuTexture;
import com.amberclient.utils.blaze3d.textures.GpuTextureView;
import com.amberclient.utils.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public interface GpuDevice {
    CommandEncoder createCommandEncoder();

    GpuTexture createTexture(@Nullable Supplier<String> labelGetter, int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels);

    GpuTexture createTexture(@Nullable String label, int usage, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels);

    GpuTextureView createTextureView(GpuTexture texture);

    GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels);

    GpuBuffer createBuffer(@Nullable Supplier<String> labelGetter, int usage, int size);

    GpuBuffer createBuffer(@Nullable Supplier<String> labelGetter, int usage, ByteBuffer data);

    String getImplementationInformation();

    List<String> getLastDebugMessages();

    boolean isDebuggingEnabled();

    String getVendor();

    String getBackendName();

    String getVersion();

    String getRenderer();

    int getMaxTextureSize();

    int getUniformOffsetAlignment();

    default CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline) {
        return this.precompilePipeline(pipeline, null);
    }

    CompiledRenderPipeline precompilePipeline(RenderPipeline pipeline, @Nullable BiFunction<Identifier, ShaderType, String> sourceRetriever);

    void clearPipelineCache();

    List<String> getEnabledExtensions();

    void close();
}
