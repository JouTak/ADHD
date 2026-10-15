package ru.joutak.adhd.listener.mode.tnt_bows

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.NamespacedKey
import org.bukkit.entity.Arrow
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.config.map.meta.concrete.TntBowsMapMeta
import ru.joutak.adhd.game.concrete.TntBowsGame
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.minigames.MiniGamesAPI.plugin


class FireListener : Listener {
    private val tntTargetKey = NamespacedKey("adhd", "tnt_bows_target")

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val arrow = event.entity as? Arrow ?: return

        val shooter = arrow.shooter as? Player ?: return

        val game = TournamentManager.getGame(shooter) as? TntBowsGame ?: return

        val meta = game.gameMeta ?: return


        val goal = event.hitEntity as? FallingBlock
        //getConsoleSender().sendMessage("GOAL = $goal")
        if (goal == null) {
            event.isCancelled = true
            return
        }

        val location_goal = goal.location

        arrow.remove()
        goal.vehicle?.remove()
        goal.remove()


        val tnt = location_goal.world.spawn(location_goal, org.bukkit.entity.TNTPrimed::class.java) { tnt ->
            tnt.fuseTicks = 400
            tnt.source = shooter
        }

        val fallY = meta.fallY
        val a1 = meta.movementSpeedTnt
        val a2 = meta.spawnSpeedTnt
        val a3 = meta.explosionRadius
        getConsoleSender().sendMessage("$fallY,$a1, $a2, $a3")

        object : BukkitRunnable() {
            override fun run() {
                if (!tnt.isValid) {
                    cancel()
                    return
                }
                val location = tnt.location

                if (tnt.isOnGround || location.block.isSolid || location.y <= fallY){
                    tnt.fuseTicks = 0
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }
}