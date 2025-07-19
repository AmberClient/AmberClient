package com.amberclient.utils.blaze3d.pipeline;

import com.amberclient.utils.blaze3d.platform.DepthTestFunction;
import com.amberclient.utils.blaze3d.platform.LogicOp;
import com.amberclient.utils.blaze3d.platform.PolygonMode;
import com.amberclient.utils.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Defines;
import com.amberclient.utils.blaze3d.UniformType;
import net.minecraft.util.Identifier;
import net.minecraft.util.annotation.DeobfuscateClass;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class RenderPipeline {
    private final Identifier location;
    private final Identifier vertexShader;
    private final Identifier fragmentShader;
    private final Defines shaderDefines;
    private final List<String> samplers;
    private final List<RenderPipeline.UniformDescription> uniforms;
    private final DepthTestFunction depthTestFunction;
    private final PolygonMode polygonMode;
    private final boolean cull;
    private final LogicOp colorLogic;
    private final Optional<BlendFunction> blendFunction;
    private final boolean writeColor;
    private final boolean writeAlpha;
    private final boolean writeDepth;
    private final VertexFormat vertexFormat;
    private final VertexFormat.DrawMode vertexFormatMode;
    private final float depthBiasScaleFactor;
    private final float depthBiasConstant;

    protected RenderPipeline(
            Identifier location,
            Identifier vertexShader,
            Identifier fragmentShader,
            Defines shaderDefines,
            List<String> samplers,
            List<RenderPipeline.UniformDescription> uniforms,
            Optional<BlendFunction> blendFunction,
            DepthTestFunction depthTestFunction,
            PolygonMode polygonMode,
            boolean cull,
            boolean writeColor,
            boolean writeAlpha,
            boolean writeDepth,
            LogicOp colorLogic,
            VertexFormat vertexFormat,
            VertexFormat.DrawMode vertexFormatMode,
            float depthBiasScaleFactor,
            float depthBiasConstant
    ) {
        this.location = location;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.shaderDefines = shaderDefines;
        this.samplers = samplers;
        this.uniforms = uniforms;
        this.depthTestFunction = depthTestFunction;
        this.polygonMode = polygonMode;
        this.cull = cull;
        this.blendFunction = blendFunction;
        this.writeColor = writeColor;
        this.writeAlpha = writeAlpha;
        this.writeDepth = writeDepth;
        this.colorLogic = colorLogic;
        this.vertexFormat = vertexFormat;
        this.vertexFormatMode = vertexFormatMode;
        this.depthBiasScaleFactor = depthBiasScaleFactor;
        this.depthBiasConstant = depthBiasConstant;
    }

    public String toString() {
        return this.location.toString();
    }

    public DepthTestFunction getDepthTestFunction() {
        return this.depthTestFunction;
    }

    public PolygonMode getPolygonMode() {
        return this.polygonMode;
    }

    public boolean isCull() {
        return this.cull;
    }

    public LogicOp getColorLogic() {
        return this.colorLogic;
    }

    public Optional<BlendFunction> getBlendFunction() {
        return this.blendFunction;
    }

    public boolean isWriteColor() {
        return this.writeColor;
    }

    public boolean isWriteAlpha() {
        return this.writeAlpha;
    }

    public boolean isWriteDepth() {
        return this.writeDepth;
    }

    public float getDepthBiasScaleFactor() {
        return this.depthBiasScaleFactor;
    }

    public float getDepthBiasConstant() {
        return this.depthBiasConstant;
    }

    public Identifier getLocation() {
        return this.location;
    }

    public VertexFormat getVertexFormat() {
        return this.vertexFormat;
    }

    public VertexFormat.DrawMode getVertexFormatMode() {
        return this.vertexFormatMode;
    }

    public Identifier getVertexShader() {
        return this.vertexShader;
    }

    public Identifier getFragmentShader() {
        return this.fragmentShader;
    }

    public Defines getShaderDefines() {
        return this.shaderDefines;
    }

    public List<String> getSamplers() {
        return this.samplers;
    }

    public List<RenderPipeline.UniformDescription> getUniforms() {
        return this.uniforms;
    }

    public boolean wantsDepthTexture() {
        return this.depthTestFunction != DepthTestFunction.NO_DEPTH_TEST || this.depthBiasConstant != 0.0F || this.depthBiasScaleFactor != 0.0F || this.writeDepth;
    }

    public static RenderPipeline.Builder builder(RenderPipeline.Snippet... snippets) {
        RenderPipeline.Builder builder = new RenderPipeline.Builder();

        for (RenderPipeline.Snippet snippet : snippets) {
            builder.withSnippet(snippet);
        }

        return builder;
    }

    @Environment(EnvType.CLIENT)
    @DeobfuscateClass
    public static class Builder {
        private Optional<Identifier> location = Optional.empty();
        private Optional<Identifier> fragmentShader = Optional.empty();
        private Optional<Identifier> vertexShader = Optional.empty();
        private Optional<Defines.Builder> definesBuilder = Optional.empty();
        private Optional<List<String>> samplers = Optional.empty();
        private Optional<List<RenderPipeline.UniformDescription>> uniforms = Optional.empty();
        private Optional<DepthTestFunction> depthTestFunction = Optional.empty();
        private Optional<PolygonMode> polygonMode = Optional.empty();
        private Optional<Boolean> cull = Optional.empty();
        private Optional<Boolean> writeColor = Optional.empty();
        private Optional<Boolean> writeAlpha = Optional.empty();
        private Optional<Boolean> writeDepth = Optional.empty();
        private Optional<LogicOp> colorLogic = Optional.empty();
        private Optional<BlendFunction> blendFunction = Optional.empty();
        private Optional<VertexFormat> vertexFormat = Optional.empty();
        private Optional<VertexFormat.DrawMode> vertexFormatMode = Optional.empty();
        private float depthBiasScaleFactor;
        private float depthBiasConstant;

        Builder() {
        }

        public RenderPipeline.Builder withLocation(String location) {
            this.location = Optional.of(Identifier.ofVanilla(location));
            return this;
        }

        public RenderPipeline.Builder withLocation(Identifier location) {
            this.location = Optional.of(location);
            return this;
        }

        public RenderPipeline.Builder withFragmentShader(String fragmentShader) {
            this.fragmentShader = Optional.of(Identifier.ofVanilla(fragmentShader));
            return this;
        }

        public RenderPipeline.Builder withFragmentShader(Identifier fragmentShader) {
            this.fragmentShader = Optional.of(fragmentShader);
            return this;
        }

        public RenderPipeline.Builder withVertexShader(String string) {
            this.vertexShader = Optional.of(Identifier.ofVanilla(string));
            return this;
        }

        public RenderPipeline.Builder withVertexShader(Identifier vertexShader) {
            this.vertexShader = Optional.of(vertexShader);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String flag) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(Defines.builder());
            }

            ((Defines.Builder)this.definesBuilder.get()).flag(flag);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String name, int value) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(Defines.builder());
            }

            ((Defines.Builder)this.definesBuilder.get()).define(name, value);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String name, float value) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(Defines.builder());
            }

            ((Defines.Builder)this.definesBuilder.get()).define(name, value);
            return this;
        }

        public RenderPipeline.Builder withSampler(String sampler) {
            if (this.samplers.isEmpty()) {
                this.samplers = Optional.of(new ArrayList());
            }

            ((List)this.samplers.get()).add(sampler);
            return this;
        }

        public RenderPipeline.Builder withUniform(String name, UniformType type) {
            if (this.uniforms.isEmpty()) {
                this.uniforms = Optional.of(new ArrayList());
            }

            ((List)this.uniforms.get()).add(new RenderPipeline.UniformDescription(name, type));
            return this;
        }

        public RenderPipeline.Builder withDepthTestFunction(DepthTestFunction depthTestFunction) {
            this.depthTestFunction = Optional.of(depthTestFunction);
            return this;
        }

        public RenderPipeline.Builder withPolygonMode(PolygonMode polygonMode) {
            this.polygonMode = Optional.of(polygonMode);
            return this;
        }

        public RenderPipeline.Builder withCull(boolean cull) {
            this.cull = Optional.of(cull);
            return this;
        }

        public RenderPipeline.Builder withBlend(BlendFunction blendFunction) {
            this.blendFunction = Optional.of(blendFunction);
            return this;
        }

        public RenderPipeline.Builder withoutBlend() {
            this.blendFunction = Optional.empty();
            return this;
        }

        public RenderPipeline.Builder withColorWrite(boolean writeColor) {
            this.writeColor = Optional.of(writeColor);
            this.writeAlpha = Optional.of(writeColor);
            return this;
        }

        public RenderPipeline.Builder withColorWrite(boolean writeColor, boolean writeAlpha) {
            this.writeColor = Optional.of(writeColor);
            this.writeAlpha = Optional.of(writeAlpha);
            return this;
        }

        public RenderPipeline.Builder withDepthWrite(boolean writeDepth) {
            this.writeDepth = Optional.of(writeDepth);
            return this;
        }

        public RenderPipeline.Builder withColorLogic(LogicOp colorLogic) {
            this.colorLogic = Optional.of(colorLogic);
            return this;
        }

        public RenderPipeline.Builder withVertexFormat(VertexFormat vertexFormat, VertexFormat.DrawMode vertexFormatMode) {
            this.vertexFormat = Optional.of(vertexFormat);
            this.vertexFormatMode = Optional.of(vertexFormatMode);
            return this;
        }

        public RenderPipeline.Builder withDepthBias(float depthBiasScaleFactor, float depthBiasConstant) {
            this.depthBiasScaleFactor = depthBiasScaleFactor;
            this.depthBiasConstant = depthBiasConstant;
            return this;
        }

        void withSnippet(RenderPipeline.Snippet snippet) {
            if (snippet.vertexShader.isPresent()) {
                this.vertexShader = snippet.vertexShader;
            }

            if (snippet.fragmentShader.isPresent()) {
                this.fragmentShader = snippet.fragmentShader;
            }

            if (snippet.shaderDefines.isPresent()) {
                if (this.definesBuilder.isEmpty()) {
                    this.definesBuilder = Optional.of(Defines.builder());
                }

                Defines defines = (Defines)snippet.shaderDefines.get();

                for (Entry<String, String> entry : defines.values().entrySet()) {
                    ((Defines.Builder)this.definesBuilder.get()).define((String)entry.getKey(), (String)entry.getValue());
                }

                for (String string : defines.flags()) {
                    ((Defines.Builder)this.definesBuilder.get()).flag(string);
                }
            }

            snippet.samplers.ifPresent(samplers -> {
                if (this.samplers.isPresent()) {
                    ((List)this.samplers.get()).addAll(samplers);
                } else {
                    this.samplers = Optional.of(new ArrayList(samplers));
                }
            });
            snippet.uniforms.ifPresent(uniforms -> {
                if (this.uniforms.isPresent()) {
                    ((List)this.uniforms.get()).addAll(uniforms);
                } else {
                    this.uniforms = Optional.of(new ArrayList(uniforms));
                }
            });
            if (snippet.depthTestFunction.isPresent()) {
                this.depthTestFunction = snippet.depthTestFunction;
            }

            if (snippet.cull.isPresent()) {
                this.cull = snippet.cull;
            }

            if (snippet.writeColor.isPresent()) {
                this.writeColor = snippet.writeColor;
            }

            if (snippet.writeAlpha.isPresent()) {
                this.writeAlpha = snippet.writeAlpha;
            }

            if (snippet.writeDepth.isPresent()) {
                this.writeDepth = snippet.writeDepth;
            }

            if (snippet.colorLogic.isPresent()) {
                this.colorLogic = snippet.colorLogic;
            }

            if (snippet.blendFunction.isPresent()) {
                this.blendFunction = snippet.blendFunction;
            }

            if (snippet.vertexFormat.isPresent()) {
                this.vertexFormat = snippet.vertexFormat;
            }

            if (snippet.vertexFormatMode.isPresent()) {
                this.vertexFormatMode = snippet.vertexFormatMode;
            }
        }

        public RenderPipeline.Snippet buildSnippet() {
            return new RenderPipeline.Snippet(
                    this.vertexShader,
                    this.fragmentShader,
                    this.definesBuilder.map(Defines.Builder::build),
                    this.samplers.map(Collections::unmodifiableList),
                    this.uniforms.map(Collections::unmodifiableList),
                    this.blendFunction,
                    this.depthTestFunction,
                    this.polygonMode,
                    this.cull,
                    this.writeColor,
                    this.writeAlpha,
                    this.writeDepth,
                    this.colorLogic,
                    this.vertexFormat,
                    this.vertexFormatMode
            );
        }

        public RenderPipeline build() {
            if (this.location.isEmpty()) {
                throw new IllegalStateException("Missing location");
            } else if (this.vertexShader.isEmpty()) {
                throw new IllegalStateException("Missing vertex shader");
            } else if (this.fragmentShader.isEmpty()) {
                throw new IllegalStateException("Missing fragment shader");
            } else if (this.vertexFormat.isEmpty()) {
                throw new IllegalStateException("Missing vertex buffer format");
            } else if (this.vertexFormatMode.isEmpty()) {
                throw new IllegalStateException("Missing vertex mode");
            } else {
                return new RenderPipeline(
                        (Identifier)this.location.get(),
                        (Identifier)this.vertexShader.get(),
                        (Identifier)this.fragmentShader.get(),
                        ((Defines.Builder)this.definesBuilder.orElse(Defines.builder())).build(),
                        List.copyOf((Collection)this.samplers.orElse(new ArrayList())),
                        (List<RenderPipeline.UniformDescription>)this.uniforms.orElse(Collections.emptyList()),
                        this.blendFunction,
                        (DepthTestFunction)this.depthTestFunction.orElse(DepthTestFunction.LEQUAL_DEPTH_TEST),
                        (PolygonMode)this.polygonMode.orElse(PolygonMode.FILL),
                        (Boolean)this.cull.orElse(true),
                        (Boolean)this.writeColor.orElse(true),
                        (Boolean)this.writeAlpha.orElse(true),
                        (Boolean)this.writeDepth.orElse(true),
                        (LogicOp)this.colorLogic.orElse(LogicOp.NONE),
                        (VertexFormat)this.vertexFormat.get(),
                        (VertexFormat.DrawMode)this.vertexFormatMode.get(),
                        this.depthBiasScaleFactor,
                        this.depthBiasConstant
                );
            }
        }
    }

    @Environment(EnvType.CLIENT)
    @DeobfuscateClass
    public record Snippet(
            Optional<Identifier> vertexShader,
            Optional<Identifier> fragmentShader,
            Optional<Defines> shaderDefines,
            Optional<List<String>> samplers,
            Optional<List<RenderPipeline.UniformDescription>> uniforms,
            Optional<BlendFunction> blendFunction,
            Optional<DepthTestFunction> depthTestFunction,
            Optional<PolygonMode> polygonMode,
            Optional<Boolean> cull,
            Optional<Boolean> writeColor,
            Optional<Boolean> writeAlpha,
            Optional<Boolean> writeDepth,
            Optional<LogicOp> colorLogic,
            Optional<VertexFormat> vertexFormat,
            Optional<VertexFormat.DrawMode> vertexFormatMode
    ) {
    }

    @Environment(EnvType.CLIENT)
    @DeobfuscateClass
    public record UniformDescription(String name, UniformType type) {
    }
}

