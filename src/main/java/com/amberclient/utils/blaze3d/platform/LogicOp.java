package com.amberclient.utils.blaze3d.platform;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum LogicOp {
    NONE,
    OR_REVERSE;
}
