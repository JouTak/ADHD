package ru.joutak.adhd.listener

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import ru.joutak.minigames.MiniGamesAPI.plugin


class TntBowsTestListener : Listener {
    private val tntTargetKey = NamespacedKey("adhd", "tnt_bows_target")

    @EventHandler
    fun onTntRightClick(event: PlayerInteractEvent) {
        val player = event.player

        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }
        val item = event.item ?: return

        if (item.type != Material.APPLE) return

        org.bukkit.Bukkit.broadcast(Component.text("Попытка ТНТ"))
        getConsoleSender().sendMessage("Попытка TNT")

        event.isCancelled = true
        val location = player.location;

        val armorStand = location.world.spawn(location, ArmorStand::class.java) { stand ->
            stand.isVisible = false
            stand.setGravity(false)
            stand.setBasePlate(false)
            stand.setArms(false)
            stand.isSmall = true

            stand.velocity = Vector(1, 0, 0)

            stand.persistentDataContainer.set(tntTargetKey, PersistentDataType.BOOLEAN, true)
        }

        val tntFallingBlock = location.world.spawn(location, FallingBlock::class.java) { fb ->
            fb.blockData = Material.TNT.createBlockData()
            fb.setGravity(false)
            fb.setDropItem(false)
            fb.setCancelDrop(true)
        }

        armorStand.addPassenger(tntFallingBlock)

        object : BukkitRunnable() {
            override fun run() {
                if (!tntFallingBlock.isValid || !armorStand.isValid) {
                    cancel()
                    return
                }

                tntFallingBlock.ticksLived = 1
                val nextLocation = armorStand.location.add(Vector(0.075,0.0,0.0))
                armorStand.teleport(nextLocation)

            }
        }.runTaskTimer(plugin, 0L, 1L)
    }
}