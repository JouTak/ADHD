package ru.joutak.adhd.world

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.storage.RegionFile
import net.minecraft.world.level.chunk.storage.RegionStorageInfo
import org.bukkit.Bukkit
import ru.joutak.adhd.ADHDPlugin
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.jvm.optionals.getOrNull

object RegionManager {

    val executor: ExecutorService = Executors.newFixedThreadPool((Runtime.getRuntime().availableProcessors() - 2).coerceIn(1, 8))

    var cachedMain: Map<Int, Map<ChunkPos, CompoundTag>> = emptyMap()

    var cachedMainFiles = mutableMapOf<Int, File>()

    var cacheFlag = false

    val VOID_NAMES = setOf("minecraft:air", "minecraft:cave_air", "minecraft:void_air")

    fun copy(worldName: String, arenaPointers: Map<Int, Int>) {
        if (!cacheFlag) {
            cacheRegions(worldName)
        }

        val regionsFolder = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(worldName)
            .resolve("region")
            .toFile()

        val futures = mutableListOf<CompletableFuture<Void>>()

        for (regionZ in arenaPointers.keys) {
            cachedMain[regionZ] ?: continue

            val amount = arenaPointers[regionZ]!!

            for (regionX in 1..<amount) {
                val file = File(regionsFolder, "r.$regionX.$regionZ.mca")

                futures += CompletableFuture.runAsync({copySingleRegion(file, worldName, regionZ, regionX * 32)}, executor)
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()
    }

    fun copySingleRegion(target: File, worldName: String, regionZ: Int, shiftChunksX: Int) {
        RegionFile(
            RegionStorageInfo(
                worldName,
                Level.OVERWORLD,
                "region"
            ),
            target.toPath(),
            target.parentFile.toPath(),
            true
        ).use { region ->
            val chunks = cachedMain[regionZ]!!

            for ((oldPos, oTag) in chunks) {
                val tag = oTag.copy()

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

                region.getChunkDataOutputStream(ChunkPos(oldPos.x + shiftChunksX, oldPos.z)).use { out ->
                    NbtIo.write(tag, out)
                }
            }
        }
    }

    fun cacheRegions(worldName: String) {
        val regionsFolder = Bukkit.getServer().levelDirectory
            .resolve("dimensions")
            .resolve("minecraft")
            .resolve(worldName)
            .resolve("region")
            .toFile()

        val futures = mutableListOf<CompletableFuture<RegionData>>()

        val regex = Regex("""r\.0\.(\d+)\.mca""")

        cachedMainFiles.clear()

        for (file in regionsFolder.listFiles()) {
            val match = regex.find(file.name)

            if (match != null) {
                val regionZ = match.groupValues[1].toInt()

                cachedMainFiles[regionZ] = file

                futures += CompletableFuture.supplyAsync({cacheRegion(file, regionZ, worldName)}, executor)
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).join()

        cachedMain = futures.map { it.join() }.associate { it.regionId to it.chunks }

        cacheFlag = true

        ADHDPlugin.instance.logger.info("Кэшированы регионы ${cachedMain.keys}")
    }

    fun cacheRegion(source: File, regionZ: Int, worldName: String): RegionData {
        val chunks = mutableMapOf<ChunkPos, CompoundTag>()

        RegionFile(
            RegionStorageInfo(
                worldName,
                Level.OVERWORLD,
                "region"
            ),
            source.toPath(),
            source.parentFile.toPath(),
            true
        ).use { region ->
            for (x in 0 until 32) {
                for (z in 0 until 32) {
                    val pos = ChunkPos(x, regionZ * 32 + z)

                    val input = region.getChunkDataInputStream(pos) ?: continue

                    val tag = NbtIo.read(input).copy()

                    input.close()

                    if (!isEmptyChunk(tag)) {
                        chunks[pos] = tag
                    }
                }
            }
        }

        return RegionData(regionZ, chunks)
    }

    fun isEmptyChunk(chunk: CompoundTag): Boolean {
        val bEs = chunk.getList("block_entities").orElse(null)

        if (bEs != null) {
            if (!bEs.isEmpty) {
                return false
            }
        }

        for (i in chunk.getList("sections").stream()) {
            for (j in i.stream()) {
                val section = j.asCompound().orElse(null) ?: continue

                val palette = section.getCompound("block_states").orElse(null)?.getList("palette")?.orElse(null) ?: continue

                for (bTag in palette.stream()) {
                    val block = bTag.asCompound().orElse(null) ?: continue

                    val name = block.getString("Name").orElse(null) ?: continue

                    if (name !in VOID_NAMES) {
                        return false
                    }
                }
            }
        }

        return true
    }
}