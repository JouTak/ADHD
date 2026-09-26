package ru.joutak.adhd.listener.mode.rps

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.RPSGame
import ru.joutak.adhd.tournament.TournamentManager

class GuiListener : Listener {

    val tokens = setOf("камень", "ножницы", "бумага")

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val game = TournamentManager.getGame(event.player) ?: return

        if (game.getGameState() == GameState.RUN && game is RPSGame) {
            val item = event.item ?: return

            if (!item.hasItemMeta()) return

            val meta = item.itemMeta

            if (!meta.persistentDataContainer.has(RPSGame.cKey)) return

            val message = meta.persistentDataContainer.get(RPSGame.cKey, PersistentDataType.STRING) ?: return

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