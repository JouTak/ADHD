package ru.joutak.adhd.listener

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.KnightsGame
import ru.joutak.adhd.game.concrete.PVPGame
import ru.joutak.adhd.game.concrete.ParkourGame
import ru.joutak.adhd.game.concrete.PillarsGame
import ru.joutak.adhd.game.concrete.SnipersGame
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID

class GameDeathRespawnListener : Listener {
    companion object {
        val respawns = mutableMapOf<UUID, SpawnPoint?>()
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val spawn = respawns[event.player.uniqueId] ?: return

        respawns.remove(event.player.uniqueId)

        val tournament = TournamentManager.playerTournaments[event.player.uniqueId] ?: return

        val location = Location(Bukkit.getWorld(tournament.worldName)!!, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch)

        event.respawnLocation = location
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val game = TournamentManager.getGame(event.player) ?: return

        if (game.getGameState() != GameState.RUN) return

        when (game) {
            is KnightsGame -> {
                game.calculateResult(event.player)

                respawns[event.player.uniqueId] = getDefaultSpawn(event.player)

                game.finish()
            }

            is PillarsGame -> {
                game.calculateResult(event.player)

                respawns[event.player.uniqueId] = getDefaultSpawn(event.player)

                game.finish()
            }

            is PVPGame -> {
                game.calculateResult(event.player)

                respawns[event.player.uniqueId] = getDefaultSpawn(event.player)

                game.finish()
            }

            is SnipersGame -> {
                game.calculateResult(event.player)

                respawns[event.player.uniqueId] = getDefaultSpawn(event.player)

                game.finish()
            }

            is ParkourGame -> {
                respawns[event.player.uniqueId] = game.spawns[event.player.uniqueId]?.get(game.spawns[event.player.uniqueId]?.lastIndex ?: 0)
            }
        }
    }

    fun getDefaultSpawn(player: Player): SpawnPoint? {
        val tournament = TournamentManager.playerTournaments[player.uniqueId] ?: return null

        val game = tournament.playerGames[player.uniqueId] ?: return null

        val info = tournament.gameInfos[tournament.idByGame[game]] ?: return null

        val spawn = info.arena.spawnPoints.random()

        return spawn
    }
}