package com.amberclient.utils.blaze3d.pipeline;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface CompiledRenderPipeline {
    boolean containsUniform(String name);

    boolean isValid();
}
