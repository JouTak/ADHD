package ru.joutak.adhd.config

import org.bukkit.configuration.file.YamlConfiguration
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.Mode
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.io.File

object ADHDConfig {

    /*
    Запишите сюда названия режимов для поиска при загрузке.
     */
    val registeredModes = listOf<String>("PVP")

    var maxPlayers: Int = 4
        private set

    var maps = mapOf<Int, Arena>()
        private set

    var modes = mapOf<String, Mode>()
        private set

    var pointsGoal: Double = 10.0
        private set

    var templateWorldName: String = "template"
        private set

    fun load() {
        val file = File(ADHDPlugin.instance.dataFolder, "config.yml")

        if (!file.exists()) {
            ADHDPlugin.instance.saveResource("config.yml", true)
        }

        val config = YamlConfiguration.loadConfiguration(file)

        maxPlayers = config.getInt("default.maxPlayers", 4)

        pointsGoal = config.getDouble("default.pointsGoal", 10.0)

        templateWorldName = config.getString("default.templateWorldName", "template") ?: "template"

        maps = loadMaps(config)

        ADHDPlugin.instance.logger.info("Карты: $maps")

        modes = loadModes()

        ADHDPlugin.instance.logger.info("Режимы: $modes")
    }

    fun loadMaps(config: YamlConfiguration): Map<Int, Arena> {
        val result = mutableMapOf<Int, Arena>()

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

            if (!spawnMap.isEmpty()) {
                val meta = maps.getConfigurationSection("$mapId.meta")?.getValues(false) ?: emptyMap<String, Any>()

                result[mapIndex] = Arena(spawnMap.values.toList(), meta)
            }
        }

        return result
    }

    fun loadModes(): Map<String, Mode> {
        val result = mutableMapOf<String, Mode>()

        for (name in registeredModes) {
            val file = File(ADHDPlugin.instance.dataFolder, "config_$name.yml")

            if (!file.exists()) {
                ADHDPlugin.instance.saveResource("config_$name.yml", true)
            }
        }

        val files = ADHDPlugin.instance.dataFolder.listFiles()

        val regex = Regex("""^config_([a-z0-9]+)\.yml$""", RegexOption.IGNORE_CASE)

        for (file in files) {
            val match = regex.find(file.name)

            if (match != null) {
                val modeName = match.groupValues[1]

                if (registeredModes.contains(modeName)) {
                    val config = YamlConfiguration.loadConfiguration(file)

                    val enabled = config.getBoolean("default.enabled")

                    if (enabled) {
                        val duration = config.getInt("default.duration", 60)

                        val maps = config.getIntegerList("default.maps")

                        if (!maps.isEmpty()) {
                            val meta = config.getConfigurationSection("meta")?.getValues(false) ?: emptyMap<String, Any>()

                            val mode = Mode(duration, maps, meta)

                            result[modeName] = mode
                        }
                    }
                }
            }
        }

        return result
    }
}