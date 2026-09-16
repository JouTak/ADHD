package ru.joutak.adhd.listener.mode.dancefloor

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.tournament.TournamentManager

class MoveListener: Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){
        val game = TournamentManager.getGame(event.player)

    }
}