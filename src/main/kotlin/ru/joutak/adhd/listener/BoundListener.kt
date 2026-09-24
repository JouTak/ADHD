package ru.joutak.adhd.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.tournament.TournamentManager
import kotlin.math.floor

class BoundListener : Listener {

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (!TournamentManager.isInLobby(event.player)) {
            val xL = floor(event.from.x / 512.0) * 512
            val zL = floor(event.from.z / 512.0) * 512

            if ((event.to.x !in xL..<(xL + 512)) || (event.to.z !in zL..<(zL + 512))) {
                event.isCancelled = true
            }
        }
    }
}