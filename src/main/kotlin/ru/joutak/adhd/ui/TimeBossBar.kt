package ru.joutak.adhd.ui

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.game.GameState
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

    fun prepareTitle() {
        bossBar.setTitle("Переключение...")
        bossBar.progress = 1.0

        bossBarSingle.setTitle("Переключение...")
        bossBarSingle.progress = 1.0
    }

    fun update() {
        val configDuration = ADHDConfig.modes[tournament.pool[tournament.round]]!!.duration * 20

        val remaining = (configDuration - tournament.currentTick).coerceAtLeast(0)

        bossBar.setTitle(formatTitle(remaining, ADHDConfig.modes[tournament.pool[tournament.round]]!!.displayName))

        tournament.single?.let { (_, name) ->
            val duoGames = tournament.games.filter { game -> game != tournament.singleGame }
            val activeDuoGames = duoGames.count { game -> game.getGameState() == GameState.RUN }

            bossBarSingle.setTitle(
                "${formatTitle(remaining, ADHDConfig.modes[name]!!.displayName)} §7| Дуо-игры: §r$activeDuoGames/${duoGames.size}"
            )
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
