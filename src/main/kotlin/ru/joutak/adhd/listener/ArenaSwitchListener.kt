package ru.joutak.adhd.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
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

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val item = event.currentItem ?: return

        if (!item.hasItemMeta()) return

        if (event.view.title() == Component.text("Арены")) {
            val tournament = TournamentManager.playerTournaments[event.whoClicked.uniqueId] ?: return

            val meta = item.itemMeta

            if (!meta.persistentDataContainer.has(NamespacedKey(ADHDPlugin.instance, "arenaId"))) return

            val id = meta.persistentDataContainer.get(NamespacedKey(ADHDPlugin.instance, "arenaId"), PersistentDataType.INTEGER)

            val info = tournament.gameInfos[id] ?: tournament.gameInfos.values.random()

            val spawn = info.arena.spawnPoints.random()

            event.whoClicked.teleport(Location(Bukkit.getWorld(tournament.worldName), spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))

            event.whoClicked.closeInventory()
        }
    }

    companion object {
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

                meta.persistentDataContainer.set(NamespacedKey(ADHDPlugin.instance, "arenaId"), PersistentDataType.INTEGER, id)

                item.itemMeta = meta

                inventory.setItem(pointer++, item)
            }

            player.openInventory(inventory)
        }
    }
}