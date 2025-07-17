package com.amberclient.utils.minecraft.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.joml.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

import java.lang.Math;

public class NametagUtils {
    private static final Vector4f vec4 = new Vector4f();
    private static final Vector4f mmMat4 = new Vector4f();
    private static final Vector4f pmMat4 = new Vector4f();
    private static final Vector3d camera = new Vector3d();
    private static final Vector3d cameraNegated = new Vector3d();
    private static final Matrix4f model = new Matrix4f();
    private static final Matrix4f projection = new Matrix4f();
    private static double windowScale;
    public static double scale;

    private NametagUtils() {
    }

    public static void onRender(Matrix4f modelView) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        camera.set(camPos.x, camPos.y, camPos.z);
        cameraNegated.set(camera);
        cameraNegated.negate();
        model.set(modelView);
        projection.set(RenderSystem.getProjectionMatrix());
        windowScale = mc.getWindow().calculateScaleFactor(1, false);
    }

    public static boolean to2D(Vector3d pos, double scale) {
        return to2D(pos, scale, true);
    }

    public static boolean to2D(Vector3d pos, double scale, boolean distanceScaling) {
        return to2D(pos, scale, distanceScaling, false);
    }

    public static boolean to2D(Vector3d pos, double scale, boolean distanceScaling, boolean allowBehind) {
        MinecraftClient mc = MinecraftClient.getInstance();
        NametagUtils.scale = scale;
        if (distanceScaling) {
            NametagUtils.scale *= getScale(pos);
        }
        vec4.set(cameraNegated.x + pos.x, cameraNegated.y + pos.y, cameraNegated.z + pos.z, 1);
        vec4.mul(model, mmMat4);
        mmMat4.mul(projection, pmMat4);
        boolean behind = pmMat4.w <= 0.f;
        if (behind && !allowBehind) return false;
        toScreen(pmMat4);
        double x = pmMat4.x * mc.getWindow().getFramebufferWidth();
        double y = pmMat4.y * mc.getWindow().getFramebufferHeight();
        if (behind) {
            x = mc.getWindow().getFramebufferWidth() - x;
            y = mc.getWindow().getFramebufferHeight() - y;
        }
        if (Double.isInfinite(x) || Double.isInfinite(y)) return false;
        pos.set(x / windowScale, mc.getWindow().getFramebufferHeight() - y / windowScale, allowBehind ? pmMat4.w : pmMat4.z);
        return true;
    }

    public static void begin(Vector3d pos, DrawContext drawContext) {
        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(pos.x, pos.y, 0);
        drawContext.getMatrices().scale((float) scale, (float) scale, 1);
    }

    public static void end(DrawContext drawContext) {
        drawContext.getMatrices().pop();
    }

    private static double getScale(Vector3d pos) {
        double dx = camera.x - pos.x;
        double dy = camera.y - pos.y;
        double dz = camera.z - pos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return MathHelper.clamp(1 - dist * 0.01, 0.5, Integer.MAX_VALUE);
    }

    private static void toScreen(Vector4f vec) {
        float newW = 1.0f / vec.w * 0.5f;
        vec.x = vec.x * newW + 0.5f;
        vec.y = vec.y * newW + 0.5f;
        vec.z = vec.z * newW + 0.5f;
        vec.w = newW;
    }
}