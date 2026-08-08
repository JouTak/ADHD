package ru.joutak.adhd.listener.mode.knights

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.KnightsGame
import ru.joutak.adhd.tournament.TournamentManager

class FireListener : Listener {

    @EventHandler
    fun onFire(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return

        val game = TournamentManager.getGame(player)

        if (game != null && game.getGameState() == GameState.RUN && game is KnightsGame) {
            val weapon = player.inventory.itemInMainHand

            Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                val meta = weapon.itemMeta as Damageable

                meta.damage = 0

                weapon.itemMeta = meta

                player.inventory.setItem(8, ItemStack(Material.ARROW, 64))
            })
        }
    }
}