package ru.joutak.adhd.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import ru.joutak.adhd.tournament.Tournament
import java.util.*
import kotlin.math.floor

class GameScoreboardManager(val tournament: Tournament) {
    val scoreboards = mutableMapOf<UUID, Scoreboard>()

    fun add(player: Player) {
        val scoreboard = Bukkit.getScoreboardManager().newScoreboard

        val objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY, Component.text("Статистика").color(NamedTextColor.GOLD))

        objective.displaySlot = DisplaySlot.SIDEBAR

        scoreboards[player.uniqueId] = scoreboard

        player.scoreboard = scoreboard
    }

    fun remove(player: Player) {
        scoreboards.remove(player.uniqueId)

        player.scoreboard = Bukkit.getScoreboardManager().mainScoreboard
    }

    fun removeAll() {
        for (uuid in scoreboards.keys.toSet()) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            remove(player)
        }
    }

    fun updateAll() {
        val leaderboard = mutableListOf<String>()

        tournament.results.filter { tournament.participants.contains(it.key) }.entries
            .sortedByDescending { it.value }
            .take(3)
            .forEachIndexed { index, (uuid, points) -> leaderboard.add("§7${index + 1}. §r${Bukkit.getPlayer(uuid)?.displayName} §7(§r${floor(points).toInt()} §6★§7)") }

        scoreboards.keys.forEach { uUID -> update(uUID, leaderboard) }
    }

    fun update(uuid: UUID, leaderboard: List<String>) {
        val scoreboard = scoreboards[uuid]!!

        val objective = scoreboard.getObjective(DisplaySlot.SIDEBAR)!!

        scoreboard.entries.forEach { entry ->
            scoreboard.resetScores(entry)
        }

        var i = 15

        objective.getScore(" ".repeat(8)).score = i--

        if (leaderboard.isNotEmpty()) {
            leaderboard.forEach { s -> objective.getScore(s).score = i-- }

            objective.getScore(" ".repeat(16)).score = i--
        }

        objective.getScore("§7Ваши очки: §r${floor(tournament.results[uuid] ?: 0.0).toInt()} §6★").score = i
    }
}