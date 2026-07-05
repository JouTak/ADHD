package ru.joutak.adhd.config

import org.bukkit.configuration.file.YamlConfiguration
import ru.joutak.adhd.ADHDPlugin
import java.io.File

object ADHDConfig {

    fun load() {
        val file = File(ADHDPlugin.instance.dataFolder, "config.yml")

        if (!file.exists()) {
            ADHDPlugin.instance.saveResource("config.yml", true)
        }

        val config = YamlConfiguration.loadConfiguration(file)
    }
}