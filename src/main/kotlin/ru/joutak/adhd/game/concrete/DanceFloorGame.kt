package ru.joutak.adhd.game.concrete

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.DanceFloorModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import org.bukkit.entity.Player
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.listener.FreezeListener
import kotlin.random.Random

import java.util.*

class DanceFloorGame : Game() {
    lateinit var arena: Arena
    lateinit var worldName: String
    lateinit var members: Set<UUID>
    lateinit var green_material: Material
    lateinit var red_material: Material
    lateinit var neutral_material: Material
    lateinit var world: World
    var width: Int = 0
    var length: Int = 0
    var updateRate: Int = 0
    var ticks: Int = 0
    var chance: Double = 0.0
    var awardPoints: Int = 0
    var finePoints: Int = 0
    var winPoints: Int = 0
    var lz: Int = 0
    var lx: Int = 0
    var ly: Int = 0
    var critical_y: Int = 0
    var result = mutableMapOf<UUID, Double>()
    var scores = mutableMapOf<UUID, Int>()
    var prevLoc = mutableMapOf<UUID, Pair<Int, Int>>()
    var respawns = mutableMapOf<UUID, Location>()
    var state = GameState.START


    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.worldName = worldName
        world = Bukkit.getWorld(worldName)!!
        this.arena = arena
        this.members = members
        var spawns = arena.spawnPoints.toMutableSet()
        for (spawn in spawns){
            lx += spawn.x.toInt()
        }
        lx /= 2
        ly = arena.spawnPoints[0].y.toInt() - 1
        lz = arena.spawnPoints[0].z.toInt() - 1

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
        critical_y = meta.critical_y
        generateMap()

        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)
        world.setGameRule(GameRules.FALL_DAMAGE, false)


        for (uuid in members){
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.gameMode = GameMode.ADVENTURE
            val spawn = spawns.random()
            spawns -= mutableSetOf(spawn)
            FreezeListener.freeze[uuid] = true
            Bukkit.getScheduler().runTaskLater(ADHDPlugin.instance, Runnable { FreezeListener.freeze[uuid] = false }, 60L)
            spawnPlayer(player, spawn)
            respawns[player.uniqueId] = Location(world, player.x, player.y, player.z)
            scores[player.uniqueId] = 0
        }
        state = GameState.RUN
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
    }

    override fun summarize(): Map<UUID, Double> {
        if (state != GameState.FINISH) finish()
        if (scores.values.toSet().toMutableSet().size == 1) return result
        result[scores.maxBy{it.value}.key] = 1.0
        return result
    }

    fun spawnPlayer(player: Player, spawn: SpawnPoint?){
        if (spawn == null) return
        var delta: Int = if (spawn.x > lx)  1 else -1
        player.teleport(Location(world, spawn.x + width / 2 * delta, spawn.y, spawn.z + length / 2, spawn.yaw, spawn.pitch))
        prevLoc[player.uniqueId] = Pair(player.x.toInt(), player.z.toInt())
    }

    fun generateMap(){
        for (x in 1..width){
            for (z in 1..length){
                world.getBlockAt(lx + x, ly, lz + z).type = neutral_material
                world.getBlockAt(lx -x, ly, lz + z).type = neutral_material
                world.getBlockAt(lx, ly + 1, lz + z).type = Material.BARRIER
                world.getBlockAt(lx, ly + 2,  lz + z).type = Material.BARRIER
            }
        }
        for (z in 0..3){
            world.getBlockAt(lx, ly + 1, lz - z).type = Material.BARRIER
            world.getBlockAt(lx, ly + 2, lz - z).type = Material.BARRIER
            world.getBlockAt(lx, ly + 1, lz + length + z).type = Material.BARRIER
            world.getBlockAt(lx, ly + 2, lz + length + z).type = Material.BARRIER
        }
    }

    fun generateBlock(){
        var type: Material = red_material
        val x: Int = Random.nextInt(width)
        val z: Int = Random.nextInt(length)
        if (Random.nextDouble(0.0, 1.0) <= chance) type = green_material
        world.getBlockAt(x + lx + 1, ly, z + lz + 1).type = type
        world.getBlockAt(-x  + lx - 1, ly, z + lz + 1).type = type
    }

    fun checkBlock(player: Player){
        val loc = Pair(player.x.toInt(), player.z.toInt())
        if (loc == prevLoc[player.uniqueId]) return
        prevLoc[player.uniqueId] = loc
        when (world.getBlockAt(player.x.toInt(), player.y.toInt() - 1, player.z.toInt()).type){
            neutral_material -> return
            green_material -> award(player)
            red_material -> fine(player)
            else -> return
        }
    }

    fun award(player: Player){
        scores[player.uniqueId] = scores[player.uniqueId]!! + awardPoints
        world.getBlockAt(player.x.toInt(), ly, player.z.toInt()).type = neutral_material
        player.sendMessage(Component.text("Ты наступил на зелёную клетку, сейчас у тебя " + scores[player.uniqueId] + " очков").color(NamedTextColor.GREEN))
        if (scores[player.uniqueId]!! >= winPoints){
            finish()
        }
    }

    fun fine(player: Player){
        scores[player.uniqueId] = scores[player.uniqueId]!! - finePoints
        world.getBlockAt(player.x.toInt(), ly, player.z.toInt()).type = neutral_material
        player.sendMessage(Component.text("Ты наступил на красную клетку, сейчас у тебя " + scores[player.uniqueId] + " очков").color(NamedTextColor.RED))
    }
}