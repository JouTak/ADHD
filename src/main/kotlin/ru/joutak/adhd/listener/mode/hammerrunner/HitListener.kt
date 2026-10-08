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
    fun onHit(event: EntityDamageByEntityEvent){
        val player: Player = event.damager as? Player ?: return
        val game = TournamentManager.getGame(player)
        if (game != null && game.getGameState() == GameState.RUN && game is HammerRunnerGame){
            player.sendMessage((event.entity == game.rabbit).toString() + " | " + (player.inventory.itemInMainHand == game.hammer))
            if (event.entity == game.rabbit && player.inventory.itemInMainHand == game.hammer) game.hit()
        }
        event.isCancelled = true
    }
}