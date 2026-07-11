package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.Mode
import ru.joutak.adhd.world.Arena
import java.util.UUID

class PVPGame : Game() {

    lateinit var mode: Mode

    lateinit var assignedMembers: Map<UUID, Arena>

    lateinit var worldName: String

    val arenaMembers = mutableMapOf<Arena, MutableList<UUID>>()

    override fun start(
        mode: Mode,
        assignedMembers: Map<UUID, Arena>,
        worldName: String
    ) {
        this.mode = mode
        this.assignedMembers = assignedMembers
        this.worldName = worldName

        for (uuid in assignedMembers.keys) {
            val arena = assignedMembers[uuid]!!

            arenaMembers.putIfAbsent(arena, mutableListOf())

            arenaMembers[arena]!!.add(uuid)
        }

        for (arena in arenaMembers.keys) {
            teleportArenaMembersToSpawn(arena)
            restoreArenaMembersHealth(arena)
            restoreArenaMembersLayout(arena)
        }
    }

    fun teleportArenaMembersToSpawn(arena: Arena) {
        val world = Bukkit.getWorld(worldName)

        val members = arenaMembers[arena]!!

        val spawnPoints = arena.spawnPoints.toMutableList()

        for (uuid in members) {
            spawnPoints.shuffle()

            val player = Bukkit.getPlayer(uuid)

            if (player != null && player.isOnline) {
                val spawnPoint = spawnPoints[0]

                player.teleport(Location(world, spawnPoint.x, spawnPoint.y, spawnPoint.z, spawnPoint.yaw, spawnPoint.pitch))
            } else {
                members.remove(uuid)
            }
        }
    }

    fun restoreArenaMembersHealth(arena: Arena) {
        val members = arenaMembers[arena]!!

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid)

            if (player != null && player.isOnline) {
                player.health = 20.0
                player.saturation = 20.0f
                player.foodLevel = 20

                player.gameMode = GameMode.SURVIVAL
            } else {
                members.remove(uuid)
            }
        }
    }

    fun restoreArenaMembersLayout(arena: Arena) {
        val members = arenaMembers[arena]!!

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid)

            if (player != null && player.isOnline) {
                player.inventory.clear()

                player.inventory.setItem(0, ItemStack(Material.NETHERITE_SWORD, 1))
            } else {
                members.remove(uuid)
            }
        }
    }

    override fun update() {

    }

    override fun finish(): Map<UUID, Double> {
        return emptyMap()
    }
}