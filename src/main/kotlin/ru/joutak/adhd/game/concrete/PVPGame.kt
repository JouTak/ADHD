package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.Mode
import ru.joutak.adhd.world.Arena
import java.util.*

class PVPGame : Game() {

    lateinit var mode: Mode

    lateinit var assignedMembers: MutableMap<UUID, Arena>

    lateinit var worldName: String

    val arenaMembers = mutableMapOf<Arena, MutableList<UUID>>()

    val results = mutableMapOf<UUID, Double>()

    override fun start(
        mode: Mode,
        assignedMembers: Map<UUID, Arena>,
        worldName: String
    ) {
        ADHDPlugin.instance.logger.info("Начат режим ПВП...")

        this.mode = mode
        this.assignedMembers = assignedMembers.toMutableMap()
        this.worldName = worldName

        val world = Bukkit.getWorld(worldName)

        world!!.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)

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

    fun calculatePoint(player: Player) {
        val arena = getArena(player)

        for (uuid in arenaMembers[arena].orEmpty()) {
            if (uuid == player.uniqueId) continue

            results[uuid] = results.getOrDefault(uuid, 0.0) + 1.0
        }
    }

    override fun finish(): Map<UUID, Double> {
        ADHDPlugin.instance.logger.info("Завершён режим ПВП...")

        val world = Bukkit.getWorld(worldName)

        world!!.setGameRule(GameRules.IMMEDIATE_RESPAWN, false)

        return calculateResults()
    }

    fun calculateResults(): MutableMap<UUID, Double> {
        val finalResults = mutableMapOf<UUID, Double>()

        for ((_, members) in arenaMembers) {
            val maxScore = members.maxOfOrNull { results.getOrDefault(it, 0.0) } ?: continue

            for (uuid in members) {
                if (results.getOrDefault(uuid, 0.0) == maxScore) {
                    finalResults[uuid] = 1.0
                } else {
                    finalResults[uuid] = 0.0
                }
            }
        }

        return finalResults
    }
}