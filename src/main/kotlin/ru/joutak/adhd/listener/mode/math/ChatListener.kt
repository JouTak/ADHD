package ru.joutak.adhd.listener.mode.math

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Sound
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
                player.sendActionBar(Component.text("Верно!").color(NamedTextColor.GREEN))
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1.0F, 1.0F)
                if (game.right < game.meta.quota) game.nextExp()
            } else if (answer != null) {
                player.sendActionBar(Component.text("Неверно! Попробуй еще раз.").color(NamedTextColor.RED))
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F)
            } else {
                player.sendActionBar(Component.text("Проверь формат сообщения!").color(NamedTextColor.RED))
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F)
            }
            event.isCancelled = true
        }
    }
}