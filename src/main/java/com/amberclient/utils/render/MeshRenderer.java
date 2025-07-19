package com.amberclient.utils.render;

import com.amberclient.utils.blaze3d.buffers.GpuBuffer;
import com.amberclient.utils.blaze3d.buffers.GpuBufferSlice;
import com.amberclient.utils.blaze3d.pipeline.RenderPipeline;
import com.amberclient.utils.blaze3d.systems.RenderPass;
import com.amberclient.utils.blaze3d.systems.DeviceManager;
import com.amberclient.utils.blaze3d.textures.OpenGLTexture;
import com.amberclient.utils.blaze3d.textures.OpenGLTextureView;
import com.amberclient.utils.blaze3d.textures.TextureFormat;
import com.amberclient.utils.minecraft.MinecraftUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.amberclient.utils.blaze3d.textures.GpuTextureView;
import com.amberclient.utils.blaze3d.vertex.VertexFormat;
import com.amberclient.utils.core.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class MeshRenderer {
    private static final MeshRenderer INSTANCE = new MeshRenderer();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static boolean taken;

    private GpuTextureView colorAttachment;
    private GpuTextureView depthAttachment;
    private Color clearColor;
    private RenderPipeline pipeline;
    private MeshBuilder mesh;
    private Matrix4f matrix;
    private final HashMap<String, GpuBufferSlice> uniforms = new HashMap<>();
    private final HashMap<String, GpuTextureView> samplers = new HashMap<>();

    private MeshRenderer() {}

    public static MeshRenderer begin() {
        if (taken)
            throw new IllegalStateException("Previous instance of MeshRenderer was not ended");

        taken = true;
        return INSTANCE;
    }

    public MeshRenderer attachments(GpuTextureView color, GpuTextureView depth) {
        colorAttachment = color;
        depthAttachment = depth;
        return this;
    }

    public MeshRenderer attachments(Framebuffer framebuffer) {
        colorAttachment = createTextureViewFromOpenGL(
                framebuffer.getColorAttachment(),
                framebuffer.textureWidth,
                framebuffer.textureHeight
        );

        if (framebuffer.useDepthAttachment && framebuffer.getDepthAttachment() != -1) {
            depthAttachment = createTextureViewFromOpenGL(
                    framebuffer.getDepthAttachment(),
                    framebuffer.textureWidth,
                    framebuffer.textureHeight
            );
        } else {
            depthAttachment = null;
        }

        return this;
    }

    private GpuTextureView createTextureViewFromOpenGL(int textureId, int width, int height) {
        if (textureId == -1) {
            return null;
        }

        TextureFormat format = TextureFormat.RGBA8_UNORM;

        OpenGLTexture texture = new OpenGLTexture(
                "FramebufferTexture",
                textureId,
                width,
                height,
                format
        );

        return new OpenGLTextureView(texture, 0, 1);
    }

    public MeshRenderer clearColor(Color color) {
        clearColor = color;
        return this;
    }

    public MeshRenderer pipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh) {
        this.mesh = mesh;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, Matrix4f matrix) {
        this.mesh = mesh;
        this.matrix = matrix;
        return this;
    }

    public MeshRenderer mesh(MeshBuilder mesh, MatrixStack matrices) {
        this.mesh = mesh;
        this.matrix = matrices.peek().getPositionMatrix();
        return this;
    }

    public MeshRenderer uniform(String name, GpuBufferSlice slice) {
        uniforms.put(name, slice);
        return this;
    }

    public MeshRenderer sampler(String name, GpuTextureView view) {
        if (name != null && view != null) {
            samplers.put(name, view);
        }

        return this;
    }

    public void end() {
        if (mesh.isBuilding()) {
            mesh.end();
        }

        if (mesh.getIndicesCount() > 0) {
            if (MinecraftUtils.rendering3D || matrix != null) {
                RenderSystem.getModelViewStack().pushMatrix();
            }

            if (matrix != null) {
                RenderSystem.getModelViewStack().mul(matrix);
            }

            if (MinecraftUtils.rendering3D) {
                applyCameraPos();
            }

            GpuBuffer vertexBuffer = mesh.getVertexBuffer();
            GpuBuffer indexBuffer = mesh.getIndexBuffer();

            {
                OptionalInt clearColor = this.clearColor != null ?
                        OptionalInt.of(ColorHelper.getArgb(this.clearColor.alpha(), this.clearColor.red(), this.clearColor.green(), this.clearColor.blue())) :
                        OptionalInt.empty();

                GpuBufferSlice meshData = MeshUniforms.write(RenderUtils.projection, RenderSystem.getModelViewStack());

                RenderPass pass = (RenderPass) ((depthAttachment != null && pipeline.wantsDepthTexture()) ?
                                        DeviceManager.getRequiredDevice().createCommandEncoder().createRenderPass(() -> "Meteor MeshRenderer", colorAttachment, clearColor, depthAttachment, OptionalDouble.empty()) :
                                        DeviceManager.getRequiredDevice().createCommandEncoder().createRenderPass(() -> "Meteor MeshRenderer", colorAttachment, clearColor));

                pass.setPipeline(pipeline);
                pass.setUniform("MeshData", meshData);

                for (var name : uniforms.keySet()) {
                    pass.setUniform(name, uniforms.get(name));
                }

                for (var name : samplers.keySet()) {
                    pass.bindSampler(name, samplers.get(name));
                }

                pass.setVertexBuffer(0, vertexBuffer);
                pass.setIndexBuffer(indexBuffer, VertexFormat.IndexType.INT);
                pass.drawIndexed(0, 0, mesh.getIndicesCount(), 1);

                pass.close();
            }

            if (MinecraftUtils.rendering3D || matrix != null) {
                RenderSystem.getModelViewStack().popMatrix();
            }
        }

        colorAttachment = null;
        depthAttachment = null;
        clearColor = null;
        pipeline = null;
        mesh = null;
        matrix = null;
        uniforms.clear();
        samplers.clear();

        taken = false;
    }

    private static void applyCameraPos() {
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        RenderSystem.getModelViewStack().translate(0, (float) -cameraPos.y, 0);
    }
}