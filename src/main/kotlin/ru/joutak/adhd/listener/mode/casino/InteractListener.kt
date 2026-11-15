package ru.joutak.adhd.listener.mode.casino

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.casino.CasinoGame
import ru.joutak.adhd.game.concrete.casino.CasinoMenu
import ru.joutak.adhd.tournament.TournamentManager

class InteractListener : Listener {
    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        val item = e.item ?: return
        if (item.type != Material.NETHER_STAR) return
        val game = TournamentManager.getGame(e.player) as? CasinoGame ?: return
        if (game.getGameState() != GameState.RUN) return
        e.isCancelled = true
        CasinoMenu.openColorMenu(e.player, game.getPlayerBalance(e.player.uniqueId), game.playerGoal)
    }
}