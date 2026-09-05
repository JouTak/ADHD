package ru.joutak.adhd.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.tournament.Tournament
import ru.joutak.adhd.tournament.TournamentManager
import kotlin.math.ceil

class ArenaSwitchListener : Listener {

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return

        if (!item.hasItemMeta()) return

        val meta = item.itemMeta

        if (!meta.persistentDataContainer.has(NamespacedKey(ADHDPlugin.instance, "switchItemArena"))) return

        val tournament = TournamentManager.playerTournaments[event.player.uniqueId] ?: return

        createInventoryAndOpen(tournament, event.player)
    }

    fun createInventoryAndOpen(tournament: Tournament, player: Player) {
        val games = tournament.games.filter { it.getGameState() == GameState.RUN }

        val ids = tournament.idByGame.filter { games.contains(it.key) }.values.take(54).sorted()

        if (ids.isEmpty()) return

        val inventory = Bukkit.createInventory(null, (ceil(ids.size / 9.0) * 9).toInt(), Component.text("Арены"))

        var pointer = 0

        for (id in ids) {
            val info = tournament.gameInfos[id]!!

            val item = ItemStack(Material.PLAYER_HEAD, 1)

            val meta = item.itemMeta

            meta.displayName(Component.text("Арена ").decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE).append(Component.text("#$id").color(NamedTextColor.GOLD)))

            val players = mutableListOf<Component>()

            for (uuid in info.members) {
                val name = Component.text("- ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).append((Bukkit.getPlayer(uuid)?.displayName() ?: continue).color(NamedTextColor.GRAY))

                players.add(name)
            }

            meta.lore(players)

            item.itemMeta = meta

            inventory.setItem(pointer++, item)
        }

        player.openInventory(inventory)
    }
}