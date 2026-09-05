package ru.joutak.adhd.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.joutak.adhd.tournament.TournamentManager

class SpectatorListener : Listener {

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return

        val tournament = TournamentManager.playerTournaments[damager.uniqueId] ?: return

        if (damager.uniqueId in tournament.spectators) {
            event.isCancelled = true
        }
    }
}