package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.SaperModeMeta
import ru.joutak.adhd.world.Arena
import java.util.*

class SaperGame : Game() {
    lateinit var arena: Arena
    lateinit var worldName: String
    lateinit var members: Set<UUID>
    lateinit var world: World

    var state = GameState.START

    private val result: Map<UUID, Double> = mutableMapOf()

    private var fieldWidth: Int = 0
    private var fieldHeight: Int = 0
    private var mines: Int = 0
    private var closedMaterial: String = ""
    private var freeMaterial: String = ""
    private var digitMaterial: String = ""
    private var mineMaterial: String = ""
    private val colors: MutableList<String> = mutableListOf()

    private val origin: Location = Location(null, 0.0, 0.0, 0.0)
    private val field: MutableList<MutableList<Int>> = mutableListOf()
    private val flags: MutableList<Int> = mutableListOf()
    private val closed: MutableSet<Int> = mutableSetOf()
    private val positionsForGenerator: MutableSet<Int> = mutablseSetOf()
    private val blocks: MutableList<Block> = mutableListOf()
    private var started: Boolean = false
    lateinit var player: Player

    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.arena = arena
        this.worldName = worldName
        this.members = members

        val meta = modeMeta as? SaperModeMeta ?: return
        world = Bukkit.getWorld(worldName)!!
        fieldWidth = meta.fieldWidth
        fieldHeight = meta.fieldHeight
        mines = if(meta.mines < meta.fieldWidth * meta.fieldHeight) meta.mines else meta.fieldWidth * meta.fieldHeight - 1
        closedMaterial = meta.closeMaterial
        freeMaterial = meta.freeMaterial
        digitMaterial = meta.digitMaterial
        mineMaterial = meta.mineMaterial

        origin.world = world
        origin.x = arena.spawnPoints[0].x
        origin.y = arena.spawnPoints[0].y
        origin.z = arena.spawnPoints[0].z

        for(UUID in members){
            player = Bukkit.getPlayer(UUID)!!
            player.inventory.clear()
            player.gameMode = GameMode.ADVENTURE
            player.health = 20.0
            player.saturation = 20.0f
            player.foodLevel = 20
        }
        state = GameState.RUN
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

    override fun update() {
        TODO("Not yet implemented")
    }

    fun open(number: Int){
        if (!closed.contains(number)) return
        closed.remove(number)
        if(!started){
            started = true;
            positionsForGenerator.remove(number)
            generator()
        }
        when (field[number / fieldWidth][number % fieldWidth]){
            0 -> {
                blocks[number].type = Material.valueOf(freeMaterial)
                if((number / fieldWidth) > 0) open(number - fieldWidth)
                if((number / fieldWidth) < fieldHeight - 1) open(number + fieldWidth)
                if(number > 0) open(number - 1)
                if(number < fieldWidth - 1) open(number + 1)
            }
            -1 -> {
                blocks[number].type = Material.valueOf(mineMaterial)
                finish()
            }
            else -> {
                blocks[number].type = Material.valueOf(digitMaterial)
            }
        }
    }

    fun normalize(){
        if (fieldWidth < 0){
            origin.x += fieldWidth
            fieldWidth = -fieldWidth
        }
        if (fieldHeight < 0){
            origin.z += fieldHeight
            fieldHeight = -fieldHeight
        }
    }

    fun flag(number: Int){

    }

    fun generator(){
        for (i in 0..mines) {
            val target: Int = positionsForGenerator.random()
            positionsForGenerator.remove(target)
            field[target / fieldWidth][target % fieldWidth] = -1
            for (yOffset in -1..1) {
                for (xOffset in -1..1) {
                    val temp: Int = target + fieldWidth * yOffset + xOffset
                    if (0 <= temp && temp < fieldWidth * fieldHeight) {
                        if (field[temp / fieldWidth][temp % fieldWidth] != -1) {
                            field[temp / fieldWidth][temp % fieldWidth] += 1
                        }
                    }
                }
            }
        }
    }
}