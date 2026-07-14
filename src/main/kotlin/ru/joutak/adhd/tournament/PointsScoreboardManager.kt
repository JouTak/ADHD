package ru.joutak.adhd.tournament

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import java.util.*
import kotlin.math.floor

class PointsScoreboardManager {
    val scoreboards = mutableMapOf<UUID, Scoreboard>()

    lateinit var tournament: Tournament

    fun create(tournament: Tournament) {
        this.tournament = tournament

        for (uuid in tournament.participants) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            val scoreboard = Bukkit.getScoreboardManager().newScoreboard

            val objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, Component.text("Статистика").color(
                NamedTextColor.GOLD))

            objective.displaySlot = DisplaySlot.SIDEBAR

            scoreboards[uuid] = scoreboard

            player.scoreboard = scoreboard

            update(0.0, scoreboard)
        }
    }

    fun updateAll() {
        for (uuid in scoreboards.keys) {
            val points = tournament.tournamentResults[uuid] ?: 0.0

            val scoreboard = scoreboards[uuid]!!

            update(points, scoreboard)
        }
    }

    fun update(points: Double, scoreboard: Scoreboard) {
        val objective = scoreboard.getObjective(DisplaySlot.SIDEBAR)!!

        scoreboard.entries.forEach { entry ->
            scoreboard.resetScores(entry)
        }

        objective.getScore(" ".repeat(16)).score = 1

        objective.getScore("§7Очки: §r${floor(points).toInt()}").score = 0
    }
}