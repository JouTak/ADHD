package ru.joutak.adhd.config

import org.bukkit.configuration.file.YamlConfiguration
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.world.SpawnPoint
import java.io.File

object ADHDConfig {

    var maxPlayers: Int = 4
        private set

    var maps = mapOf<Int, Map<Int, SpawnPoint>>()
        private set

    fun load() {
        val file = File(ADHDPlugin.instance.dataFolder, "config.yml")

        if (!file.exists()) {
            ADHDPlugin.instance.saveResource("config.yml", true)
        }

        val config = YamlConfiguration.loadConfiguration(file)

        maxPlayers = config.getInt("default.maxPlayers", 4)

        maps = loadMaps(config)

        ADHDPlugin.instance.logger.info("Карты: $maps")
    }

    fun loadMaps(config: YamlConfiguration): Map<Int, Map<Int, SpawnPoint>> {
        val result = mutableMapOf<Int, MutableMap<Int, SpawnPoint>>()

        val maps = config.getConfigurationSection("maps") ?: return emptyMap()

        for (mapId in maps.getKeys(false)) {
            val mapIndex = mapId.toInt()
            val spawns = maps.getConfigurationSection("$mapId.spawns") ?: continue

            val spawnMap = mutableMapOf<Int, SpawnPoint>()

            for (spawnId in spawns.getKeys(false)) {
                val spawnIndex = spawnId.toInt()

                spawnMap[spawnIndex] = SpawnPoint(
                    x = spawns.getDouble("$spawnId.x"),
                    y = spawns.getDouble("$spawnId.y"),
                    z = spawns.getDouble("$spawnId.z"),
                    yaw = spawns.getDouble("$spawnId.yaw").toFloat(),
                    pitch = spawns.getDouble("$spawnId.pitch").toFloat()
                )
            }

            result[mapIndex] = spawnMap
        }

        return result
    }
}