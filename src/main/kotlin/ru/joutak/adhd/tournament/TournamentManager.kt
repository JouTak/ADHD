package ru.joutak.adhd.tournament

import org.bukkit.GameMode
import org.bukkit.entity.Player
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.world.WorldManager
import ru.joutak.minigames.domain.GameInstance
import ru.joutak.minigames.domain.GameInstanceConfig
import ru.joutak.minigames.domain.MatchmakingMode
import ru.joutak.minigames.lobby.LobbyItemsManager
import ru.joutak.minigames.managers.MatchmakingManager
import ru.joutak.minigames.ui.LobbyScoreboardManager
import kotlin.math.ceil
import kotlin.random.Random

object TournamentManager {

    fun handleJoin(player: Player) {
        sendToLobby(player)
    }

    fun handleQuit(player: Player) {
        sendToLobby(player)
    }

    fun load() {
        MatchmakingManager.loadInstances(listOf(GameInstanceConfig("default", ADHDConfig.maxPlayers, 1, matchmakingMode = MatchmakingMode.SOLO)))
    }

    fun createTournament(instance: GameInstance) {
        var toRemove = instance.teams.toMutableList().flatten()

        if (!WorldManager.isAvailable()) {
            ADHDPlugin.instance.logger.severe("Template world is not available. Game won't start...")

            for (player in toRemove) {
                ensure(player)
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
            ensure(player)
        }

        val tournament = Tournament(participants.toMutableList(), createPool())

        WorldManager.generate(tournament)
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

        player.teleport(spawn)
    }

    fun shutdown() {
        WorldManager.shutdown()
    }

    fun ensure(player: Player) {
        MatchmakingManager.removePlayer(player)
        MatchmakingManager.addPlayer(player)
        LobbyItemsManager.ensure(player)
        LobbyScoreboardManager.ensure(player)
    }

    fun isInLobby(player: Player): Boolean {
        return !MatchmakingManager.isPlayerInStartedGame(player.uniqueId)
    }
}