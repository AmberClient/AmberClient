package com.amberclient.modules.render;

import com.amberclient.utils.module.ConfigurableModule;
import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ModuleCategory;
import com.amberclient.utils.module.ModuleSettings;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Trajectory extends Module implements ConfigurableModule {

    private final Set<Item> simpleItems = new HashSet<>();
    private final Set<Item> complexItems = new HashSet<>();

    private final ModuleSettings lineVisibility;
    private final ModuleSettings boxVisibility;
    private final ModuleSettings approxBoxVisibility;
    private final ModuleSettings arrowTrajectory;
    private final ModuleSettings lineOrigin;

    private final List<ModuleSettings> settings;

    private static final int[] DEFAULT_COLOR = {255, 0, 0, 100}; // R, G, B, A

    public Trajectory() {
        super("Trajectory", "Renders the trajectory of projectiles", ModuleCategory.RENDER);

        // Initialisation des items déclencheurs
        simpleItems.add(Items.SNOWBALL);
        simpleItems.add(Items.ENDER_PEARL);
        simpleItems.add(Items.EGG);
        complexItems.add(Items.BOW);

        // Définition des paramètres configurables
        lineVisibility = new ModuleSettings("Line Visibility", "Whether to show the trajectory line", true);
        boxVisibility = new ModuleSettings("Box Visibility", "Whether to show the hit box", true);
        approxBoxVisibility = new ModuleSettings("Approx Box Visibility", "Whether to show the approximate hit box", true);
        arrowTrajectory = new ModuleSettings("Arrow Trajectory", "Whether to show trajectory for arrows", true);
        lineOrigin = new ModuleSettings("Line Origin", "Origin of the trajectory line (1=Left, 2=Auto, 3=Right)", 2, 1, 3);

        settings = List.of(lineVisibility, boxVisibility, approxBoxVisibility, arrowTrajectory, lineOrigin);

        // Enregistrement de l'événement de rendu
        WorldRenderEvents.END.register(this::render);
    }

    @Override
    public List<ModuleSettings> getSettings() {
        return settings;
    }

    private void render(WorldRenderContext context) {
        if (!isEnabled()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        boolean mainHand = simpleItems.contains(mainHandStack.getItem()) || complexItems.contains(mainHandStack.getItem());

        if (!mainHand && !(simpleItems.contains(offHandStack.getItem()) || complexItems.contains(offHandStack.getItem()))) {
            return;
        }

        // Configuration du rendu
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(4.0f);

        MatrixStack stack = context.matrixStack();
        if (stack == null) return;

        stack.push();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Set the shader program
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Camera camera = context.camera();
        World world = client.world;
        BlockPos blockPos = new BlockPos.Mutable((int)player.getX(), (int)player.getY(), (int)player.getZ());
        float pitch = player.getPitch();
        float yaw = player.getYaw();
        double eye = player.getEyeY();

        int[] color = DEFAULT_COLOR;
        boolean[] booleans = {lineVisibility.getBooleanValue(), boxVisibility.getBooleanValue(), approxBoxVisibility.getBooleanValue()};
        int[] integers = {(int)lineOrigin.getValue()};

        if (simpleItems.contains(mainHand ? mainHandStack.getItem() : offHandStack.getItem())) {
            renderCurve(stack, bufferBuilder, camera, world, blockPos, pitch, yaw, eye, player, color, 1.5f, 0.03f, booleans, integers, mainHand);
        } else if (complexItems.contains(mainHand ? mainHandStack.getItem() : offHandStack.getItem()) && arrowTrajectory.getBooleanValue()) {
            float bowMultiplier = (72000.0f - player.getItemUseTimeLeft()) / 20.0f;
            bowMultiplier = (bowMultiplier * bowMultiplier + bowMultiplier * 2.0f) / 3.0f;
            if (bowMultiplier > 1.0f) bowMultiplier = 1.0f;
            float speed = bowMultiplier * 3.0f;
            renderCurve(stack, bufferBuilder, camera, world, blockPos, pitch, yaw, eye, player, color, speed, 0.05f, booleans, integers, mainHand);
        }

        // Draw the buffer
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        stack.pop();

        // Reset rendering settings
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private void renderCurve(MatrixStack stack, BufferBuilder buffer, Camera camera, World world, BlockPos pos, float pitch, float yaw, double eye, PlayerEntity player, int[] color, float speed, float gravity, boolean[] booleans, int[] integers, boolean mainHand) {
        double accurateX = player.getX();
        double accurateY = player.getY();
        double accurateZ = player.getZ();

        float drag = 0.99f;

        float entityVelX = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float entityVelY = -MathHelper.sin(pitch * 0.017453292F);
        float entityVelZ = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        Vec3d vec3d = (new Vec3d(entityVelX, entityVelY, entityVelZ)).normalize().multiply(speed);
        Vec3d entityVelocity = new Vec3d(vec3d.x, vec3d.y, vec3d.z);
        Vec3d entityPosition = new Vec3d(0, 1.5, 0);

        int playerside = (integers[0] == 2) ? (mainHand ? 1 : -1) : (integers[0] == 3 ? 1 : -1);
        if (player.getMainArm() == Arm.LEFT) playerside *= -1;

        // Correction du calcul des offsets - utilisation d'une taille plus petite et direction corrigée
        double handOffset = 0.4;
        double offsetX = playerside * handOffset * Math.cos(Math.toRadians(yaw));
        double offsetZ = playerside * handOffset * Math.sin(Math.toRadians(yaw));

        double prevX = 0;
        double prevZ = 0;

        SnowballEntity tempEntity = new SnowballEntity(world, player, new ItemStack(Items.SNOWBALL));

        Matrix4f matrix = stack.peek().getPositionMatrix();

        for (int i = 0; i < 100; i++) {
            HitResult hitResult = world.raycast(new RaycastContext(
                    new Vec3d(accurateX + entityPosition.x, accurateY + entityPosition.y, accurateZ + entityPosition.z),
                    new Vec3d(accurateX + entityPosition.x, accurateY + entityPosition.y, accurateZ + entityPosition.z).add(entityVelocity),
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, tempEntity));

            if (hitResult.getType() != HitResult.Type.MISS) {
                double hitDistance = hitResult.getPos().distanceTo(player.getPos());
                double boxSize = hitDistance > 30 ? hitDistance / 70 : 0.5;
                double defaultBoxSize = 0.5;

                if (booleans[1]) { // Box visibility - correction de la position
                    renderBox(stack, buffer, matrix,
                            hitResult.getPos().x - defaultBoxSize, hitResult.getPos().y - defaultBoxSize, hitResult.getPos().z - defaultBoxSize,
                            hitResult.getPos().x + defaultBoxSize, hitResult.getPos().y + defaultBoxSize, hitResult.getPos().z + defaultBoxSize, color);
                }
                if (booleans[2]) { // Approx box visibility - correction de la position
                    renderBox(stack, buffer, matrix,
                            hitResult.getPos().x - boxSize, hitResult.getPos().y - boxSize, hitResult.getPos().z - boxSize,
                            hitResult.getPos().x + boxSize, hitResult.getPos().y + boxSize, hitResult.getPos().z + boxSize, color);
                }
                break;
            }

            // Correction du calcul des positions - utilisation des coordonnées monde
            double startx = player.getX() + prevX + offsetX;
            double starty = player.getEyeY() + (entityPosition.y - 1.5);
            double startz = player.getZ() + prevZ + offsetZ;

            entityPosition = entityPosition.add(entityVelocity);
            entityVelocity = entityVelocity.multiply(drag);
            entityVelocity = new Vec3d(entityVelocity.x, entityVelocity.y - gravity, entityVelocity.z);

            // Suppression de la rotation complexe - utilisation directe des positions
            double newX = entityPosition.x;
            double newZ = entityPosition.z;

            if (booleans[0]) { // Line visibility - correction du calcul des positions finales
                double endx = player.getX() + newX + offsetX;
                double endy = player.getEyeY() + (entityPosition.y - 1.5);
                double endz = player.getZ() + newZ + offsetZ;
                renderSingleLine(stack, buffer, (float)startx, (float)starty, (float)startz, (float)endx, (float)endy, (float)endz, color);
            }

            prevX = newX;
            prevZ = newZ;
        }
    }

    private void renderBox(MatrixStack stack, BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, int[] color) {
        float r = color[0] / 255f;
        float g = color[1] / 255f;
        float b = color[2] / 255f;
        float a = color[3] / 100f;

        // Conversion en coordonnées relatives à la caméra
        Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        float cx1 = (float)(x1 - cameraPos.x);
        float cy1 = (float)(y1 - cameraPos.y);
        float cz1 = (float)(z1 - cameraPos.z);
        float cx2 = (float)(x2 - cameraPos.x);
        float cy2 = (float)(y2 - cameraPos.y);
        float cz2 = (float)(z2 - cameraPos.z);

        // Bottom face
        buffer.vertex(matrix, cx1, cy1, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy1, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy1, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy1, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy1, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy1, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy1, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy1, cz1).color(r, g, b, a);

        // Top face
        buffer.vertex(matrix, cx1, cy2, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy2, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy2, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy2, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy2, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy2, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy2, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy2, cz1).color(r, g, b, a);

        // Vertical edges
        buffer.vertex(matrix, cx1, cy1, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy2, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy1, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy2, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy1, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy2, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy1, cz2).color(r, g, b, a);
        buffer.vertex(matrix, cx1, cy2, cz2).color(r, g, b, a);
    }

    private void renderSingleLine(MatrixStack stack, BufferBuilder buffer, float x1, float y1, float z1, float x2, float y2, float z2, int[] color) {
        float r = color[0] / 255f;
        float g = color[1] / 255f;
        float b = color[2] / 255f;
        float a = color[3] / 100f;

        // Conversion en coordonnées relatives à la caméra
        Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        float cx1 = (float)(x1 - cameraPos.x);
        float cy1 = (float)(y1 - cameraPos.y);
        float cz1 = (float)(z1 - cameraPos.z);
        float cx2 = (float)(x2 - cameraPos.x);
        float cy2 = (float)(y2 - cameraPos.y);
        float cz2 = (float)(z2 - cameraPos.z);

        Matrix4f matrix = stack.peek().getPositionMatrix();
        buffer.vertex(matrix, cx1, cy1, cz1).color(r, g, b, a);
        buffer.vertex(matrix, cx2, cy2, cz2).color(r, g, b, a);
    }

    @Override
    public void onSettingChanged(@NotNull ModuleSettings setting) {
        if (setting == lineVisibility) {
            System.out.println("Line visibility changed to: " + setting.getBooleanValue());
        } else if (setting == boxVisibility) {
            System.out.println("Box visibility changed to: " + setting.getBooleanValue());
        } else if (setting == approxBoxVisibility) {
            System.out.println("Approx box visibility changed to: " + setting.getBooleanValue());
        } else if (setting == arrowTrajectory) {
            System.out.println("Arrow trajectory changed to: " + setting.getBooleanValue());
        } else if (setting == lineOrigin) {
            System.out.println("Line origin changed to: " + setting.getIntegerValue());
        }
    }
}