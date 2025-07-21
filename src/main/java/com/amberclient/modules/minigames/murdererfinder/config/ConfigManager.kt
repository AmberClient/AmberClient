package com.amberclient.modules.minigames.murdererfinder.config

import com.google.gson.GsonBuilder
import com.amberclient.modules.minigames.murdererfinder.MurdererFinder
import com.amberclient.modules.minigames.murdererfinder.ModProperties
import net.fabricmc.loader.api.FabricLoader
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object ConfigManager {
    private var initialized = false
    private var config: Config? = null
    private val defaults = Config()
    private lateinit var configFile: File

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    private val executor: Executor = Executors.newSingleThreadExecutor()

    @JvmStatic
    fun init() {
        if (initialized) return

        configFile = File(FabricLoader.getInstance().configDir.toFile(), "${ModProperties.MOD_ID}.json")
        readConfig(false)

        if (config == null) {
            MurdererFinder.logger.warn("Config is still null after initialization, using defaults")
            config = Config()
            writeConfig(false)
        }

        initialized = true
        MurdererFinder.logger.info("ConfigManager initialized successfully")
    }

    fun readConfig(async: Boolean) {
        val task = Runnable {
            try {
                if (configFile.exists()) {
                    val fileContents = FileUtils.readFileToString(configFile, Charset.defaultCharset())

                    if (fileContents.isNullOrBlank()) {
                        MurdererFinder.logger.warn("Config file is empty, creating new config")
                        writeNewConfig()
                        return@Runnable
                    }

                    val loadedConfig = gson.fromJson(fileContents, Config::class.java)

                    if (loadedConfig == null) {
                        MurdererFinder.logger.warn("Config file contains invalid JSON, creating new config")
                        writeNewConfig()
                        return@Runnable
                    }

                    config = loadedConfig

                    if (!loadedConfig.validate()) {
                        MurdererFinder.logger.info("Config validation failed, fixing and saving")
                        writeConfig(true)
                    }
                } else {
                    MurdererFinder.logger.info("Config file doesn't exist, creating new one")
                    writeNewConfig()
                }
            } catch (e: Exception) {
                MurdererFinder.logger.error("Error reading config file: ${e.message}")
                e.printStackTrace()
                writeNewConfig()
            }
        }

        if (async) {
            executor.execute(task)
        } else {
            task.run()
        }
    }

    fun writeNewConfig() {
        try {
            config = Config()
            writeConfig(false)
            MurdererFinder.logger.info("Created new config file")
        } catch (e: Exception) {
            MurdererFinder.logger.error("Failed to create new config: ${e.message}")
            e.printStackTrace()

            if (config == null) {
                config = Config()
            }
        }
    }

    fun writeConfig() {
        writeConfig(true)
    }

    fun writeConfig(async: Boolean) {
        val task = Runnable {
            try {
                config?.let { cfg ->
                    val serialized = gson.toJson(cfg)
                    FileUtils.writeStringToFile(configFile, serialized, Charset.defaultCharset())
                } ?: run {
                    MurdererFinder.logger.error("Cannot write config: config is null")
                }
            } catch (e: Exception) {
                MurdererFinder.logger.error("Error writing config file: ${e.message}")
                e.printStackTrace()
            }
        }

        if (async) {
            executor.execute(task)
        } else {
            task.run()
        }
    }

    @JvmStatic
    fun getConfig(): Config {
        return config ?: run {
            MurdererFinder.logger.warn("Config is null in getConfig(), returning defaults")
            defaults
        }
    }

    fun getDefaults(): Config = defaults
}
