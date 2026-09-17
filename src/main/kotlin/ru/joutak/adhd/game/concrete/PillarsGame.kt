package ru.joutak.adhd.game.concrete

import org.bukkit.inventory.ItemStack
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.map.meta.concrete.PillarsMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
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

    private lateinit var set: MutableMap<String, MutableList<Material>>

    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        this.meta = modeMeta as? PillarsModeMeta ?: throw IllegalArgumentException("PillarsGame requires PillarsModeMeta.")

        set = getRandomSet() ?: return

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
            for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            if (player.isDead || !player.isOnline) continue

            getRandomItem(set)?.let { item ->
                player.inventory.addItem(item)
            }
        }
        }
    }

    private fun getRandomSet(): MutableMap<String, MutableList<Material>>? {
        val mapMeta = arena.metas["pillars"] as? PillarsMapMeta
        val bannedSetsFromMap = mapMeta?.setsBanList ?: emptyList()

        val availableSets: Map<String, MutableMap<String, MutableList<Material>>> = if (bannedSetsFromMap.isNotEmpty()) {
            meta.getSetsExcluding(bannedSetsFromMap)
        } else{
            meta.getAllSets()
        }

        ADHDPlugin.instance.logger.info("Pillars: banned=$bannedSetsFromMap, availableSets keys=${availableSets.keys}, size=${availableSets.size}")

        return availableSets.values.randomOrNull()
    }

    private fun getRandomItem(currentSet: MutableMap<String, MutableList<Material>>): ItemStack? {
        if (currentSet.isEmpty()) return null

        val randomType = currentSet.keys.random()
        val materials = currentSet[randomType] ?: return null
        if (materials.isEmpty()) return null

        val randomMaterial = materials.random()

        return ItemStack(randomMaterial)
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