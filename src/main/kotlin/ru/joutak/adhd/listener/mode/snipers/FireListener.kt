package ru.joutak.adhd.listener.mode.snipers

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ChargedProjectiles
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.inventory.meta.Damageable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.SnipersGame
import ru.joutak.adhd.tournament.TournamentManager
import java.util.UUID

class FireListener : Listener {
    companion object {
        val reload = mutableMapOf<UUID, Boolean>()
    }

    @EventHandler
    fun onFire(event: EntityShootBowEvent) {
        val player = event.entity as? Player ?: return

        val game = TournamentManager.getGame(player)

        if (game != null && game.getGameState() == GameState.RUN && game is SnipersGame) {
            val crossbow = event.bow!!

            val uuid = player.uniqueId

            reload[uuid] = true

            Bukkit.getScheduler().runTaskLater(ADHDPlugin.instance, Runnable {
                val firework = game.buildFirework()

                crossbow.setData(
                    DataComponentTypes.CHARGED_PROJECTILES,
                    ChargedProjectiles.chargedProjectiles()
                        .add(firework)
                        .build()
                )

                val meta = crossbow.itemMeta as Damageable

                meta.damage = 0

                crossbow.itemMeta = meta

                reload[uuid] = false
            }, 10L)
        }
    }
}