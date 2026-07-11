package ru.joutak.adhd.tournament

import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.concrete.PVPGame
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

    lateinit var currentGame: Game

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
                if (modePointer == modesPool.size) {
                    status = TournamentStatus.FINISH

                    return
                }

                val arenas = adjustedMaps[modePointer]!!

                currentMode = modesPool[modePointer++]

                val actualMode = ADHDConfig.modes[currentMode]!!.copy()

                tick = actualMode.duration * 20L

                var arenaPointer = 0

                //TODO: Make random more random

                participants.shuffle()

                val assignedMembers = mutableMapOf<UUID, Arena>()

                for (i in participants.indices step 2) {
                    assignedMembers[participants[i]] = arenas[arenaPointer]
                    assignedMembers[participants[i + 1]] = arenas[arenaPointer++]
                }

                currentGame = when(currentMode) {
                    "PVP" -> PVPGame()
                    else -> return
                }

                currentGame.start(actualMode, assignedMembers, worldName)

                status = TournamentStatus.RUNNING
            }
            TournamentStatus.RUNNING -> {
                if (tick <= 0L) {
                    status = TournamentStatus.PREPARING

                    val gameResults = currentGame.finish()

                    return
                }

                currentGame.update()

                tick--
            }
            TournamentStatus.FINISH -> {

            }
        }
    }
}