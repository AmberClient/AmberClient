package com.amberclient.utils.blaze3d.shaders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public enum ShaderType {
    VERTEX("vertex", ".vsh"),
    FRAGMENT("fragment", ".fsh");

    private static final ShaderType[] TYPES = values();
    private final String name;
    private final String extension;

    private ShaderType(final String name, final String extension) {
        this.name = name;
        this.extension = extension;
    }

    @Nullable
    public static ShaderType byLocation(Identifier id) {
        for (ShaderType shaderType : TYPES) {
            if (id.getPath().endsWith(shaderType.extension)) {
                return shaderType;
            }
        }

        return null;
    }

    public String getName() {
        return this.name;
    }

    public ResourceFinder idConverter() {
        return new ResourceFinder("shaders", this.extension);
    }
}
