package com.amberclient.utils.minecraft;

import com.amberclient.utils.accessors.MinecraftServerAccessor;
import com.mojang.authlib.GameProfile;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MinecraftUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static double frameTime;

    private static final Set<EntityType<?>> HOSTILE_ENTITIES = new HashSet<>();
    private static final Set<EntityType<?>> NEUTRAL_ENTITIES = new HashSet<>();
    private static final Set<EntityType<?>> PASSIVE_ENTITIES = new HashSet<>();

    static {
        initializeEntitySets();
    }

    private static void initializeEntitySets() {
        HOSTILE_ENTITIES.addAll(Arrays.asList(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                EntityType.ENDERMAN, EntityType.WITCH, EntityType.BLAZE, EntityType.GHAST,
                EntityType.WITHER_SKELETON, EntityType.STRAY, EntityType.HUSK, EntityType.DROWNED,
                EntityType.PHANTOM, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
                EntityType.RAVAGER, EntityType.VEX, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN,
                EntityType.SHULKER, EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.CAVE_SPIDER,
                EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN_BRUTE,
                EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.WARDEN
        ));

        NEUTRAL_ENTITIES.addAll(Arrays.asList(
                EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.WOLF, EntityType.LLAMA,
                EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.PANDA, EntityType.BEE,
                EntityType.DOLPHIN, EntityType.POLAR_BEAR
        ));

        PASSIVE_ENTITIES.addAll(Arrays.asList(
                EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
                EntityType.RABBIT, EntityType.VILLAGER, EntityType.HORSE, EntityType.DONKEY,
                EntityType.MULE, EntityType.CAT, EntityType.OCELOT, EntityType.PARROT,
                EntityType.BAT, EntityType.SQUID, EntityType.MOOSHROOM, EntityType.TURTLE,
                EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH,
                EntityType.AXOLOTL, EntityType.GLOW_SQUID, EntityType.GOAT, EntityType.ALLAY,
                EntityType.FROG, EntityType.TADPOLE, EntityType.CAMEL, EntityType.SNIFFER
        ));
    }

    // Entity classification methods
    public static Set<EntityType<?>> getHostileEntities() { return Set.copyOf(HOSTILE_ENTITIES); }
    public static Set<EntityType<?>> getNeutralEntitiesEntities() { return Set.copyOf(NEUTRAL_ENTITIES); }
    public static Set<EntityType<?>> getPassiveEntities() { return Set.copyOf(PASSIVE_ENTITIES); }

    public static boolean isHostile(EntityType<?> entityType) { return HOSTILE_ENTITIES.contains(entityType); }
    public static boolean isNeutral(EntityType<?> entityType) { return NEUTRAL_ENTITIES.contains(entityType); }
    public static boolean isPassive(EntityType<?> entityType) { return PASSIVE_ENTITIES.contains(entityType); }

    public static boolean isMob(EntityType<?> entityType) { return isHostile(entityType) ||isNeutral(entityType) || isPassive(entityType); }

    public static String getEntityCategory(EntityType<?> entityType) {
        if (entityType == EntityType.PLAYER)
            return "Player";
        else if (isHostile(entityType))
            return "Hostile";
        else if (isNeutral(entityType))
            return "Neutral";
        else if (isPassive(entityType))
            return "Passive";
        else
            return "Unknown";
    }

    // World utilities
    public static String getWorldName() {
        // Singleplayer
        if (mc.isInSingleplayer()) {
            if (mc.world == null) return "";
            if (mc.getServer() == null) return "FAILED_BECAUSE_LEFT_WORLD";

            File folder = ((MinecraftServerAccessor) mc.getServer()).getSession().getWorldDirectory(mc.world.getRegistryKey()).toFile();
            if (folder.toPath().relativize(mc.runDirectory.toPath()).getNameCount() != 2) {
                folder = folder.getParentFile();
            }
            return folder.getName();
        }

        // Multiplayer
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().isRealm() ? "realms" : mc.getCurrentServerEntry().address;
        }

        return "";
    }

    public static boolean isChatOpen() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;
    }

    public static void sendChatMessage(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.inGameHud != null)
            mc.inGameHud.getChatHud().addMessage(message);
    }

    public static void updateFrameTime(float tickDelta) {
        frameTime = tickDelta / 20.0;
    }

    public static void sendChatMessage(String message) {
        sendChatMessage(Text.literal(message));
    }

    public static boolean isPlayerInTabList(PlayerEntity player) {
        return isPlayerInTabList(player.getGameProfile());
    }

    public static boolean isPlayerInTabList(GameProfile profile) {
        if (MinecraftClient.getInstance().player != null) {
            String name = profile.getName();
            for (PlayerListEntry entry : MinecraftClient.getInstance().player.networkHandler.getPlayerList())
                if (entry.getProfile().getName().equals(name))
                    return true;
        }
        return false;
    }

    public static ModMetadata getModMetadata(String modId) {
        ModContainer container = FabricLoader.getInstance().getModContainer(modId).get();
        return container.getMetadata();
    }
}