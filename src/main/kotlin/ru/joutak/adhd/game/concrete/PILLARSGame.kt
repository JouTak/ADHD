package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.Mode
import ru.joutak.adhd.world.Arena
import java.util.UUID

class PILLARSGame : Game() {
    lateinit var mode: Mode

    lateinit var assignedMembers: MutableMap<UUID, Arena>

    lateinit var worldName: String

    val arenaMembers = mutableMapOf<Arena, MutableList<UUID>>()
    private var isRunning = false
    val results = mutableMapOf<UUID, Double>()

    private var itemRunnable: BukkitRunnable? = null
    private var minInterval = 2

    override fun start(mode: Mode, assignedMembers: Map<UUID, Arena>, worldName: String) {

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

            val interval = minInterval
        }
        isRunning = true
        startItemDistribution()
    }

    private fun getRandomItemForArena(arena: Arena): ItemStack? {
        val items = (arena.meta["items"] as? List<*>)?.filterIsInstance<String>() ?: return null
        if (items.isEmpty()) return null

        val randomMaterial = items.random()
        val material = Material.getMaterial(randomMaterial) ?: return null

        return ItemStack(material)
    }

    private fun giveRandomItem(){
        if (!isRunning) {
            itemRunnable?.cancel()
            itemRunnable = null
            return
        }

        for ((arena, members) in arenaMembers) {
            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                if (player.isDead || !player.isOnline) continue

                getRandomItemForArena(arena)?.let { item ->
                    player.inventory.addItem(item)
                }
            }
        }
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

    fun getArena(player: Player): Arena {
        return assignedMembers[player.uniqueId]!!
    }

    fun teleportArenaMembersToSpawn(arena: Arena) {
        val world = Bukkit.getWorld(worldName)

        val members = arenaMembers[arena]!!

        val spawnPoints = arena.spawnPoints.shuffled().toMutableList()

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid)!!

            if (!player.isDead) {
                val spawnPoint = if (spawnPoints.isNotEmpty()) {
                    spawnPoints.removeAt(0)
                } else {
                    arena.spawnPoints.random()
                }

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
            }
        }
    }

    fun restoreArenaMembers(arena: Arena) {
        teleportArenaMembersToSpawn(arena)
        restoreArenaMembersHealth(arena)
        restoreArenaMembersLayout(arena)
    }

    private fun startItemDistribution(){
        val interval = (minInterval * 20L).coerceAtLeast(20L)
        ADHDPlugin.instance.logger.info("Запущена выдача предметов с интервалом ${interval/20} секунд")

        val runnable = object : BukkitRunnable() {
            override fun run() {
                try {

                    giveRandomItem()
                } catch (e: Exception) {
                    ADHDPlugin.instance.logger.warning("Ошибка в таске выдачи предметов: ${e.message}")
                }
            }
        }

        runnable.runTaskTimer(ADHDPlugin.instance, interval, interval)
        itemRunnable = runnable
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

    override fun finish(): Map<UUID, Double> {
        isRunning = false
        itemRunnable?.cancel()
        itemRunnable = null

        for ((_, members) in arenaMembers) {
            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid)
                if (player != null && player.isOnline) {
                    player.inventory.clear()
                    player.inventory.armorContents = arrayOfNulls<ItemStack>(4)
                }
            }
        }

        ADHDPlugin.instance.logger.info("Завершён режим Столбов...")

        val world = Bukkit.getWorld(worldName)

        world!!.setGameRule(GameRules.IMMEDIATE_RESPAWN, false)

        return calculateResults()
    }
}