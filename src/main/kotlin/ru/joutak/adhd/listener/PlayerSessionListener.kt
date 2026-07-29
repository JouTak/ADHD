package ru.joutak.adhd.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import ru.joutak.adhd.tournament.TournamentManager

class PlayerSessionListener : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (TournamentManager.isInLobby(event.player)) {
            TournamentManager.handleJoin(event.player)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (TournamentManager.isInLobby(event.player)) {
            TournamentManager.handleQuit(event.player)
        }
    }
}