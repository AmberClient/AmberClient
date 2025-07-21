package com.amberclient.modules.minigames.murdererfinder

import com.amberclient.utils.minecraft.MinecraftUtils

object ModProperties {
    const val MOD_ID = "mmhelper"
    val METADATA = MinecraftUtils.getModMetadata(MOD_ID)
    val MOD_NAME: String = METADATA.name
    val MOD_VERSION = Version(METADATA.version.friendlyString)
    val MC_VERSION = Version("1.21.4")
}