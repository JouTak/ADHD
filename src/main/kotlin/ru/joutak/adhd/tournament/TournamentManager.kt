package ru.joutak.adhd.tournament

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.world.WorldManager
import ru.joutak.minigames.domain.GameInstance
import ru.joutak.minigames.domain.GameInstanceConfig
import ru.joutak.minigames.domain.MatchmakingMode
import ru.joutak.minigames.lobby.LobbyItemsManager
import ru.joutak.minigames.managers.MatchmakingManager
import ru.joutak.minigames.ui.LobbyScoreboardManager
import java.util.UUID
import kotlin.math.ceil
import kotlin.random.Random

object TournamentManager {

    val playerTournaments = mutableMapOf<UUID, Tournament>()

    val activeTournaments = mutableSetOf<Tournament>()

    var shutdownFlag = false

    fun handleJoin(player: Player) {
        sendToLobby(player)
    }

    fun handleQuit(player: Player) {
        sendToLobby(player)

        val playerTournament = playerTournaments.remove(player.uniqueId)

        val delta = activeTournaments.toSet() - playerTournaments.values.toSet()

        for (tournament in delta) {
            tournament.finish()
        }

        if (playerTournament == null) return

        playerTournament.remove(player)
    }

    fun load() {
        MatchmakingManager.loadInstances(listOf(GameInstanceConfig("default", ADHDConfig.maxPlayers, 1, matchmakingMode = MatchmakingMode.SOLO)))
    }

    fun createTournament(instance: GameInstance) {
        var toRemove = instance.teams.toMutableList().flatten()

        if (!WorldManager.isAvailable()) {
            ADHDPlugin.instance.logger.severe("Template world is not available. Game won't start...")

            for (player in toRemove) {
                ensureRetry(player)
            }

            return
        }

        //TODO: Implement such player count system in minigames api instead

        toRemove = toRemove.subList(toRemove.size / 2 * 2, toRemove.size)

        for (player in toRemove) {
            instance.removePlayer(player)
        }

        val participants = instance.startMatchAndSnapshotPlayers()

        instance.teams.clear()

        for (player in toRemove) {
            ensureRetry(player)
        }

        val tournament = Tournament(participants.toMutableList(), createPool())

        activeTournaments.add(tournament)

        for (uuid in participants) {
            playerTournaments[uuid] = tournament
        }

        WorldManager.generate(tournament)
    }

    fun getGame(player: Player): Game? {
        val tournament = playerTournaments[player.uniqueId] ?: return null

        if (tournament.status != TournamentStatus.RUNNING) return null

        return tournament.currentGame
    }

    fun createPool(): List<String> {
        val pool = mutableListOf<String>()

        for (i in 0..<ceil(2 * ADHDConfig.pointsGoal - 1).toInt()) {
            pool.add(ADHDConfig.modes.keys.toList()[Random.nextInt(ADHDConfig.modes.size)])
        }

        return pool
    }

    fun sendToLobby(player: Player) {
        val lobby = WorldManager.getLobbyWorld()

        val spawn = lobby.spawnLocation

        player.gameMode = GameMode.ADVENTURE

        player.inventory.clear()

        player.teleport(spawn)
    }

    fun shutdown() {
        shutdownFlag = true

        for (tournament in activeTournaments) {
            tournament.finish()
        }

        WorldManager.shutdown()
    }

    fun ensureRetry(player: Player) {
        MatchmakingManager.removePlayer(player)
        MatchmakingManager.addPlayer(player)
        LobbyItemsManager.ensure(player)
        LobbyScoreboardManager.ensure(player)
    }

    fun finish(tournament: Tournament) {
        activeTournaments.remove(tournament)

        for (uuid in tournament.participants) {
            if (playerTournaments.contains(uuid)) {
                val player = Bukkit.getPlayer(uuid)

                if (player != null && player.isOnline) {
                    sendToLobby(player)

                    if (!shutdownFlag) {
                        MatchmakingManager.removePlayer(player)
                    }
                }
            }

            playerTournaments.remove(uuid)
        }

        if (tournament.generated) WorldManager.clear(tournament)
    }

    fun isInLobby(player: Player): Boolean {
        return !MatchmakingManager.isPlayerInStartedGame(player.uniqueId)
    }
}