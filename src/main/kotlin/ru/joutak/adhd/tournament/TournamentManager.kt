package ru.joutak.adhd.tournament

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import org.bukkit.entity.Player
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.minigames.domain.GameInstanceConfig
import ru.joutak.minigames.managers.MatchmakingManager

object TournamentManager {

    fun handleJoin(player: Player) {
        sendToLobby(player)
    }

    fun handleQuit(player: Player) {
        sendToLobby(player)
    }

    fun load() {
        MatchmakingManager.loadInstances(listOf(GameInstanceConfig("default", 1, 8)))
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

        return lobby
    }

    fun shutdown() {
        val lobby = Bukkit.getWorld("lobby")

        if (lobby != null) {
            Bukkit.unloadWorld(lobby, false)
        }
    }

    fun isInLobby(player: Player): Boolean {
        return true
    }
}