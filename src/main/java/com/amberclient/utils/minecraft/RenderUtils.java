package com.amberclient.utils.minecraft;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderUtils {

    public static void renderSingleLine(MatrixStack stack, VertexConsumer buffer, float x1, float y1, float z1,
                                        float x2, float y2,
                                        float z2, float r, float g, float b, float a) {
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        normal.normalize();
        renderSingleLine(stack, buffer, x1, y1, z1, x2, y2, z2, r, g, b, a, normal.x(), normal.y(),
                normal.z());
    }

    public static void renderSingleLine(MatrixStack stack, VertexConsumer buffer, float x1, float y1, float z1,
                                        float x2, float y2,
                                        float z2, float r, float g, float b, float a, float normalX, float normalY, float normalZ) {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();
        MatrixStack.Entry entry = stack.peek();
        buffer.vertex(matrix4f, x1, y1, z1).color(r, g, b, a)
                .normal(entry, normalX, normalY, normalZ);
        buffer.vertex(matrix4f, x2, y2, z2).color(r, g, b, a)
                .normal(entry, normalX, normalY, normalZ);
    }

    public static void drawBox(MatrixStack stack, VertexConsumer buffer, Box box, float r, float g, float b, float a) {
        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Bottom face edges
        renderSingleLine(stack, buffer, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        renderSingleLine(stack, buffer, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        renderSingleLine(stack, buffer, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        renderSingleLine(stack, buffer, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Top face edges
        renderSingleLine(stack, buffer, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        renderSingleLine(stack, buffer, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        renderSingleLine(stack, buffer, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        renderSingleLine(stack, buffer, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Vertical edges
        renderSingleLine(stack, buffer, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        renderSingleLine(stack, buffer, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        renderSingleLine(stack, buffer, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        renderSingleLine(stack, buffer, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }
}