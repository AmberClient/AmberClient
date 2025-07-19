package com.amberclient.utils.blaze3d.systems;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class DeviceManager {
    @Nullable
    private static GpuDevice device;

    /**
     * Sets the global GPU device instance
     */
    public static void setDevice(GpuDevice device) {
        DeviceManager.device = device;
    }

    /**
     * Gets the global GPU device instance
     * @return the GPU device, or null if not initialized
     */
    @Nullable
    public static GpuDevice getDevice() {
        return device;
    }

    /**
     * Gets the global GPU device instance, throwing an exception if not initialized
     * @return the GPU device
     * @throws IllegalStateException if the device is not initialized
     */
    public static GpuDevice getRequiredDevice() {
        if (device == null) {
            throw new IllegalStateException("GpuDevice not initialized. Call DeviceManager.setDevice() first.");
        }
        return device;
    }

    /**
     * Checks if the device is initialized
     */
    public static boolean isDeviceInitialized() {
        return device != null;
    }

    /**
     * Clears the device reference (useful for cleanup)
     */
    public static void clearDevice() {
        device = null;
    }
}