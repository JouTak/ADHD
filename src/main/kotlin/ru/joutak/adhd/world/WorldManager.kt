package ru.joutak.adhd.world

import org.bukkit.Bukkit
import org.bukkit.GameRules
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.tournament.Tournament
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object WorldManager {

    var worldId = 0

    fun isAvailable(): Boolean {
        val worldFolder = Bukkit.getServer()
            .levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve("template")
            .toFile()

        val existedBefore = worldFolder.exists()

        if (!existedBefore) return false

        val template = Bukkit.createWorld(WorldCreator("template"))

        var available = false

        if (template != null) {
            available = true

            Bukkit.unloadWorld("template", false)
        }

        return available
    }

    fun generate(tournament: Tournament) {
        Bukkit.getScheduler().runTaskAsynchronously(ADHDPlugin.instance, Runnable {
            val pair = copy(tournament)

            Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                Bukkit.createWorld(WorldCreator(pair.first))

                tournament.start(pair.first, pair.second)
            })
        })
    }

    fun copy(tournament: Tournament): Pair<String, Map<Int, List<Arena>>> {
        val source = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve("template")

        val worldName = "template_${worldId++}"

        val target = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(worldName)

        copyFolderFiltered(source, target)

        val adjustedMaps = mutableMapOf<Int, MutableList<Arena>>()

        for (mapId in ADHDConfig.maps.keys) {
            adjustedMaps[mapId] = mutableListOf()

            val spawns = ADHDConfig.maps[mapId]!!

            for (i in 0..<(tournament.participants.size / 2)) {
                val adjustedSpawns = mutableListOf<SpawnPoint>()

                for (spawnId in spawns.keys) {
                    val oSpawn = spawns[spawnId]!!

                    val nSpawn = SpawnPoint(512 * i + oSpawn.x, oSpawn.y, 512 * mapId + oSpawn.z, oSpawn.yaw, oSpawn.pitch)

                    adjustedSpawns.add(nSpawn)
                }

                adjustedMaps[mapId]!!.add(Arena(adjustedSpawns))
            }
        }

        copyRegions(tournament, worldName)

        return Pair(worldName, adjustedMaps)
    }

    fun copyRegions(tournament: Tournament, worldName: String) {
        val regionsFolder = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(worldName)
            .resolve("region")
            .toFile()

        ADHDPlugin.instance.logger.info("${regionsFolder.listFiles().mapNotNull { file -> file.name }}")
    }

    fun copyFolderFiltered(source: Path, target: Path) {
        Files.walk(source).forEach { path ->
            val relative = source.relativize(path)
            val destination = target.resolve(relative)
            val fileName = path.fileName?.toString()

            if (fileName == "metadata.dat") return@forEach

            if (Files.isDirectory(path)) {
                Files.createDirectories(destination)
            } else {
                Files.copy(
                    path,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
                )
            }
        }
    }

    fun getLobbyWorld(): World {
        var lobby = Bukkit.getWorld("lobby")

        if (lobby == null) {
            lobby = Bukkit.createWorld(WorldCreator("lobby").type(WorldType.NORMAL))
        }

        if (lobby == null) {
            lobby = Bukkit.getWorld("overworld")

            ADHDPlugin.instance.logger.warning("Couldn't load lobby. Using default world as fallback")
        }

        lobby!!.setGameRule(GameRules.SPAWN_MOBS, false)
        lobby.setGameRule(GameRules.SPAWN_MONSTERS, false)
        lobby.setGameRule(GameRules.FALL_DAMAGE, false)
        lobby.setGameRule(GameRules.FIRE_DAMAGE, false)
        lobby.setGameRule(GameRules.FREEZE_DAMAGE, false)

        return lobby
    }

    fun shutdown() {
        val lobby = Bukkit.getWorld("lobby")

        if (lobby != null) {
            Bukkit.unloadWorld(lobby, false)
        }
    }
}