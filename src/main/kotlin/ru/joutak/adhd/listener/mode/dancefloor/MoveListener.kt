package ru.joutak.adhd.listener.mode.dancefloor

import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.DanceFloorGame
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.adhd.world.SpawnPoint

class MoveListener: Listener {

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent){
        val game = TournamentManager.getGame(event.player)
        if (game != null && game.getGameState() == GameState.RUN && game is DanceFloorGame){
            if (event.player.y == game.ly + 1.0) game.checkBlock(event.player)
            if (event.player.y < game.critical_y) {
                val spawn = game.respawns[event.player.uniqueId]!!
                game.prevLoc[event.player.uniqueId] = Pair(spawn.x.toInt(), spawn.z.toInt())
                event.player.teleport(Location(spawn.world, spawn.x, spawn.y, spawn.z))
            }
        }
    }
}