package ru.joutak.adhd.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import java.util.UUID

class KeepInventoryListener : Listener {
    companion object {
        val states = mutableMapOf<UUID, Boolean>()
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (states[event.whoClicked.uniqueId] ?: false) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (states[event.whoClicked.uniqueId] ?: false) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        if (states[event.player.uniqueId] ?: false) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPickUp(event: PlayerAttemptPickupItemEvent) {
        if (states[event.player.uniqueId] ?: false) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onSwap(event: PlayerSwapHandItemsEvent) {
        if (states[event.player.uniqueId] ?: false) {
            event.isCancelled = true
        }
    }
}