package ru.joutak.adhd.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID

class FreezeListener : Listener {
    companion object {
        val freeze = mutableMapOf<UUID, Boolean>()
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if (freeze[event.player.uniqueId] ?: false) {
            event.isCancelled = true
        }
    }
}