package ru.joutak.adhd.listener

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.adhd.tournament.TournamentStatus
import kotlin.math.floor

class BoundListener : Listener {

    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        if ((TournamentManager.playerTournaments[event.player.uniqueId]?.status) == TournamentStatus.RUN) {
            val xL = floor(event.from.x / 512.0) * 512
            val zL = floor(event.from.z / 512.0) * 512

            if ((event.to.x !in (xL + 8)..<(xL + 504)) || (event.to.z !in (zL + 8)..<(zL + 504))) {
                val player = event.player

                player.kill(DamageSource.builder(DamageType.OUTSIDE_BORDER).build())

                player.sendMessage(Component.text("Вы были убиты причине попытки вылета за пределы арены...").color(
                    NamedTextColor.RED))
            }
        }
    }
}