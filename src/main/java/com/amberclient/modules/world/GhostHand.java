package com.amberclient.modules.world;

import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ModuleCategory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class GhostHand extends Module {

    private static final double MAX_REACH_DISTANCE = 6.0;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public GhostHand() {
        super("GhostHand", "Allows the player to open containers through walls", ModuleCategory.WORLD);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        if (mc.options.useKey.wasPressed()) {
            handleGhostInteraction();
        }
    }

    private void handleGhostInteraction() {
        PlayerEntity player = mc.player;
        World world = mc.world;

        if (player == null || world == null) return;

        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.multiply(MAX_REACH_DISTANCE));

        BlockPos targetPos = findContainerInRange(world, eyePos, endPos);

        if (targetPos != null) {
            BlockState blockState = world.getBlockState(targetPos);
            Block block = blockState.getBlock();

            if (isContainer(block)) {
                interactWithContainer(world, targetPos, blockState);
            }
        }
    }

    private BlockPos findContainerInRange(World world, Vec3d start, Vec3d end) {
        Vec3d direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);

        for (double d = 0; d <= distance; d += 0.1) {
            Vec3d currentPos = start.add(direction.multiply(d));
            BlockPos blockPos = BlockPos.ofFloored(currentPos);
            BlockState blockState = world.getBlockState(blockPos);

            if (isContainer(blockState.getBlock())) {
                return blockPos;
            }
        }

        return null;
    }

    private boolean isContainer(Block block) {
        String blockName = block.getTranslationKey();
        return blockName.contains("chest") ||
                blockName.contains("furnace") ||
                blockName.contains("dispenser") ||
                blockName.contains("dropper") ||
                blockName.contains("hopper") ||
                blockName.contains("barrel") ||
                blockName.contains("shulker_box") ||
                blockName.contains("ender_chest") ||
                blockName.contains("brewing_stand") ||
                blockName.contains("enchanting_table") ||
                blockName.contains("crafting_table") ||
                blockName.contains("smithing_table") ||
                blockName.contains("stonecutter") ||
                blockName.contains("loom") ||
                blockName.contains("grindstone") ||
                blockName.contains("cartography_table") ||
                blockName.contains("fletching_table") ||
                blockName.contains("anvil");
    }

    private void interactWithContainer(World world, BlockPos pos, BlockState blockState) {
        if (mc.player == null || mc.interactionManager == null) return;

        try {
            BlockHitResult hitResult = new BlockHitResult(
                    Vec3d.ofCenter(pos),
                    Direction.UP,
                    pos,
                    false
            );

            ActionResult result = mc.interactionManager.interactBlock(
                    mc.player,
                    Hand.MAIN_HAND,
                    hitResult
            );

            if (result.isAccepted()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } catch (Exception ignored) { }
    }

    public boolean isContainerOpen() {
        return mc.player != null && mc.player.currentScreenHandler != null &&
                !(mc.player.currentScreenHandler instanceof net.minecraft.screen.PlayerScreenHandler);
    }

    public double getReachDistance() {
        return isEnabled() ? MAX_REACH_DISTANCE : 4.5;
    }

    private boolean hasLineOfSight(World world, Vec3d start, Vec3d end) {
        if (isEnabled()) { return true; }

        assert mc.player != null;
        BlockHitResult result = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return result.getType() == HitResult.Type.MISS;
    }

    public java.util.List<BlockPos> getContainersInRange() {
        java.util.List<BlockPos> containers = new java.util.ArrayList<>();

        if (mc.player == null || mc.world == null) return containers;

        Vec3d playerPos = mc.player.getEyePos();
        int range = (int) Math.ceil(MAX_REACH_DISTANCE);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = mc.player.getBlockPos().add(x, y, z);

                    if (playerPos.distanceTo(Vec3d.ofCenter(pos)) <= MAX_REACH_DISTANCE) {
                        BlockState state = mc.world.getBlockState(pos);
                        if (isContainer(state.getBlock())) {
                            containers.add(pos);
                        }
                    }
                }
            }
        }

        return containers;
    }
}