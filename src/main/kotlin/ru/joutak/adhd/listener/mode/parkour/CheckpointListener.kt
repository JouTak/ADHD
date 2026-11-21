package ru.joutak.adhd.listener.mode.parkour

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.ParkourGame
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.adhd.world.SpawnPoint
import kotlin.math.ceil
import kotlin.math.floor

class CheckpointListener : Listener {

    companion object {
        private const val CHECKPOINT_RADIUS = 3.0
    }

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val game = TournamentManager.getGame(player)

        if (game is ParkourGame && game.state == GameState.RUN) {
            findCheckpoint(player)?.let { checkpoint ->
                game.saveCheckpoint(player, checkpoint)
            }
        }
    }

    private fun findCheckpoint(player: Player): SpawnPoint? {
        val location = player.location
        val radius = ceil(CHECKPOINT_RADIUS).toInt()
        var closest: SpawnPoint? = null
        var closestDistanceSquared = CHECKPOINT_RADIUS * CHECKPOINT_RADIUS

        for (x in floor(location.x).toInt() - radius..floor(location.x).toInt() + radius) {
            for (y in floor(location.y).toInt() - radius..floor(location.y).toInt() + radius) {
                for (z in floor(location.z).toInt() - radius..floor(location.z).toInt() + radius) {
                    val block = location.world.getBlockAt(x, y, z)
                    if (block.type != Material.GOLD_BLOCK) continue

                    val blockCenter = block.location.add(0.5, 0.5, 0.5)
                    val distanceSquared = location.distanceSquared(blockCenter)
                    if (distanceSquared > closestDistanceSquared) continue

                    closestDistanceSquared = distanceSquared
                    closest = SpawnPoint(
                        block.x + 0.5,
                        block.y + 1.0,
                        block.z + 0.5,
                        player.location.yaw,
                        player.location.pitch
                    )
                }
            }
        }

        return closest
    }
}
