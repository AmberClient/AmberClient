package com.amberclient.modules.miscellaneous

import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class NoAnimations : Module("NoAnimations", "Disable Amber Client's animations", ModuleCategory.MISC) {

    companion object {
        const val MOD_ID = "amberclient-noanimations"
        val LOGGER: Logger? = LogManager.getLogger(MOD_ID)
    }

}