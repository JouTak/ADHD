package ru.joutak.adhd.listener.mode.casino

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.CasinoGame
import ru.joutak.adhd.tournament.TournamentManager

class ChatListener : Listener {

    private val colors = setOf("красное", "черное", "зеленое")
    private val colorAliases = mapOf(
        "красный" to "красное",
        "красн" to "красное",
        "red" to "красное",
        "черный" to "черное",
        "чёрное" to "черное",
        "чёрн" to "черное",
        "black" to "черное",
        "зеленый" to "зеленое",
        "зелёное" to "зеленое",
        "зел" to "зеленое",
        "green" to "зеленое"
    )

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val player = event.player
        val game = TournamentManager.getGame(event.player)


        if (game != null && game.getGameState() == GameState.RUN && game is CasinoGame) {
            event.isCancelled = true

            val message = PlainTextComponentSerializer.plainText().serialize(event.message()).lowercase().trim()
            ADHDPlugin.instance.logger.info("Parsed message: $message")

            val parts = message.split(" ")
            if (parts.size < 2) {
                Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                    event.player.sendMessage(Component.text("Введите цвет и сумму! Пример: §eчерное 100").color(
                        NamedTextColor.RED))
                })
                return
            }

            var color = parts[0]
            if (color in colorAliases) {
                color = colorAliases[color]!!
            }

            if (color !in colors) {
                Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                    event.player.sendMessage(Component.text("Недопустимый цвет. Доступны: красное, черное, зеленое").color(
                        NamedTextColor.RED))
                })
                return
            }

            val amount = parts[1].toIntOrNull()
            if (amount == null || amount < 0) {
                Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                    event.player.sendMessage(Component.text("Введите корректную сумму ставки").color(
                        NamedTextColor.RED))
                })
                return
            }

            val balance = game.getPlayerBalance(event.player.uniqueId)
            if (amount > balance) {
                Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                    event.player.sendMessage(Component.text("У тебя маловато деньжат для такой ставки. Твой баланс $balance").color(
                        NamedTextColor.RED))
                })
                return
            }

            val success = game.placeBet(event.player,color, amount)
        }
    }
}