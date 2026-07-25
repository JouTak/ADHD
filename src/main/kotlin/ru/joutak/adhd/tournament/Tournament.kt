package ru.joutak.adhd.tournament

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.concrete.PVPGame
import ru.joutak.adhd.world.Arena
import java.util.*
import kotlin.math.floor

class Tournament(val participants: MutableList<UUID>, val modesPool: List<String>) {

    var currentTick = 0L

    var currentMode = ""

    var modePointer = 0

    val ticker = object : BukkitRunnable() {
        override fun run() {
            tick()
        }
    }

    var winners = emptySet<UUID>()

    lateinit var currentGame: Game

    var status = TournamentStatus.START

    lateinit var worldName: String

    lateinit var adjustedMaps: Map<Int, List<Arena>>

    val tournamentResults = mutableMapOf<UUID, Double>()

    val timeBossBar = TimeBossBar()

    val pointsScoreboardManager = PointsScoreboardManager()

    fun start(worldName: String, adjustedMaps: Map<Int, List<Arena>>) {
        ADHDPlugin.instance.logger.info("Начат турнир $this")

        this.worldName = worldName
        this.adjustedMaps = adjustedMaps

        pointsScoreboardManager.create(this)

        ticker.runTaskTimer(ADHDPlugin.instance, 0L, 4L)
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

                currentTick = 0

                var arenaPointer = 0

                //TODO: Make random more random

                participants.shuffle()

                val assignedMembers = mutableMapOf<UUID, Arena>()

                for (i in 0 until participants.size - 1 step 2) {
                    val arena = arenas[arenaPointer++]

                    assignedMembers[participants[i]] = arena
                    assignedMembers[participants[i + 1]] = arena
                }

                val toIgnore = participants.toSet() - assignedMembers.keys

                for (uuid in toIgnore) {
                    val player = Bukkit.getPlayer(uuid)!!

                    player.sendMessage(Component.text("Вы не учавствуете в текущей игре..."))
                }

                val world = Bukkit.getWorld(worldName)!!

                for (arena in arenas) {
                    for (spawn in arena.spawnPoints) {
                        world.loadChunk(floor(spawn.x / 16).toInt(), floor(spawn.z / 16).toInt())
                    }
                }

                currentGame = when(currentMode) {
                    "PVP" -> PVPGame()
                    else -> return
                }

                timeBossBar.create(this)

                currentGame.start(actualMode, assignedMembers, worldName)

                status = TournamentStatus.RUNNING
            }
            TournamentStatus.RUNNING -> {
                if (currentTick >= ADHDConfig.modes[currentMode]!!.duration * 20L) {
                    status = TournamentStatus.PREPARING

                    timeBossBar.update()

                    val gameResults = currentGame.finish()

                    for (uuid in gameResults.keys) {
                        tournamentResults.putIfAbsent(uuid, 0.0)

                        val result = gameResults[uuid]!!

                        tournamentResults[uuid] = tournamentResults[uuid]!! + result
                    }

                    pointsScoreboardManager.updateAll()

                    checkWin()

                    return
                }

                currentGame.update()

                timeBossBar.update()

                currentTick += 4L
            }
            TournamentStatus.FINISH -> {
                finish()
            }
        }
    }

    fun checkWin() {
        winners = tournamentResults
            .filterValues { it >= 10.0 }
            .keys

        if (winners.isNotEmpty()) {
            status = TournamentStatus.FINISH
        }
    }

    fun remove(player: Player) {
        participants.remove(player.uniqueId)

        currentGame.remove(player.uniqueId)

        timeBossBar.remove(player)

        player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
    }

    fun finish() {
        status = TournamentStatus.FINISH

        timeBossBar.clear()

        ADHDPlugin.instance.logger.info("Завершён турнир $this")

        try {
            ticker.cancel()
        } catch (_: IllegalStateException) {}

        TournamentManager.finish(this)
    }
}