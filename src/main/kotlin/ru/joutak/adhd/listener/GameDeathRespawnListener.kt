package ru.joutak.adhd.listener

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.KnightsGame
import ru.joutak.adhd.game.concrete.PVPGame
import ru.joutak.adhd.game.concrete.PillarsGame
import ru.joutak.adhd.game.concrete.SnipersGame
import ru.joutak.adhd.tournament.TournamentManager

class GameDeathRespawnListener : Listener {

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val tournament = TournamentManager.playerTournaments[event.player.uniqueId] ?: return

        val game = tournament.playerGames[event.player.uniqueId] ?: return

        val info = tournament.gameInfos[tournament.idByGame[game]] ?: return

        val spawn = info.arena.spawnPoints.random()

        val loc = Location(Bukkit.getWorld(tournament.worldName)!!, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch)

        event.respawnLocation = loc
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val game = TournamentManager.getGame(event.player) ?: return

        if (game.getGameState() != GameState.RUN) return

        when (game) {
            is KnightsGame -> {
                game.calculateResult(event.player)

                game.finish()
            }

            is PillarsGame -> {
                game.calculateResult(event.player)

                game.finish()
            }

            is PVPGame -> {
                game.calculateResult(event.player)

                game.finish()
            }

            is SnipersGame -> {
                game.calculateResult(event.player)

                game.finish()
            }
        }
    }
}