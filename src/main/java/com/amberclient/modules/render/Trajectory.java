package com.amberclient.modules.render;

import com.amberclient.utils.module.ConfigurableModule;
import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ModuleCategory;
import com.amberclient.utils.module.ModuleSettings;
import com.amberclient.utils.minecraft.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

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
    private final ModuleSettings debugMode;

    // Predefined color values
    private static final int LINE_COLOR_R = 255;
    private static final int LINE_COLOR_G = 0;
    private static final int LINE_COLOR_B = 0;
    private static final float LINE_COLOR_A = 1.0f;

    private final List<ModuleSettings> settings;

    public Trajectory() {
        super("Trajectory", "Renders the trajectory of projectiles", ModuleCategory.RENDER);

        // Initialize trigger items
        simpleItems.add(Items.SNOWBALL);
        simpleItems.add(Items.ENDER_PEARL);
        simpleItems.add(Items.EGG);
        complexItems.add(Items.BOW);

        // Define configurable settings
        lineVisibility = new ModuleSettings("Line Visibility", "Whether to show the trajectory line", true);
        boxVisibility = new ModuleSettings("Box Visibility", "Whether to show the hit box", true);
        approxBoxVisibility = new ModuleSettings("Approx Box Visibility", "Whether to show the approximate hit box", true);
        arrowTrajectory = new ModuleSettings("Arrow Trajectory", "Whether to show trajectory for arrows", true);
        lineOrigin = new ModuleSettings("Line Origin", "Origin of the trajectory line (1=Left, 2=Auto, 3=Right)", 2, 1, 3);
        debugMode = new ModuleSettings("Debug Mode", "Print debug information", false);

        // Initialize settings list
        settings = List.of(lineVisibility, boxVisibility, approxBoxVisibility, arrowTrajectory, lineOrigin, debugMode);
    }

    @Override
    public List<ModuleSettings> getSettings() {
        return settings;
    }

    @Override
    public void render(MatrixStack stack) {
        if (!isEnabled()) return;

        MinecraftClient client = getClient();
        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        // Debug pour savoir si la fonction est bien appelée
        if (debugMode.getBooleanValue()) {
            System.out.println("Trajectory render called");
        }

        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        Item mainItem = mainHandStack.getItem();
        Item offItem = offHandStack.getItem();

        boolean isSimple = simpleItems.contains(mainItem) || simpleItems.contains(offItem);
        boolean isComplex = complexItems.contains(mainItem) || complexItems.contains(offItem);

        if (!isSimple && !isComplex) return;

        float speed;
        float gravity;
        boolean mainHand = simpleItems.contains(mainItem) || complexItems.contains(mainItem);

        if (isSimple) {
            speed = 1.5f;
            gravity = 0.03f;
        } else {
            if (!arrowTrajectory.getBooleanValue()) return;

            if (mainItem != Items.BOW && offItem != Items.BOW) return;

            float bowMultiplier = 0f;
            if (player.isUsingItem() && (player.getActiveItem().getItem() == Items.BOW)) {
                float useTime = 72000.0f - player.getItemUseTimeLeft();
                bowMultiplier = (useTime / 20.0f);
                bowMultiplier = (bowMultiplier * bowMultiplier + bowMultiplier * 2.0f) / 3.0f;
                bowMultiplier = Math.min(bowMultiplier, 1.0f);
            } else {
                bowMultiplier = 0.3f;
            }

            speed = bowMultiplier * 3.0f;
            gravity = 0.05f;

            if (debugMode.getBooleanValue()) {
                System.out.println("=== TRAJECTORY DEBUG ===");
                System.out.println("Is using item: " + player.isUsingItem());
                System.out.println("Item use time left: " + player.getItemUseTimeLeft());
                System.out.println("Active item: " + (player.getActiveItem() != null ? player.getActiveItem().getItem() : "null"));
                System.out.println("Bow multiplier: " + bowMultiplier);
                System.out.println("Speed: " + speed);
                System.out.println("========================");
            }

            if (speed < 0.1f) speed = 0.1f;
        }

        int playerside = determinePlayerSide(mainHand, player);

        // Configuration OpenGL pour lignes visibles
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        // Fixed: Use setShader with ShaderProgramKey instead of method reference
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        stack.push();
        try {
            Tessellator tessellator = Tessellator.getInstance();
            // Fixed: Use begin method from Tessellator instead of getBuffer
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            Camera camera = client.gameRenderer.getCamera();
            Vec3d cameraPos = camera.getPos();
            Vec3d playerPos = player.getPos();
            double playerX = playerPos.x;
            double playerY = playerPos.y + player.getStandingEyeHeight();
            double playerZ = playerPos.z;

            float pitch = player.getPitch();
            float yaw = player.getYaw();
            double radYaw = Math.toRadians(yaw);
            double radPitch = Math.toRadians(pitch);

            double vx = -Math.sin(radYaw) * Math.cos(radPitch);
            double vy = -Math.sin(radPitch);
            double vz = Math.cos(radYaw) * Math.cos(radPitch);

            Vec3d velocity = new Vec3d(vx, vy, vz).normalize().multiply(speed);
            Vec3d currentPos = new Vec3d(playerX, playerY, playerZ);

            for (int i = 0; i < 100; i++) {
                Vec3d nextPos = currentPos.add(velocity);

                // Affichage de la ligne
                if (lineVisibility.getBooleanValue()) {
                    RenderUtils.renderSingleLine(
                            stack, buffer,
                            (float)(currentPos.x - cameraPos.x),
                            (float)(currentPos.y - cameraPos.y),
                            (float)(currentPos.z - cameraPos.z),
                            (float)(nextPos.x - cameraPos.x),
                            (float)(nextPos.y - cameraPos.y),
                            (float)(nextPos.z - cameraPos.z),
                            LINE_COLOR_R / 255f,
                            LINE_COLOR_G / 255f,
                            LINE_COLOR_B / 255f,
                            LINE_COLOR_A
                    );
                }

                // Collision avec bloc
                HitResult hit = client.world.raycast(new RaycastContext(
                        currentPos, nextPos,
                        RaycastContext.ShapeType.OUTLINE,
                        RaycastContext.FluidHandling.NONE,
                        player
                ));

                if (hit.getType() != HitResult.Type.MISS) {
                    Vec3d hitPos = hit.getPos();
                    double distance = hitPos.distanceTo(player.getPos());
                    double boxSize = distance > 30 ? distance / 70 : 0.5;
                    double defaultBoxSize = 0.5;

                    if (boxVisibility.getBooleanValue()) {
                        renderBox(stack, buffer,
                                hitPos.x - defaultBoxSize - cameraPos.x,
                                hitPos.y - defaultBoxSize - cameraPos.y,
                                hitPos.z - defaultBoxSize - cameraPos.z,
                                hitPos.x + defaultBoxSize - cameraPos.x,
                                hitPos.y + defaultBoxSize - cameraPos.y,
                                hitPos.z + defaultBoxSize - cameraPos.z);
                    }

                    if (approxBoxVisibility.getBooleanValue()) {
                        renderBox(stack, buffer,
                                hitPos.x - boxSize - cameraPos.x,
                                hitPos.y - boxSize - cameraPos.y,
                                hitPos.z - boxSize - cameraPos.z,
                                hitPos.x + boxSize - cameraPos.x,
                                hitPos.y + boxSize - cameraPos.y,
                                hitPos.z + boxSize - cameraPos.z);
                    }

                    break;
                }

                currentPos = nextPos;
                velocity = velocity.multiply(0.99);
                velocity = new Vec3d(velocity.x, velocity.y - gravity, velocity.z);

                if (currentPos.y < player.getWorld().getBottomY()) break;
            }

            // Fixed: Use BufferRenderer.drawWithGlobalProgram with the built mesh
            BufferRenderer.drawWithGlobalProgram(buffer.build());

        } catch (Exception e) {
            System.err.println("Erreur lors du rendu de la trajectoire : " + e.getMessage());
            e.printStackTrace();
        } finally {
            stack.pop();
        }

        // Restauration OpenGL
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private int determinePlayerSide(boolean mainHand, PlayerEntity player) {
        int lineOriginValue = lineOrigin.getIntValue();
        int playerside = 1;
        if (lineOriginValue != 2) {
            playerside = (lineOriginValue == 3 ? 1 : -1);
        } else {
            playerside = (mainHand ? 1 : -1);
        }
        if (player.getMainArm() == Arm.LEFT) {
            playerside *= -1;
        }
        return playerside;
    }

    private void renderBox(MatrixStack stack, VertexConsumer buffer, double x1, double y1, double z1, double x2, double y2, double z2) {
        Box box = new Box(x1, y1, z1, x2, y2, z2);
        RenderUtils.drawBox(stack, buffer, box, LINE_COLOR_R / 255f, LINE_COLOR_G / 255f, LINE_COLOR_B / 255f, LINE_COLOR_A);
    }

    @Override
    public void onSettingChanged(@NotNull ModuleSettings setting) { }
}