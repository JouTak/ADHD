package ru.joutak.adhd.tournament

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import java.util.UUID

class Tournament(val participants: MutableList<UUID>) {

    fun start(worldName: String) {
        val world = Bukkit.createWorld(WorldCreator(worldName))!!

        for (uuid in participants) {
            val player = Bukkit.getPlayer(uuid)

            if (player != null && player.isOnline) {
                player.teleport(world.spawnLocation)

                player.sendMessage(Component.text("Вы в игре!"))
            } else {
                participants.remove(uuid)
            }
        }
    }
}