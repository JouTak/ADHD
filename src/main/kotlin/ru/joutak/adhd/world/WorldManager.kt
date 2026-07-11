package ru.joutak.adhd.world

import net.minecraft.nbt.NbtIo
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.storage.RegionFile
import net.minecraft.world.level.chunk.storage.RegionStorageInfo
import org.bukkit.Bukkit
import org.bukkit.GameRules
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
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
import kotlin.random.Random

object WorldManager {

    var worldId = 0

    val executor: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors().coerceAtMost(6))

    val tournamentWorlds = mutableMapOf<Tournament, String>()

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
            copy(tournament)
        })
    }

    fun copy(tournament: Tournament) {
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

        val chosenArenas = mutableListOf<Int>()

        for (i in tournament.modesPool.indices) {
            chosenArenas.add(ADHDConfig.modes[tournament.modesPool[i]]!!.maps[Random.nextInt(ADHDConfig.modes[tournament.modesPool[i]]!!.maps.size)])
        }

        val arenaPointers = mutableMapOf<Int, Int>()

        var round = 0

        for (i in chosenArenas) {
            arenaPointers.putIfAbsent(i, 0)

            val arenas = mutableListOf<Arena>()

            val oArena = ADHDConfig.maps[i]!!.copy()

            for (j in 0..<(tournament.participants.size / 2)) {
                val spawns = mutableListOf<SpawnPoint>()

                for (oSpawn in oArena.spawnPoints) {
                    spawns.add(SpawnPoint(oSpawn.x + arenaPointers[i]!! * 512, oSpawn.y, oSpawn.z + 512 * i, oSpawn.yaw, oSpawn.pitch))
                }

                arenaPointers[i] = arenaPointers[i]!! + 1

                arenas.add(Arena(spawns.toList(), oArena.meta))
            }

            adjustedMaps[round++] = arenas
        }

        copyRegions(worldName, arenaPointers, tournament, adjustedMaps)
    }

    fun copyRegions(
        worldName: String,
        arenaPointers: MutableMap<Int, Int>,
        tournament: Tournament,
        adjustedMaps: MutableMap<Int, MutableList<Arena>>
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
            tournamentWorlds[tournament] = worldName

            if (tournament.status != TournamentStatus.FINISH) {
                Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                    Bukkit.createWorld(WorldCreator(worldName))
                    tournament.start(worldName, adjustedMaps)
                })
            } else {
                clear(tournament)
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
        clearAll()

        val lobby = Bukkit.getWorld("lobby")

        if (lobby != null) {
            Bukkit.unloadWorld(lobby, false)
        }
    }

    fun clear(tournament: Tournament) {
        val worldName = tournamentWorlds[tournament] ?: return

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

    fun clearAll() {
        for (tournament in tournamentWorlds.keys) {
            clear(tournament)
        }
    }
}