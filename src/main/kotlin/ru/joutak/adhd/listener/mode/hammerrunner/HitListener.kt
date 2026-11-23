package ru.joutak.adhd.listener.mode.hammerrunner

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.HammerRunnerGame
import ru.joutak.adhd.tournament.TournamentManager

class HitListener: Listener {
    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent){
        val player: Player = event.damager as? Player ?: return
        val game = TournamentManager.getGame(player)
        if (game != null && game.getGameState() == GameState.RUN && game is HammerRunnerGame){
            if (event.entity == game.rabbit && player.activeItem == game.hammer) game.hit()
        }
    }
}