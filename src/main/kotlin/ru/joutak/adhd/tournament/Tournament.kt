package ru.joutak.adhd.tournament

import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.world.Arena
import java.util.*

class Tournament(val participants: MutableList<UUID>, val modesPool: List<String>) {

    var tick = 0L

    var currentMode = ""

    var modePointer = 0

    val ticker = object : BukkitRunnable() {
        override fun run() {
            tick()
        }
    }

    var status = TournamentStatus.START

    lateinit var worldName: String

    lateinit var adjustedMaps: Map<Int, List<Arena>>

    fun start(worldName: String, adjustedMaps: Map<Int, List<Arena>>) {
        ADHDPlugin.instance.logger.info("Начата игра в мире $worldName, кол-во игроков ${participants.size}, размер пула режимов ${modesPool.size}")

        this.worldName = worldName
        this.adjustedMaps = adjustedMaps

        ticker.runTaskTimer(ADHDPlugin.instance, 0L, 2L)
    }

    fun tick() {
        when(status) {
            TournamentStatus.START -> status = TournamentStatus.PREPARING
            TournamentStatus.PREPARING -> {

            }
            TournamentStatus.RUNNING -> {

            }
            TournamentStatus.FINISH -> {

            }
        }
    }
}