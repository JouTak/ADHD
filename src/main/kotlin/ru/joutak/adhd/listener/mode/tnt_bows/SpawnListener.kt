package ru.joutak.adhd.listener.mode.tnt_bows

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.FallingBlock
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.tournament.TournamentManager
import ru.joutak.minigames.MiniGamesAPI.plugin

class SpawnListener : Listener {

    fun startTntSpawner(loc1: Location, loc2: Location) {
        // Вычисляем скорость: сколько блоков в тик должен лететь ТНТ.
        // Например, 0.2 блока за тик = 4 блока в секунду. Можете настроить под себя.
        val speedX = 0.1
        val velocityVector = Vector(speedX, 0.0, 0.0)

        //val game = TournamentManager.getGame(shooter)

        object : BukkitRunnable() {
            override fun run() {

                spawnMovingTnt(loc1, loc2, velocityVector)
            }
        }.runTaskTimer(plugin, 0L, 30L) // 30 тиков = 1.5 секунды
    }

    private fun spawnMovingTnt(startLoc: Location, endLoc: Location, velocity: Vector) {
        val world = startLoc.world ?: return

        // 1. Спавним невидимую стойку для брони
        val armorStand = world.spawn(startLoc, ArmorStand::class.java) { stand ->
            stand.isVisible = false
            stand.setGravity(false)
            stand.setBasePlate(false)
            stand.isSmall = true
        }

        // 2. Спавним блок ТНТ на неё пассажиром
        val tntBlock = world.spawn(startLoc, FallingBlock::class.java) { fb ->
            fb.blockData = Material.TNT.createBlockData()
            fb.setGravity(false)
            fb.setDropItem(false)
            fb.setCancelDrop(true)
        }
        armorStand.addPassenger(tntBlock)

        armorStand.velocity = velocity

        // 4. Запускаем ежетиксовый трекер движения и удаления
        object : BukkitRunnable() {
            override fun run() {
                // Если игроки уже сбили ТНТ (из вашего FireListener), удаляем задачу
                if (!armorStand.isValid || !tntBlock.isValid) {
                    cancel()
                    return
                }

                tntBlock.ticksLived = 1

                armorStand.velocity = velocity

                if (armorStand.location.x >= endLoc.x) {
                    tntBlock.remove()
                    armorStand.remove()
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }



}