package ru.joutak.adhd.game.concrete

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.checkerframework.checker.units.qual.C
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.loader.concrete.RabbitSpawnPoint
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.HammerRunnerModeMeta
import ru.joutak.adhd.listener.FreezeListener
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.*

class HammerRunnerGame: Game() {
    lateinit var worldName: String
    lateinit var world: World
    lateinit var arena: Arena
    lateinit var members: Set<UUID>

    lateinit var gamer: Player
    lateinit var origin: Location
    lateinit var rabbit: ArmorStand
    private var rabbitPoints = mutableSetOf<SpawnPoint>()
    private var result = mutableMapOf<UUID, Double>()
    private var state: GameState = GameState.START
    private var hits: Int = 0
    private var tiks: Int = 0

    lateinit var hammerMaterial: Material
    lateinit var hammer: ItemStack
    private var hammerName: String = ""
    private var winHits: Int = 0
    private var tickInterval: Int = 0
    private var rabbitMessage: String = ""

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
        hammer.editMeta { hmeta ->
            hmeta.itemName(Component.text(hammerName))
        }
        tickInterval = meta.tickInterval
        rabbitPoints = meta.rabbitPoints
        rabbitMessage = meta.rabbitMessage

        for(UUID in members){
            val spawn = arena.spawnPoints.random()
            FreezeListener.freeze[UUID] = true
            Bukkit.getScheduler().runTaskLater(ADHDPlugin.instance, Runnable {
                rabbit = world.spawn(Location(world, 0.0, 0.0, 0.0), ArmorStand::class.java)
                rabbit.setGravity(false)
                rabbit.setArms(true)
                FreezeListener.freeze[UUID] = false
                state = GameState.RUN}, 20L)
            gamer = Bukkit.getPlayer(UUID)!!
            gamer.teleport(Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
            setPlayer(gamer)
            origin = gamer.location
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
        rabbit.remove()
    }

    override fun getGameState(): GameState {
        return state
    }

    fun hit() {
        hits++
        tiks = 0
        if (hits > winHits){
            result[gamer.uniqueId] = 1.0
            finish()
        }
    }

    fun spawnRabbit() {
        val newPoint = rabbitPoints.random()
        rabbit.remove()
        rabbit = world.spawn(Location(world, origin.x + newPoint.x, origin.y + newPoint.y, origin.z + newPoint.z, newPoint.yaw, newPoint.pitch), ArmorStand::class.java)
        rabbit.setGravity(false)
        rabbit.setArms(true)
        gamer.showTitle(Title.title(Component.text(rabbitMessage), Component.empty(), 5, 20, 10))
    }

    fun setPlayer(player: Player) {
        player.inventory.clear()
        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
        player.inventory.setItem(0, hammer)
        }
}