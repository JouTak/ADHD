package ru.joutak.adhd.tournament

import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.world.Arena
import java.util.*

class Tournament(val participants: MutableList<UUID>) {

    fun start(worldName: String, adjustedMaps: Map<Int, List<Arena>>) {
        ADHDPlugin.instance.logger.info("Стартуем в мире $worldName с картами $adjustedMaps с игроками $participants!")
    }
}