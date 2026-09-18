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
import kotlin.random.Random

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
    var updateRate: Int = 0
    var ticks: Int = 0
    var chance: Double = 0.0
    var awardPoints: Int = 0
    var finePoints: Int = 0
    var winPoints: Int = 0
    var result = mutableMapOf<UUID, Double>()
    var scores = mutableMapOf<UUID, Int>()
    var prevLoc = mutableMapOf<UUID, Pair<Int, Int>>()
    var respawns = mutableMapOf<UUID, SpawnPoint>()
    var state = GameState.START


    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.worldName = worldName
        this.arena = arena
        this.members = members
        var spawns = arena.spawnPoints.toMutableSet()

        val meta = modeMeta as DanceFloorModeMeta
        width = meta.width
        length = meta.length
        green_material = Material.valueOf(meta.green.toString())
        red_material = Material.valueOf(meta.red.toString())
        neutral_material = Material.valueOf(meta.neutral.toString())
        updateRate = meta.interval_ticks
        chance = meta.green_chacne
        awardPoints = meta.green_points
        finePoints = meta.red_penalty
        winPoints = meta.win_points

        generateMap()

        for (uuid in members){
            val player = Bukkit.getPlayer(uuid) ?: continue
            val spawn = spawns.random()
            spawns -= mutableSetOf(spawn)
            scores[player.uniqueId] = 0
            result[player.uniqueId] = 0.0
            respawns[player.uniqueId] = spawn
            spawnPlayer(player, spawn)
        }
    }

    override fun update() {
        if (state != GameState.RUN) return
        if (ticks % updateRate == 0) generateBlock()
        ticks++
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH
        var winner: UUID
        var max: Int = -1000000
        for((id, score) in scores){
            if (score > max) {
                max = score
                winner = id
            }
        }
        result[winner] = 1.0
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }

    fun spawnPlayer(player: Player, spawn: SpawnPoint){

        player.teleport(Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
        prevLoc[player.uniqueId] = Pair(player.x.toInt(), player.z.toInt())
    }

    fun generateMap(){
        for (x in 1..width){
            for (z in 1..length){
                world.getBlockAt(Location(world, x.toDouble(), 0.0, z.toDouble())).type = neutral_material
                world.getBlockAt(Location(world, -x.toDouble(), 0.0, z.toDouble())).type = neutral_material
                world.getBlockAt(0, 1, z).type = Material.BARRIER
                world.getBlockAt(0, 2, z).type = Material.BARRIER
            }
        }
    }

    fun generateBlock(){
        var type: Material = red_material
        var x: Int = Random.nextInt(width)
        var z: Int = Random.nextInt(length)
        if (Random.nextDouble(0.0, 1.0.next_up()) <= chance) type = green_material
        world.getBlockAt(x + 1, 0, z + 1).type = type
        world.getBlockAt(-x - 1, 0, z + 1).type = type
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
        scores[player.uniqueId] += awardPoints
        if (scores[player.uniqueId] >= winPoints){
            finish()
        }
    }

    fun fine(player: Player){
        scores[player.uniqueId] -= finePoints
    }
}