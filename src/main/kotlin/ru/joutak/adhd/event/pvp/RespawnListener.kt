package ru.joutak.adhd.event.pvp

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.concrete.PVPGame
import ru.joutak.adhd.tournament.TournamentManager

class RespawnListener : Listener {

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        val game = TournamentManager.getGame(event.player)

        if (game != null && game is PVPGame) {
            val arena = game.getArena(event.player)

            Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {game.restoreArenaMembers(arena)})

            game.calculatePoint(event.player)
        }
    }
}