package ru.joutak.adhd.listener.mode.math

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.MathGame
import ru.joutak.adhd.tournament.TournamentManager

class ChatListener : Listener {
    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val game = TournamentManager.getGame(player)

        if (game is MathGame && game.getGameState() == GameState.RUN) {
            val answer = PlainTextComponentSerializer.plainText().serialize(event.message()).toIntOrNull()
            if (answer == game.exp.result) {
                game.right += 1
                player.sendMessage(Component.text("Верно!").color(NamedTextColor.GREEN))
                if (game.right < game.meta.quota) game.nextExp()
            } else if (answer != null) {
                player.sendMessage(Component.text("Неверно! Попробуй еще раз.").color(NamedTextColor.RED))
            } else {
                player.sendMessage(Component.text("Проверь формат сообщения!").color(NamedTextColor.RED))
            }
            event.isCancelled = true
        }
    }
}