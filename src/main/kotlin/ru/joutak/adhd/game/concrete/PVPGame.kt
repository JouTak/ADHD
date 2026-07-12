package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.Mode
import ru.joutak.adhd.world.Arena
import java.util.UUID

class PVPGame : Game() {

    lateinit var mode: Mode

    lateinit var assignedMembers: MutableMap<UUID, Arena>

    lateinit var worldName: String

    val arenaMembers = mutableMapOf<Arena, MutableList<UUID>>()

    override fun start(
        mode: Mode,
        assignedMembers: Map<UUID, Arena>,
        worldName: String
    ) {
        ADHDPlugin.instance.logger.info("Начат режим ПВП...")

        this.mode = mode
        this.assignedMembers = assignedMembers.toMutableMap()
        this.worldName = worldName

        for (uuid in assignedMembers.keys) {
            val arena = assignedMembers[uuid]!!

            arenaMembers.putIfAbsent(arena, mutableListOf())

            arenaMembers[arena]!!.add(uuid)
        }

        for (arena in arenaMembers.keys) {
            restoreArenaMembers(arena)
        }
    }

    fun teleportArenaMembersToSpawn(arena: Arena) {
        val world = Bukkit.getWorld(worldName)

        val members = arenaMembers[arena]!!

        val spawnPoints = arena.spawnPoints.toMutableList()

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid)!!

            if (!player.isDead) {
                val spawnPoint = spawnPoints.random()

                player.teleport(Location(world, spawnPoint.x, spawnPoint.y, spawnPoint.z, spawnPoint.yaw, spawnPoint.pitch))
            }
        }
    }

    fun restoreArenaMembersHealth(arena: Arena) {
        val members = arenaMembers[arena]!!

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid)!!

            if (!player.isDead) {
                player.health = 20.0
                player.saturation = 20.0f
                player.foodLevel = 20

                player.gameMode = GameMode.SURVIVAL
            }
        }
    }

    fun restoreArenaMembersLayout(arena: Arena) {
        val members = arenaMembers[arena]!!

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid)!!

            if (!player.isDead) {
                player.inventory.clear()

                player.inventory.setItem(0, ItemStack(Material.NETHERITE_SWORD, 1))
            }
        }
    }

    fun getArena(player: Player): Arena {
        return assignedMembers[player.uniqueId]!!
    }

    fun restoreArenaMembers(arena: Arena) {
        teleportArenaMembersToSpawn(arena)
        restoreArenaMembersHealth(arena)
        restoreArenaMembersLayout(arena)
    }

    override fun update() {

    }

    override fun remove(uuid: UUID) {
        val arena = assignedMembers.remove(uuid) ?: return

        arenaMembers[arena]!!.remove(uuid)
    }

    override fun finish(): Map<UUID, Double> {
        ADHDPlugin.instance.logger.info("Завершён режим ПВП...")

        return emptyMap()
    }
}