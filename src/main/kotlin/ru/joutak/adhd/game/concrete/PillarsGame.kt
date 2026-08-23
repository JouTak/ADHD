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
import ru.joutak.adhd.config.map.meta.concrete.PillarsMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.Mode
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.PillarsModeMeta
import ru.joutak.adhd.listener.FreezeListener
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID

class PillarsGame : Game() {
    lateinit var arena: Arena

    lateinit var worldName: String

    lateinit var members: Set<UUID>

    lateinit var meta: PillarsModeMeta

    val interval: Double
        get() = meta.interval

    var result = mutableMapOf<UUID, Double>()

    var lSpawn : SpawnPoint? = null

    var state = GameState.START

    private var tickCounter = 0
    private var intervalTicks = 0

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

            FreezeListener.freeze[uuid] = true

            Bukkit.getScheduler().runTaskLater(ADHDPlugin.instance, Runnable { FreezeListener.freeze[uuid] = false }, 20L)

            teleportToSpawn(player)
            player.inventory.clear()
            restoreStats(player)
        }

        state = GameState.RUN

        intervalTicks = (interval * 20).toInt()
        tickCounter = 0
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

    override fun update() {
        if (isFinished || state != GameState.RUN) return

        tickCounter++

        if (tickCounter >= intervalTicks) {
            tickCounter = 0
            giveRandomItem()
        }
    }

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

    override fun getGameState(): GameState { return state }

    override fun summarize(): Map<UUID, Double> {
        return result

    }

    override fun finish() {
        isFinished = true

        state = GameState.FINISH
    }
}