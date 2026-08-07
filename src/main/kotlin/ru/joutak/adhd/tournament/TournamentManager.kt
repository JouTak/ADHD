package ru.joutak.adhd.tournament

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.world.WorldManager
import ru.joutak.minigames.command.ready.ReadyCommand
import ru.joutak.minigames.domain.GameInstance
import ru.joutak.minigames.domain.GameInstanceConfig
import ru.joutak.minigames.domain.MatchmakingMode
import ru.joutak.minigames.managers.MatchmakingManager
import java.util.UUID
import kotlin.math.ceil

object TournamentManager {

    val playerTournaments = mutableMapOf<UUID, Tournament>()

    val activeTournaments = mutableSetOf<Tournament>()

    var shutdownFlag = false

    fun handleJoin(player: Player) {
        sendToLobby(player)
    }

    fun handleQuit(player: Player) {
        sendToLobby(player)

        val tournament = playerTournaments.remove(player.uniqueId) ?: return

        tournament.remove(player)
    }

    fun load() {
        MatchmakingManager.loadInstances(listOf(GameInstanceConfig("default", ADHDConfig.maxPlayers, 1, matchmakingMode = MatchmakingMode.SOLO)))

        WorldManager.clearOnStartUp()
    }

    fun createTournament(instance: GameInstance) {
        val everyone = instance.teams.flatten()

        if (!WorldManager.isAvailable()) {
            ADHDPlugin.instance.logger.severe("World template can't load. Game won't start...")

            for (player in everyone) {
                retry(player)
            }

            return
        } else if (ADHDConfig.modes.isEmpty()) {
            ADHDPlugin.instance.logger.severe("No modes were loaded. Game won't start...")

            for (player in everyone) {
                retry(player)
            }

            return
        }

        val toRemove = everyone.subList(everyone.size / 2 * 2, everyone.size)

        for (player in toRemove) {
            retry(player)
        }

        instance.teams.forEach { l -> l.clear() }

        val participants = instance.getActivePlayerIds().toMutableList()

        val pool = createPool()

        val tournament = Tournament(participants, pool)

        activeTournaments.add(tournament)

        participants.forEach { uUID -> playerTournaments[uUID] = tournament }

        participants.forEach { uUID -> Bukkit.getPlayer(uUID)?.sendMessage(Component.text("Скоро начнём...").color(NamedTextColor.GOLD)) }

        WorldManager.generate(tournament)
    }

    fun retry(player: Player) {
        MatchmakingManager.removePlayer(player)

        Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
            if (player.isOnline) {
                ReadyCommand.performReady(player)
            }
        })
    }

    fun createPool(): List<String> {
        val pool = mutableListOf<String>()

        val names = ADHDConfig.modes.keys.toList()

        for (i in 0..<ceil(2 * ADHDConfig.pointsGoal - 1).toInt()) {
            pool.add(names.random())
        }

        return pool
    }

    fun sendToLobby(player: Player) {
        val lobby = WorldManager.getLobbyWorld()

        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
        player.isGlowing = false

        player.inventory.clear()

        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }

        player.teleport(lobby.spawnLocation)
    }

    fun getGame(player: Player): Game? {
        val tournament = playerTournaments[player.uniqueId] ?: return null

        return tournament.getGame(player)
    }

    fun finish(tournament: Tournament) {
        activeTournaments.remove(tournament)

        for (uuid in tournament.participants) {
            playerTournaments.remove(uuid)

            val player = Bukkit.getPlayer(uuid) ?: continue

            sendToLobby(player)

            if (!shutdownFlag) {
                MatchmakingManager.removePlayer(player)
            }
        }

        WorldManager.clear(tournament)
    }

    fun shutdown() {
        shutdownFlag = true

        activeTournaments.forEach { tournament -> tournament.finish() }
    }

    fun isInLobby(player: Player) = !MatchmakingManager.isPlayerInStartedGame(player.uniqueId)
}