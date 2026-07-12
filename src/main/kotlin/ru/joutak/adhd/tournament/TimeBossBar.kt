package ru.joutak.adhd.tournament

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import ru.joutak.adhd.config.ADHDConfig

class TimeBossBar {
    lateinit var bossBar: BossBar

    lateinit var tournament: Tournament

    fun create(tournament: Tournament) {
        this.tournament = tournament

        if (!::bossBar.isInitialized) {
            bossBar = Bukkit.createBossBar(formatTitle(ADHDConfig.modes[tournament.currentMode]!!.duration * 20L), BarColor.BLUE, BarStyle.SOLID)

            for (uuid in tournament.participants) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                bossBar.addPlayer(player)
            }
        } else {
            update()
        }
    }

    fun formatTitle(ticks: Long): String {
        return "§7[§6${tournament.currentMode}§7] §7Осталось: ${formatTime(ticks)}"
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

    fun update() {
        val configDuration = ADHDConfig.modes[tournament.currentMode]!!.duration * 20L

        val remaining = (configDuration - tournament.currentTick).coerceAtLeast(0)

        bossBar.setTitle(formatTitle(remaining))

        bossBar.progress = (remaining.toDouble() / configDuration.toDouble()).coerceIn(0.0, 1.0)
    }

    fun remove(player: Player) {
        if (::bossBar.isInitialized) {
            bossBar.removePlayer(player)
        }
    }

    fun clear() {
        if (::bossBar.isInitialized) {
            bossBar.removeAll()
        }
    }
}