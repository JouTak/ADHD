package ru.joutak.adhd.listener.mode.tnt_bows

import org.bukkit.NamespacedKey
import org.bukkit.entity.Arrow
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.SnipersGame
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.minigames.MiniGamesAPI.plugin


class FireListener : Listener {
    private val tntTargetKey = NamespacedKey("adhd", "tnt_bows_target")

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        val arrow = event.entity as? Arrow ?: return

        val shooter = arrow.shooter as? Player ?: return

        val game = TournamentManager.getGame(shooter)
        //if (game == null || game.getGameState() != GameState.RUN || !(game is SnipersGame)) { return }

        val goal = event.hitEntity as? FallingBlock
        val check_player = event.hitEntity as? Player


        if (check_player != null) { // если игрок стреляет в другого игрока - отменяем
            event.isCancelled = true
            return
        }

        if (goal != null) {
            val location_goal = goal.location

            val tnt = location_goal.world.spawn(location_goal, org.bukkit.entity.TNTPrimed::class.java) { tnt ->
                tnt.fuseTicks = 400

                tnt.source = shooter
            }

            arrow.remove()
            goal.vehicle?.remove()
            goal.remove()


            object : BukkitRunnable() {
                override fun run() {
                    if (!tnt.isValid()) {
                        cancel()
                        return
                    }
                    val location = tnt.location

                    if (tnt.isOnGround || location.block.isSolid){
                        tnt.fuseTicks = 0
                        cancel()
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L)
        }
    }
}