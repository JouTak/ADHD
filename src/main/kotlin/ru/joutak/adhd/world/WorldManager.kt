package ru.joutak.adhd.world

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.tournament.Tournament
import ru.joutak.adhd.tournament.TournamentStatus
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.math.ceil

object WorldManager {

    val worldNameByTournaments = mutableMapOf<Tournament, String>()

    var worldId = 0

    fun isAvailable(): Boolean {
        val worldFolder = Bukkit.getServer()
            .levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(ADHDConfig.templateWorldName)
            .toFile()

        val existedBefore = worldFolder.exists()

        if (!existedBefore) return false

        val template = Bukkit.createWorld(WorldCreator(ADHDConfig.templateWorldName))

        var available = false

        if (template != null) {
            available = true

            Bukkit.unloadWorld(template, false)
        }

        return available
    }

    fun generate(tournament: Tournament) {
        Bukkit.getScheduler().runTaskAsynchronously(ADHDPlugin.instance, Runnable {
            copy(tournament)
        })
    }

    fun copy(tournament: Tournament) {
        val startTime = System.currentTimeMillis()

        val source = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(ADHDConfig.templateWorldName)

        val worldName = "${ADHDConfig.templateWorldName}_${worldId++}"

        val target = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(worldName)

        copyFolderFiltered(source, target)

        val arenas = mutableMapOf<Int, List<Arena>>()

        val arenaPointers = mutableMapOf<Int, Int>()

        var round = 0

        for (stage in tournament.gameSequence) {
            val i = stage.mapId
            arenaPointers.putIfAbsent(i, 0)

            val adjustedArenas = mutableListOf<Arena>()

            val configMap = ADHDConfig.configMaps[i]!!.copy()

            for (j in 0..<ceil(tournament.participants.size / 2.0).toInt()) {
                val spawns = mutableListOf<SpawnPoint>()

                for (spawn in configMap.spawnPoints) {
                    spawns.add(SpawnPoint(spawn.x + arenaPointers[i]!! * 512, spawn.y, spawn.z + 512 * i, spawn.yaw, spawn.pitch))
                }

                arenaPointers[i] = arenaPointers[i]!! + 1

                adjustedArenas.add(Arena(spawns.toList(), configMap.metas))
            }

            arenas[round++] = adjustedArenas
        }

        val singleArenas = mutableMapOf<String, List<Arena>>()

        for (name in ADHDConfig.singleModeNames) {
            val adjustedArenas = mutableListOf<Arena>()

            for (i in ADHDConfig.modes[name]!!.maps) {
                arenaPointers.putIfAbsent(i, 0)

                val configMap = ADHDConfig.configMaps[i]!!.copy()

                val spawns = mutableListOf<SpawnPoint>()

                for (spawn in configMap.spawnPoints) {
                    spawns.add(SpawnPoint(spawn.x + arenaPointers[i]!! * 512, spawn.y, spawn.z + 512 * i, spawn.yaw, spawn.pitch))
                }

                arenaPointers[i] = arenaPointers[i]!! + 1

                adjustedArenas.add(Arena(spawns, configMap.metas))
            }

            singleArenas[name] = adjustedArenas
        }

        RegionManager.copy(worldName, arenaPointers)

        worldNameByTournaments[tournament] = worldName

        if (tournament.status == TournamentStatus.GENERATE) {
            try {
                Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                    val creator = WorldCreator(worldName)

                    creator.generator(VoidGenerator())

                    Bukkit.createWorld(creator)

                    ADHDPlugin.instance.logger.info("Мир для $tournament сгенерирован за ${(System.currentTimeMillis() - startTime) / 1000.0} сек.")

                    tournament.generated = true

                    tournament.start(worldName, arenas, singleArenas)
                })
            } catch (_: Exception) {}
        }
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
        val folder = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(ADHDConfig.lobbyWorld)
            .toFile()

        var lobby: World? = null

        if (folder.exists()) {
            lobby = Bukkit.createWorld(WorldCreator(ADHDConfig.lobbyWorld))
        }

        if (lobby == null) {
            lobby = Bukkit.getWorlds()[0]
        }

        return lobby!!
    }

    fun clear(tournament: Tournament) {
        val worldName = worldNameByTournaments.remove(tournament) ?: return

        val world = Bukkit.getWorld(worldName)

        if (world != null) {
            Bukkit.unloadWorld(world, false)
        }

        val worldFolder = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(worldName)
            .toFile()

        worldFolder.deleteRecursively()
    }

    fun clearOnStartUp() {
        val regex = Regex("${ADHDConfig.templateWorldName}_\\d+")

        val worldsFolder = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .toFile()

        worldsFolder.listFiles().filter { f -> regex.matches(f.name) }.forEach { file -> file.deleteRecursively() }
    }
}
