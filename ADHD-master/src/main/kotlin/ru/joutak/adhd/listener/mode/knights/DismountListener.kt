package ru.joutak.adhd.listener.mode.knights

import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDismountEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.KnightsGame
import ru.joutak.adhd.tournament.TournamentManager

class DismountListener : Listener {

    @EventHandler
    fun onDismount(event: EntityDismountEvent) {
        if (event.entity is Player && event.dismounted is Horse) {
            val game = TournamentManager.getGame(event.entity as Player)

            if (game != null && game.getGameState() == GameState.RUN && game is KnightsGame) {
                event.isCancelled = true
            }
        }
    }
}