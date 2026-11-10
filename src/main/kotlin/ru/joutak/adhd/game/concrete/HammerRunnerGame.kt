package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.HammerRunnerModeMeta
import ru.joutak.adhd.listener.FreezeListener
import ru.joutak.adhd.world.Arena
import java.util.*

class HammerRunnerGame: Game() {
    lateinit var worldName: String
    lateinit var world: World
    lateinit var arena: Arena
    lateinit var members: Set<UUID>

    lateinit var player: Player
    lateinit var origin: Location
    lateinit var rabbit: ArmorStand
    private var rabbitPositions = mutableSetOf<Location>()
    private var result = mutableMapOf<UUID, Double>()
    private var state: GameState = GameState.START
    private var hits: Int = 0
    private var tiks: Int = 0

    lateinit var hammerMaterial: Material
    lateinit var hammer: ItemStack
    private var hammerName: String = ""
    private var winHits: Int = 0
    private var tickInterval: Int = 0

    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.worldName = worldName
        this.arena = arena
        this.members = members
        world = Bukkit.getWorld(worldName)!!

        val meta = modeMeta as HammerRunnerModeMeta
        hammerMaterial = Material.valueOf(meta.hammerMaterial)
        hammerName = meta.hammerName
        winHits = meta.winHits
        hammer = ItemStack.of(hammerMaterial, 1)
        hammer.itemMeta.setDisplayName(hammerName)
        tickInterval = meta.tickInterval

        for(UUID in members){
            val spawn = arena.spawnPoints.random()
            FreezeListener.freeze[UUID] = true
            Bukkit.getScheduler().runTaskLater(ADHDPlugin.instance, Runnable {
                rabbit = world.spawn(Location(world, 0.0, 0.0, 0.0), ArmorStand::class.java)
                rabbit.isSmall = true
                rabbit.isGlowing = true
                rabbit.setGravity(false)
                rabbit.setArms(true)
                rabbit.setBasePlate(false)
                FreezeListener.freeze[UUID] = false
                state = GameState.RUN}, 20L)
            player = Bukkit.getPlayer(UUID)!!
            player.teleport(Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
            origin = player.location
        }
    }

    override fun update() {
        if (state != GameState.RUN) return
        if (tiks % tickInterval == 0){
            spawnRabbit()
        }
        tiks++
    }
    override fun summarize(): Map<UUID, Double> {
         return result
    }

    override fun finish() {
        state = GameState.FINISH
        rabbit.kill()
    }

    override fun getGameState(): GameState {
        return state
    }

    fun hit() {
        hits++
        if (hits > winHits){
            result[player.uniqueId] = 1.0
            finish()
        }
    }

    fun spawnRabbit() {
        val newPoint = rabbitPositions.random()
        rabbit.teleport(Location(world, origin.x + newPoint.x, origin.y + newPoint.y, origin.z + newPoint.z))
    }
}