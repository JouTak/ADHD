package ru.joutak.adhd.listener.mode.rglight

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerTeleportEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.PillarsGame
import ru.joutak.adhd.game.concrete.RGLightGame
import ru.joutak.adhd.tournament.TournamentManager

class PlayerMoveListener : Listener {

    @EventHandler
    fun onPlayerMoveEvent(event: PlayerMoveEvent) {
        if (event is PlayerTeleportEvent) return

        val from = event.from
        val to = event.to

        val dx = to.x - from.x
        val dz = to.z - from.z
        if (dx * dx + dz * dz < 0.0016) return

        val game = TournamentManager.getGame(event.player) as? RGLightGame ?: return

        if (game.getGameState() == GameState.RUN &&
            game.getLightColor() == RGLightGame.LightColor.RED &&
            game.isRedGraceOver()
        ) {
            game.punishPlayer(event.player)
        }
    }
}