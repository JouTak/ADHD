package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import ru.joutak.adhd.config.map.meta.concrete.ParkourMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID
import kotlin.math.floor

class ParkourGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var state = GameState.START

    var result = mutableMapOf<UUID, Double>()

    val finishes = mutableSetOf<Interaction>()

    var lSpawn: SpawnPoint? = null

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        val meta = arena.metas["parkour"] as? ParkourMapMeta ?: error("Arena must have parkour meta for this mode to operate...")

        val world = Bukkit.getWorld(worldName)!!

        val xOffset = floor(arena.spawnPoints[0].x / 512) * 512

        val zOffset = floor(arena.spawnPoints[0].z / 512) * 512

        for (p in meta.finish) {
            val loc = Location(world, p.x + xOffset, p.y, p.z + zOffset)

            val interaction = world.spawn(loc, Interaction::class.java) {
                it.interactionWidth = 1f
                it.interactionHeight = 1f
            }

            finishes.add(interaction)
        }

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            teleportToSpawn(player)

            restoreStats(player)
        }
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
        player.inventory.clear()
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