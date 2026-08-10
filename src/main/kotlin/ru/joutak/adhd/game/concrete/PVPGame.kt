package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID

class PVPGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var result = mutableMapOf<UUID, Double>()

    var state = GameState.START

    var lSpawn: SpawnPoint? = null

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?,
        variantParameters: Map<String, String>,
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        val world = Bukkit.getWorld(worldName)!!

        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            teleportToSpawn(player)

            restoreStats(player)

            giveLayout(player)
        }

        state = GameState.RUN
    }

    fun teleportToSpawn(player: Player) {
        val world = Bukkit.getWorld(worldName)!!

        val spawns = arena.spawnPoints.toMutableSet()

        if (lSpawn != null) {
            spawns -= mutableSetOf(lSpawn!!)
        }

        val chosen: SpawnPoint = if (spawns.isEmpty()) {
            lSpawn!!
        } else {
            spawns.random()
        }

        lSpawn = chosen

        player.teleport(Location(world, chosen.x, chosen.y, chosen.z, chosen.yaw, chosen.pitch))
    }

    fun restoreStats(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
    }

    fun giveLayout(player: Player) {
        player.inventory.clear()

        player.inventory.setItem(0, ItemStack(Material.NETHERITE_SWORD, 1))

        player.inventory.heldItemSlot = 0
    }

    fun calculateResult(player: Player) {
        members.filter { uUID -> uUID != player.uniqueId }.forEach { uUID -> result[uUID] = 1.0 }
    }

    override fun update() {

    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}
