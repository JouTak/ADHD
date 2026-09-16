package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
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
    lateinit var green_material: Material
    lateinit var red_material: Material
    lateinit var neutral_material: Material

    val world = Bukkit.getWorld(worldName)!!
    var width: Int = 0
    var length: Int = 0
    var result = mutableMapOf<UUID, Double>()
    var scores = mutableMapOf<UUID, Int>()
    var prevLoc = mutableMapOf<UUID, Pair<Int, Int>>()
    var state = GameState.START


    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.worldName = worldName
        this.arena = arena
        this.members = members
        var spawns = arena.spawnPoints.toMutableSet()

        val meta = modeMeta as DanceFloorModeMeta
        this.width = meta.width
        this.length = meta.length
        this.green_material = Material.valueOf(meta.green.toString())
        this.red_material = Material.valueOf(meta.red.toString())
        this.neutral_material = Material.valueOf(meta.neutral.toString())

        generateFloor()

        for (uuid in members){
            val player = Bukkit.getPlayer(uuid) ?: continue
            val spawn = spawns.random()
            spawns -= mutableSetOf(spawn)
            scores[player.uniqueId] = 0
            spawnPlayer(player, spawn)
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

    fun spawnPlayer(player: Player, spawn: SpawnPoint){

        player.teleport(Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
        prevLoc[player.uniqueId] = Pair(player.x.toInt(), player.z.toInt())
    }

    fun generateFloor(){
        for (x in 1..width){
            for (z in 1..length){
                world.getBlockAt(Location(world, x.toDouble(), 0.0, z.toDouble())).type = neutral_material
                world.getBlockAt(Location(world, -x.toDouble(), 0.0, z.toDouble())).type = neutral_material
            }
        }
    }

    fun checkBlock(player: Player){
        val loc = Pair(player.x.toInt(), player.z.toInt())
        if (loc == prevLoc[player.uniqueId]) return
        prevLoc[player.uniqueId] = loc
        when (world.getBlockAt(Location(world, player.x, player.y - 1.0, player.z)).type){
            neutral_material -> return
            green_material -> award(player)
            red_material -> fine(player)
        }
    }

    fun award(player: Player){

    }

    fun fine(player: Player){

    }
}