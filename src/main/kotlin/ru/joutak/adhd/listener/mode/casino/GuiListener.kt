package ru.joutak.adhd.listener.mode.casino

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import ru.joutak.adhd.game.concrete.casino.CasinoGame
import ru.joutak.adhd.game.concrete.casino.CasinoMenu
import ru.joutak.adhd.tournament.TournamentManager
import java.util.UUID
import ru.joutak.adhd.game.GameState

class GuiListener : Listener {

    private val pendingColors = mutableMapOf<UUID, String>()

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = event.view.title()

        val plain = PlainTextComponentSerializer
            .plainText().serialize(title)

        val game = TournamentManager.getGame(player) as? CasinoGame ?: return
        if (game.getGameState() != GameState.RUN) return

        if (event.clickedInventory != event.view.topInventory) {
            event.isCancelled = true
            return
        }

        event.isCancelled = true
        when {
            plain.contains(CasinoMenu.TITLE_COLOR_MENU) -> {
                val color = when (event.rawSlot) {
                    CasinoMenu.SLOT_RED -> "красное"
                    CasinoMenu.SLOT_GREEN -> "зеленое"
                    CasinoMenu.SLOT_BLACK -> "черное"
                    else -> return
                }
                val balance = game.getPlayerBalance(player.uniqueId)

                pendingColors[player.uniqueId] = color
                CasinoMenu.openAmountMenu(player, color, balance)
            }

            plain.contains(CasinoMenu.TITLE_AMOUNT) ->{
                if (event.rawSlot == 22) {
                    pendingColors.remove(player.uniqueId)
                    CasinoMenu.openColorMenu(player, game.getPlayerBalance(player.uniqueId), game.playerGoal)
                    return
                }

                val color = pendingColors.remove(player.uniqueId) ?: return
                val balance = game.getPlayerBalance(player.uniqueId)

                val amount = CasinoMenu.amountForSlot(event.rawSlot, balance) ?: return


                if (amount !in 1..balance) {
                    player.sendMessage(net.kyori.adventure.text.Component
                        .text("Недостаточно средств!").color(net.kyori.adventure.text.format.NamedTextColor.RED))
                    return
                }

                player.closeInventory()
                game.placeBet(player, color, amount)
            }
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val plain = PlainTextComponentSerializer.plainText().serialize(event.view.title())
        if (plain.contains(CasinoMenu.TITLE_AMOUNT)) {
            pendingColors.remove(event.player.uniqueId)
        }
    }
}