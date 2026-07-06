package ru.joutak.adhd.tournament

import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.world.SpawnPoint
import java.util.*

class Tournament(val participants: MutableList<UUID>) {

    fun start(pair: Pair<String, Map<Int, List<SpawnPoint>>>) {
        ADHDPlugin.instance.logger.info("${pair.first} ${pair.second}")
    }
}