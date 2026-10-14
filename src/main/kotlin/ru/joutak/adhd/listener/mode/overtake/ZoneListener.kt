package ru.joutak.adhd.listener.mode.overtake

import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.config.map.meta.concrete.OvertakeMapMeta
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.OvertakeGame
import ru.joutak.adhd.tournament.TournamentManager
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.properties.Delegates

class ZoneListener : Listener {

    var pointer = 1.0
    val overtakeMapMeta = ADHDConfig.configMaps[1]!!.metas["overtake"] as OvertakeMapMeta
    val center = overtakeMapMeta.center
    val radius = overtakeMapMeta.radius
    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val game = TournamentManager.getGame(player)

        if (game is OvertakeGame && game.getGameState() == GameState.RUN) {
            pointer = floor(game.arena.spawnPoints[0].x / 512)

            if (isInZone(event.to) && !isInZone(event.from)) {
                player.sendMessage("Вы вошли в зону захвата!")
                game.currentPlayers.add(player.uniqueId)
            } else if (!isInZone(event.to) && isInZone(event.from)) {
                player.sendMessage("Вы вышли из зоны захвата!")
                game.currentPlayers.remove(player.uniqueId)
            }
        }
    }

    fun isInZone(location: Location): Boolean {
        if (location.x in (center.x + 512 * pointer - radius)..(center.x + 512 * pointer + radius) &&
            location.z in (center.z - radius)..(center.z + radius)) {
            return true
        } else {
            return false
        }
    }
}