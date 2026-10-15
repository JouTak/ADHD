package ru.joutak.adhd.listener.mode.rglight

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.RGLightGame
import ru.joutak.adhd.tournament.TournamentManager

class FinishLineListener : Listener {

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val game = TournamentManager.getGame(event.player) ?: return
        if (game.getGameState() != GameState.RUN || game !is RGLightGame) return

        if (game.hasFinished(event.player)) return

        if (game.isOnFinishLine(event.player)) {
            game.calculateResult(event.player)
            game.finish()
        }
    }
}