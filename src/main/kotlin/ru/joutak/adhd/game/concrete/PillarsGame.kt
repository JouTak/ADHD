package ru.joutak.adhd.game.concrete

import org.bukkit.inventory.ItemStack
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.Mode
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.PillarsModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID

class PillarsGame : Game() {
    lateinit var arena: Arena

    lateinit var worldName: String

    lateinit var members: Set<UUID>

    lateinit var meta: PillarsModeMeta

    var result = mutableMapOf<UUID, Double>()

    var lSpawn : SpawnPoint? = null

    var state = GameState.START

    private var interval = 2
    private var itemRunnable: BukkitRunnable? = null

    private var isFinished = false

    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        this.meta = modeMeta as? PillarsModeMeta ?: throw IllegalArgumentException("PillarsGame requires PillarsModeMeta.")

        val world = Bukkit.getWorld(worldName)!!

        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            teleportToSpawn(player)

            restoreStats(player)
        }

        state = GameState.RUN
        startItemDistribution()
    }


    fun teleportToSpawn(player: Player) {
        val world = Bukkit.getWorld(worldName)!!

        val spawns = arena.spawnPoints.toMutableSet()

        if (lSpawn != null) {
            spawns -= mutableSetOf(lSpawn!!)
        }

        val chosen: SpawnPoint = if (spawns.isEmpty()){
            lSpawn!!
        } else {
            spawns.random()
        }

        lSpawn = chosen

        player.teleport(Location(world, chosen.x, chosen.y, chosen.z, chosen.yaw, chosen.pitch))
    }

    fun restoreStats(player: Player) {
        player.gameMode = GameMode.SURVIVAL
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
    }

    fun calculateResult(player: Player) {
        members.filter {uUID -> uUID != player.uniqueId}.forEach { uUID -> result[uUID] = 1.0 }
    }

    override fun update() {}

    private fun getRandomItem(): ItemStack? {
        if (meta.items.isEmpty()) return null

        val randomMaterial = meta.items.random()

        return ItemStack(randomMaterial)
    }

    private fun giveRandomItem(){
        if (isFinished) return

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            if (player.isDead || !player.isOnline) continue

            getRandomItem()?.let { item ->
                player.inventory.addItem(item)
            }
        }
    }

    private fun startItemDistribution() {
        val interval = (interval * 20L).coerceAtLeast(20L)
        ADHDPlugin.instance.logger.info("Started items distribution with interval: $interval seconds")

        val runnable = object : BukkitRunnable() {
            override fun run() {
                if (isFinished) {
                    this.cancel()
                    itemRunnable = null
                    return
                }
                try {
                    giveRandomItem()
                } catch (e : Exception) {
                    ADHDPlugin.instance.logger.warning(e.message)
                }
            }
        }

        runnable.runTaskTimer(ADHDPlugin.instance, interval, interval)
        itemRunnable = runnable
    }

    override fun getGameState(): GameState { return state }

    override fun summarize(): Map<UUID, Double> {
        return result

    }

    override fun finish() {
        ADHDPlugin.instance.logger.info("PillarsGame finishing for members: $members")
        isFinished = true
        itemRunnable?.cancel()
        itemRunnable = null

        state = GameState.FINISH
    }
}