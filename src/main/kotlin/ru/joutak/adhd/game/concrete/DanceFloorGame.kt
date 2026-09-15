package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.Location
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.DanceFloorModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import org.bukkit.entity.Player

import java.util.*

class DanceFloorGame : Game() {
    lateinit var arena: Arena
    lateinit var worldName: String
    lateinit var members: Set<UUID>

    var result = mutableMapOf<UUID, Double>()
    var state = GameState.START


    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.worldName = worldName
        this.arena = arena
        this.members = members
        var spawns = arena.spawnPoints.toMutableSet()

        val meta = modeMeta as? DanceFloorModeMeta

        for (uuid in members){
            val player = Bukkit.getPlayer(uuid) ?: continue
            val spawn = spawns.random()
            spawns -= mutableSetOf(spawn)
            SpawnPlayer(player, spawn)
        }
    }

    override fun update() {
        if (state != GameState.RUN) return

    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        return
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }

    fun SpawnPlayer(player: Player, spawn: SpawnPoint){
        val world = Bukkit.getWorld(worldName)!!
        player.teleport(Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
    }
}