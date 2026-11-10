package ru.joutak.adhd.listener.mode.tnt_bows

import org.bukkit.entity.Arrow
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.game.concrete.TntBowsGame
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.minigames.MiniGamesAPI.plugin


class FireListener : Listener {
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val arrow = event.entity as? Arrow ?: return

        val shooter = arrow.shooter as? Player ?: return

        val game = TournamentManager.getGame(shooter) as? TntBowsGame ?: return

        val meta = game.gameMeta ?: return

        val goal = event.hitEntity as? FallingBlock
        if (goal == null) {
            event.isCancelled = true
            return
        }

        val locationGoal = goal.location

        arrow.remove()
        goal.vehicle?.remove()
        goal.remove()


        val tnt = locationGoal.world.spawn(locationGoal, org.bukkit.entity.TNTPrimed::class.java) { tnt ->
            tnt.fuseTicks = 400
            tnt.source = shooter
        }

        val fallY = meta.fallY

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