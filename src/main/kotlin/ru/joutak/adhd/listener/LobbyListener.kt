package ru.joutak.adhd.listener

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.tournament.TournamentManager

class LobbyListener : Listener {

    @EventHandler
    fun onDeath(event: PlayerRespawnEvent) {
        if (TournamentManager.isInLobby(event.player)) {
            Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {TournamentManager.sendToLobby(event.player)})
        }
    }
}