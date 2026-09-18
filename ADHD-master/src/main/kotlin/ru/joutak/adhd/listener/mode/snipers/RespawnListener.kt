package ru.joutak.adhd.listener.mode.snipers

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.SnipersGame
import ru.joutak.adhd.tournament.TournamentManager

class RespawnListener : Listener {

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val game = TournamentManager.getGame(event.player)

        if (game != null && game.getGameState() == GameState.RUN && game is SnipersGame) {
            game.calculateResult(event.player)

            game.finish()

            Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {game.teleportToSpawn(event.player)})
        }
    }
}