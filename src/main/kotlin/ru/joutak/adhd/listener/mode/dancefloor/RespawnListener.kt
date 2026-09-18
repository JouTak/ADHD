package ru.joutak.adhd.listener.mode.dancefloor

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.DanceFloorGame
import ru.joutak.adhd.tournament.TournamentManager

class RespawnListener: Listener {
    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent){
        val game = TournamentManager.getGame(event.player)
        if (game != null && game.getGameState() == GameState.RUN && game is DanceFloorGame){
            game.spawnPlayer(event.player, game.respawns[event.player.uniqueId])
        }
    }
}