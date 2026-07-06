package ru.joutak.adhd.world

import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.tournament.Tournament

object WorldManager {

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
        return Pair("template_test", emptyMap())
    }
}