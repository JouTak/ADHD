package ru.joutak.adhd

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import ru.joutak.minigames.MiniGamesCore
import java.io.File

class ADHDPlugin : JavaPlugin() {
    companion object {
        @JvmStatic
        lateinit var instance: ADHDPlugin
    }

    private var customConfig = YamlConfiguration()

    private fun loadConfig() {
        val fx = File(dataFolder, "config.yml")
        if (!fx.exists()) {
            saveResource("config.yml", true)
        }
    }

    /**
     * Plugin startup logic
     */
    override fun onEnable() {
        instance = this

        MiniGamesCore.initialize(this)

        loadConfig()

        // Register commands and events

        logger.info("Плагин ${pluginMeta.name} версии ${pluginMeta.version} включен!")
    }

    /**
     * Plugin shutdown logic
     */
    override fun onDisable() {
    }
}
