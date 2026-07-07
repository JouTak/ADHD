package ru.joutak.adhd.tournament

import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.Mode
import ru.joutak.adhd.world.Arena
import java.util.*

class Tournament(val participants: MutableList<UUID>, val modesPool: List<Mode>) {

    fun start(worldName: String, adjustedMaps: Map<Int, List<Arena>>) {
        ADHDPlugin.instance.logger.info("Start ($this): $worldName $modesPool $participants $adjustedMaps")
    }
}