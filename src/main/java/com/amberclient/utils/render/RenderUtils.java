package com.amberclient.utils.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.util.math.Vec3d;

import static net.minecraft.client.MinecraftClient.getInstance;

public class RenderUtils {
    public static Vec3d center;
    public static final Matrix4f projection = new Matrix4f();

    public static void updateScreenCenter(Matrix4f projection, Matrix4f view) {
        RenderUtils.projection.set(projection);

        Matrix4f invProjection = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f(view).invert();

        Vector4f center4 = new Vector4f(0, 0, 0, 1).mul(invProjection).mul(invView);
        center4.div(center4.w);

        Vec3d camera = getInstance().gameRenderer.getCamera().getPos();
        center = new Vec3d(camera.x + center4.x, camera.y + center4.y, camera.z + center4.z);
    }
}