package ru.joutak.adhd.world

import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import ru.joutak.adhd.ADHDPlugin
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
            val pair = copy()

            Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                Bukkit.createWorld(WorldCreator(pair.first))

                tournament.start(pair.first, pair.second)
            })
        })
    }

    fun copy(): Pair<String, Map<Int, List<Arena>>> {
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

        return Pair(worldName, emptyMap())
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
}