package ru.joutak.adhd.listener.mode.parkour

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.ParkourGame
import ru.joutak.adhd.tournament.TournamentManager

class FallListener : Listener {

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val game = TournamentManager.getGame(player)

        if (game is ParkourGame && game.state == GameState.RUN) {
            val loc = player.location

            if (loc.y <= game.borderY) {
                val checkpoint = game.spawns[player.uniqueId]?.lastOrNull() ?: return
                player.teleport(game.toLocation(checkpoint))
            }
        }
    }
}
