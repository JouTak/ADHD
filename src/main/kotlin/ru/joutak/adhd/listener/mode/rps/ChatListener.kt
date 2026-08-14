package ru.joutak.adhd.listener.mode.rps

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.RPSGame
import ru.joutak.adhd.tournament.TournamentManager

class ChatListener : Listener {

    val tokens = setOf("камень", "ножницы", "бумага")

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val game = TournamentManager.getGame(event.player)

        if (game != null && game.getGameState() == GameState.RUN && game is RPSGame) {
            event.isCancelled = true

            val message = PlainTextComponentSerializer.plainText().serialize(event.message()).lowercase()

            if (game.choice.isEmpty()) {
                if (tokens.contains(message)) {
                    game.choice = message

                    Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                        event.player.sendMessage(Component.text("Вы сделали свой выбор!").color(NamedTextColor.GREEN))
                    })
                } else {
                    Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                        event.player.sendMessage(Component.text("Нет такого варианта...").color(NamedTextColor.YELLOW))
                    })
                }
            } else {
                Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                    event.player.sendMessage(Component.text("Ваш выбор уже сделан...").color(NamedTextColor.RED))
                })
            }
        }
    }
}