package ru.joutak.adhd.event.pvp

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.joutak.adhd.game.concrete.PVPGame
import ru.joutak.adhd.tournament.TournamentManager

class RespawnListener : Listener {

    @EventHandler
    fun onRespawn(event: PlayerPostRespawnEvent) {
        val game = TournamentManager.getGame(event.player)

        if (game != null && game is PVPGame) {
            val arena = game.getArena(event.player)

            game.restoreArenaMembers(arena)

            game.calculatePoint(event.player)
        }
    }
}