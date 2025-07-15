package com.amberclient.utils.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourceManager;

import java.awt.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class FontUtil {
    private static final AtomicInteger completed = new AtomicInteger(0);
    private static final int TOTAL_FONTS = 3;

    private static volatile Font normal_;

    private static Font getFont(Map<String, Font> locationMap, String location, int size) {
        Font font = null;

        try {
            if (locationMap.containsKey(location)) {
                font = locationMap.get(location).deriveFont(Font.PLAIN, size);
            } else {
                MinecraftClient client = MinecraftClient.getInstance();
                ResourceManager resourceManager = client.getResourceManager();

                Identifier fontId = Identifier.of("amberclient", "font/" + location);

                Resource resource = resourceManager.getResource(fontId).orElse(null);
                if (resource == null) {
                    throw new RuntimeException("Font resource not found: " + fontId);
                }

                try (InputStream is = resource.getInputStream()) {
                    font = Font.createFont(Font.TRUETYPE_FONT, is);
                    locationMap.put(location, font);
                    font = font.deriveFont(Font.PLAIN, (float) size);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading font: " + location);
            font = new Font("SansSerif", Font.PLAIN, size);
        }

        return font;
    }

    public static boolean hasLoaded() {
        return completed.get() >= TOTAL_FONTS;
    }

    public static void bootstrap() {
        CompletableFuture<Font> normalFontFuture = CompletableFuture.supplyAsync(() -> {
            Map<String, Font> locationMap = new HashMap<>();
            Font font = getFont(locationMap, "JetBrainsMono-Regular.ttf", 19);
            completed.incrementAndGet();
            return font;
        });

        CompletableFuture<Void> task2 = CompletableFuture.runAsync(() -> {
            Map<String, Font> locationMap = new HashMap<>();
            completed.incrementAndGet();
        });

        CompletableFuture<Void> task3 = CompletableFuture.runAsync(() -> {
            Map<String, Font> locationMap = new HashMap<>();
            completed.incrementAndGet();
        });

        try {
            normal_ = normalFontFuture.get();
            task2.get();
            task3.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            normal_ = new Font("SansSerif", Font.PLAIN, 19);
        }
    }

    public static CompletableFuture<Void> bootstrapAsync() {
        return CompletableFuture.runAsync(() -> {
            Map<String, Font> locationMap = new HashMap<>();
            normal_ = getFont(locationMap, "JetBrainsMono-Regular.ttf", 19);
            completed.incrementAndGet();
        }).thenRunAsync(() -> {
            Map<String, Font> locationMap = new HashMap<>();
            completed.incrementAndGet();
        }).thenRunAsync(() -> {
            Map<String, Font> locationMap = new HashMap<>();
            completed.incrementAndGet();
        });
    }

    public static Font getNormalFont() {
        return normal_;
    }

    public static boolean isFontAvailable(String fontPath) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            ResourceManager resourceManager = client.getResourceManager();

            Identifier fontId = Identifier.of("amberclient", "font/" + fontPath);
            return resourceManager.getResource(fontId).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public static Font loadCustomFont(String fontPath, int size) {
        Map<String, Font> locationMap = new HashMap<>();
        return getFont(locationMap, fontPath, size);
    }

    public static void cleanup() {
        normal_ = null;
        completed.set(0);
    }
}