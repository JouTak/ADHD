package ru.joutak.adhd.ui

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.tournament.Tournament

class TimeBossBar {
    val bossBar = Bukkit.createBossBar("Загрузка...", BarColor.BLUE, BarStyle.SOLID)

    val bossBarSingle = Bukkit.createBossBar("Загрузка...", BarColor.BLUE, BarStyle.SOLID)

    lateinit var tournament: Tournament

    fun load(tournament: Tournament) {
        this.tournament = tournament
    }

    fun add(player: Player) {
        bossBar.addPlayer(player)
    }

    fun remove(player: Player) {
        switchSingle(player, false)

        bossBar.removePlayer(player)
    }

    fun removeAll() {
        bossBar.removeAll()
        bossBarSingle.removeAll()
    }

    fun switchSingle(player: Player, state: Boolean) {
        if (state) {
            remove(player)

            bossBarSingle.addPlayer(player)
        } else {
            bossBarSingle.removePlayer(player)

            add(player)
        }
    }

    fun update() {
        val variant = tournament.gameSequence[tournament.round]
        val configDuration = variant.durationSeconds * 20

        val remaining = (configDuration - tournament.currentTick).coerceAtLeast(0)

        bossBar.setTitle(formatTitle(remaining, ADHDConfig.modes[variant.modeName]!!.displayName))

        tournament.single?.let { (_, name) ->
            bossBarSingle.setTitle(formatTitle(remaining, ADHDConfig.modes[name]!!.displayName))
        }

        val progress = (remaining.toDouble() / configDuration).coerceIn(0.0, 1.0)

        bossBar.progress = progress

        bossBarSingle.progress = progress
    }

    fun formatTitle(remaining: Long, name: String): String {
        return "§7[§6${name}§7] Осталось: ${formatTime(remaining)}"
    }

    private fun formatTime(ticks: Long): String {
        var seconds = ticks / 20

        val minutes = seconds / 60

        seconds -= minutes * 60

        val nil = if (seconds < 10) {
            "0"
        } else {
            ""
        }

        return "§r$minutes:$nil$seconds"
    }
}
