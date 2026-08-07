package ru.joutak.adhd.world

import net.minecraft.nbt.NbtIo
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.storage.RegionFile
import net.minecraft.world.level.chunk.storage.RegionStorageInfo
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.tournament.Tournament
import ru.joutak.adhd.tournament.TournamentStatus
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.optionals.getOrNull

object WorldManager {

    val worldNameByTournaments = mutableMapOf<Tournament, String>()

    val executor: ExecutorService = Executors.newFixedThreadPool(4)

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

        val arenaIds = mutableListOf<Int>()

        tournament.pool.forEach { name -> arenaIds.add(ADHDConfig.modes[name]!!.maps.random()) }

        val arenas = mutableMapOf<Int, List<Arena>>()

        val arenaPointers = mutableMapOf<Int, Int>()

        var round = 0

        for (i in arenaIds) {
            arenaPointers.putIfAbsent(i, 0)

            val adjustedArenas = mutableListOf<Arena>()

            val configMap = ADHDConfig.configMaps[i]!!.copy()

            for (j in 0..<(tournament.participants.size / 2)) {
                val spawns = mutableListOf<SpawnPoint>()

                for (spawn in configMap.spawnPoints) {
                    spawns.add(SpawnPoint(spawn.x + arenaPointers[i]!! * 512, spawn.y, spawn.z + 512 * i, spawn.yaw, spawn.pitch))
                }

                arenaPointers[i] = arenaPointers[i]!! + 1

                adjustedArenas.add(Arena(spawns.toList(), configMap.metas))
            }

            arenas[round++] = adjustedArenas
        }

        copyRegions(worldName, arenaPointers, tournament, arenas)
    }

    fun copyRegions(
        worldName: String,
        arenaPointers: MutableMap<Int, Int>,
        tournament: Tournament,
        arenas: MutableMap<Int, List<Arena>>
    ) {
        val regionsFolder = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(worldName)
            .resolve("region")
            .toFile()

        val regex = Regex("""r\.0\.(\d+)\.mca""")

        val futures = mutableListOf<CompletableFuture<Void>>()

        for (file in regionsFolder.listFiles()) {
            val match = regex.find(file.name)

            if (match != null) {
                val regionZ = match.groupValues[1].toInt()

                val amount = arenaPointers[regionZ] ?: continue

                for (regionX in 1..<amount) {
                    val targetFile = File(regionsFolder, "r.$regionX.$regionZ.mca")

                    futures += CompletableFuture.runAsync({copySingleRegion(file, targetFile, 0, regionZ, 32 * regionX, worldName)}, executor)
                }
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).thenRun {
            worldNameByTournaments[tournament] = worldName

            if (tournament.status == TournamentStatus.GENERATE) {
                try {
                    Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                        val creator = WorldCreator(worldName)

                        creator.generator(VoidGenerator())

                        Bukkit.createWorld(creator)

                        tournament.generated = true

                        tournament.start(worldName, arenas)
                    })
                } catch (_: Exception) {}
            }
        }
    }

    fun copySingleRegion(
        sourceFile: File,
        targetFile: File,
        sourceRegionX: Int,
        sourceRegionZ: Int,
        shiftChunksX: Int,
        worldName: String
    ) {
        RegionFile(
            RegionStorageInfo(worldName, Level.OVERWORLD, "region"),
            sourceFile.toPath(),
            sourceFile.parentFile.toPath(),
            true
        ).use { source ->

            RegionFile(
                RegionStorageInfo(worldName, Level.OVERWORLD, "region"),
                targetFile.toPath(),
                targetFile.parentFile.toPath(),
                true
            ).use { target ->

                for (z in 0 until 32) {
                    for (x in 0 until 32) {

                        val oldPos = ChunkPos(
                            sourceRegionX * 32 + x,
                            sourceRegionZ * 32 + z
                        )

                        val input =
                            source.getChunkDataInputStream(oldPos)
                                ?: continue

                        val tag = NbtIo.read(input)

                        input.close()

                        tag.remove("structures")

                        tag.putInt(
                            "xPos",
                            tag.getInt("xPos").getOrNull()!! + shiftChunksX
                        )

                        tag.getList("block_entities")
                            .ifPresent { list ->
                                for (i in list.indices) {
                                    list.getCompound(i)
                                        .ifPresent { be ->
                                            be.putInt(
                                                "x",
                                                be.getInt("x").getOrNull()!! +
                                                        shiftChunksX * 16
                                            )
                                        }
                                }
                            }

                        val newPos = ChunkPos(
                            oldPos.x + shiftChunksX,
                            oldPos.z
                        )

                        val output =
                            target.getChunkDataOutputStream(newPos)

                        NbtIo.write(tag, output)

                        output.close()
                    }
                }
            }
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