package ru.joutak.adhd.tournament

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.entity.Player
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.world.WorldManager
import ru.joutak.minigames.domain.GameInstance
import ru.joutak.minigames.domain.GameInstanceConfig
import ru.joutak.minigames.domain.MatchmakingMode
import ru.joutak.minigames.lobby.LobbyItemsManager
import ru.joutak.minigames.managers.MatchmakingManager

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
                MatchmakingManager.removePlayer(player)
                MatchmakingManager.addPlayer(player)
                LobbyItemsManager.ensure(player)
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
            MatchmakingManager.removePlayer(player)
            MatchmakingManager.addPlayer(player)
            LobbyItemsManager.ensure(player)
        }

        val tournament = Tournament(participants.toMutableList())

        for (uuid in participants) {
            val player = Bukkit.getPlayer(uuid)!!

            player.sendMessage(Component.text("Скоро начнём...").color(NamedTextColor.GOLD))
        }

        WorldManager.generate(tournament)
    }

    fun sendToLobby(player: Player) {
        val lobby = getLobbyWorld()

        val spawn = lobby.spawnLocation

        player.gameMode = GameMode.ADVENTURE

        player.teleport(spawn)
    }

    fun getLobbyWorld(): World {
        var lobby = Bukkit.getWorld("lobby")

        if (lobby == null) {
            lobby = Bukkit.createWorld(WorldCreator("lobby").type(WorldType.NORMAL))
        }

        if (lobby == null) {
            lobby = Bukkit.getWorld("overworld")

            ADHDPlugin.instance.logger.warning("Couldn't load lobby. Using default world as fallback")
        }

        lobby!!.setGameRule(GameRules.SPAWN_MOBS, false)
        lobby.setGameRule(GameRules.SPAWN_MONSTERS, false)
        lobby.setGameRule(GameRules.FALL_DAMAGE, false)
        lobby.setGameRule(GameRules.FIRE_DAMAGE, false)
        lobby.setGameRule(GameRules.FREEZE_DAMAGE, false)

        return lobby
    }

    fun shutdown() {
        val lobby = Bukkit.getWorld("lobby")

        if (lobby != null) {
            Bukkit.unloadWorld(lobby, false)
        }
    }

    fun isInLobby(player: Player): Boolean {
        return !MatchmakingManager.isPlayerInStartedGame(player.uniqueId)
    }
}