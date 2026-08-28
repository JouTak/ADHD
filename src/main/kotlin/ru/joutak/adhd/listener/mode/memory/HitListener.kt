package ru.joutak.adhd.listener.mode.memory

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.MemoryGame
import ru.joutak.adhd.tournament.TournamentManager

class HitListener : Listener {

    @EventHandler
    fun onHit(event: EntityDamageByEntityEvent) {
        if (event.entity is ArmorStand) {
            val player = event.damager as? Player ?: return

            val game = TournamentManager.getGame(player)

            if (game != null && game.getGameState() == GameState.RUN && game is MemoryGame) {
                if (game.acknowledged) {
                    if (event.entity == game.pool[0]) {
                        game.pool.removeFirst()

                        player.sendActionBar(Component.text("Верно ☺").color(NamedTextColor.GREEN))

                        if (game.pool.isEmpty()) {
                            game.result[player.uniqueId] = 1.0

                            game.finish()
                        }
                    } else {
                        game.finish()

                        player.sendActionBar(Component.text("Неверно ☹").color(NamedTextColor.RED))
                    }

                    Bukkit.getWorld(game.worldName)?.playSound(event.entity.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, game.soundStands[event.entity]!!)
                }

                event.isCancelled = true
            }
        }
    }
}