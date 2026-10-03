package ru.joutak.adhd.game.concrete

import io.papermc.paper.util.Tick
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.EntityEffect
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Warden
import org.bukkit.scheduler.BukkitTask
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.map.meta.concrete.RGLightMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.listener.FreezeListener
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.time.Duration
import java.util.UUID

class RGLightGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    private var result = mutableMapOf<UUID, Double>()

    private var state = GameState.START

    private var playerSpawns = mutableMapOf<UUID, SpawnPoint>()

    private var LightTicks: Int = 0
    private var TickCounter: Int = 0
    private var gameTick = 0
    private var TicksSinceRed: Int = 0
    private val RedGraceTicks = 10

    enum class LightColor {GREEN, RED}
    private var Light: LightColor = LightColor.GREEN

    private var warden: Warden? = null
    private var targetYaw: Float = 0f
    private var rotationTask: BukkitTask? = null

    private val punishCooldown = mutableMapOf<UUID, Int>()

    private var finishX: Double? = null
    private var finishZ: Double? = null
    private val finished = mutableSetOf<UUID>()

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        val world = Bukkit.getWorld(worldName)!!

        val mapMeta = arena.metas["rglight"] as? RGLightMapMeta
        val wardenSpawnPoint = mapMeta?.wardenSpawnPoint
        finishX = mapMeta?.finishX
        finishZ = mapMeta?.finishZ


        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            teleportToSpawn(player)

            FreezeListener.freeze[uuid] = true

            Bukkit.getScheduler().runTaskLater(ADHDPlugin.instance, Runnable { FreezeListener.freeze[uuid] = false }, 40L)

            restoreStats(player)


            player.inventory.clear()
        }

        if (wardenSpawnPoint != null) {
            val loc = Location(world, wardenSpawnPoint.x, wardenSpawnPoint.y, wardenSpawnPoint.z, wardenSpawnPoint.yaw, wardenSpawnPoint.pitch)
            warden = world.spawn(loc, Warden::class.java)
            warden?.setAI(false)
            targetYaw = wardenSpawnPoint.yaw
        }

        state = GameState.RUN
        randomizeLightTicks()
    }

    fun teleportToSpawn(player: Player) {
        val world = Bukkit.getWorld(worldName)!!

        val spawn: SpawnPoint = playerSpawns[player.uniqueId]
            ?: chooseFreeSpawn().also { playerSpawns[player.uniqueId] = it }

        player.teleport(Location(world, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))

    }

    private fun chooseFreeSpawn(): SpawnPoint {
        val occupied = playerSpawns.values.toSet()
        val free = arena.spawnPoints.filter { it !in occupied }

        return if (free.isNotEmpty()) {
            free.random()
        } else {
            arena.spawnPoints.random()
        }
    }

    fun randomizeLightTicks(){
        LightTicks = (40..100).random()
    }

    fun restoreStats(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
    }


    override fun update() {
        TickCounter++
        gameTick++
        if (TickCounter >= LightTicks) {
            ADHDPlugin.instance.logger.info("Смена цвета")
            switchLight()
            TickCounter = 0
        }

        if (Light == LightColor.RED) {
            TicksSinceRed++
        }

    }

    fun isRedGraceOver(): Boolean = TicksSinceRed > RedGraceTicks

    fun punishPlayer(player: Player) {
        val last = punishCooldown[player.uniqueId]
        if (last != null && gameTick - last < 10) return
        punishCooldown[player.uniqueId] = gameTick

        val w = warden
        if (w != null) {
            val world = w.world
            val from = w.eyeLocation
            val to = player.location.clone().add(0.0, 1.0, 0.0)

            w.playEffect(EntityEffect.WARDEN_SONIC_ATTACK)

            world.playSound(from, Sound.ENTITY_WARDEN_SONIC_BOOM, 3f, 1f)
            world.playSound(from, Sound.ENTITY_WARDEN_ROAR, 2f, 1f)

            val dir = to.toVector().subtract(from.toVector())
            val length = dir.length()
            if (length > 0.1) {
                dir.normalize()
                var d = 0.0
                while (d < length) {
                    val p = from.clone().add(dir.clone().multiply(d))
                    world.spawnParticle(Particle.SONIC_BOOM, p, 1, 0.0, 0.0, 0.0, 0.0)
                    d += 0.5
                }
            }
        }

        teleportToSpawn(player)
    }

    fun getLightColor(): LightColor {
        return Light
    }

    fun switchLight(){

        Light = if (Light == LightColor.GREEN) LightColor.RED else LightColor.GREEN

        targetYaw += 180f
        rotateWarden(targetYaw, durationTicks = 10)

        TicksSinceRed = 0
        printCurrentLight()
        randomizeLightTicks()
    }

    fun printCurrentLight(){
        val times = Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofSeconds(1),
            Duration.ofMillis(500)
        )

        val title = Title.title(
            Component.text(if (Light == LightColor.RED) "Красный свет" else "Зелёный свет", if (Light == LightColor.RED) NamedTextColor.RED else NamedTextColor.GREEN,),
            Component.text(if (Light == LightColor.RED) "Не двигайся" else "Можешь идти"),
            times
        )

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.showTitle(title)
        }
    }

    private fun setWardenYaw(w: Warden, yaw: Float) {
        val loc = w.location.clone()
        loc.yaw = yaw
        w.teleport(loc)

        val nms = (w as CraftLivingEntity).handle
        nms.yRot = yaw
        nms.yHeadRot = yaw
        nms.yBodyRot = yaw
    }

    private fun rotateWarden(targetYaw: Float, durationTicks: Int = 10) {
        val w = warden ?: return

        rotationTask?.cancel()

        val startYaw = w.location.yaw
        var delta = (targetYaw - startYaw) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f

        if (delta == 0f) return

        val stepPerTick = delta / durationTicks
        var elapsed = 0

        rotationTask = Bukkit.getScheduler().runTaskTimer(ADHDPlugin.instance, Runnable {
            elapsed++

            val wardenNow = warden
            if (wardenNow == null) {
                rotationTask?.cancel()
                rotationTask = null
                return@Runnable
            }

            val newYaw = if (elapsed >= durationTicks) startYaw + delta
            else startYaw + stepPerTick * elapsed

            setWardenYaw(wardenNow, newYaw)

            if (elapsed >= durationTicks) {
                rotationTask?.cancel()
                rotationTask = null
            }
        }, 0L, 1L)
    }

    override fun getGameState(): GameState {
        return state
    }

    fun hasFinished(player: Player): Boolean = player.uniqueId in finished

    fun isOnFinishLine(player: Player) : Boolean {
        val loc = player.location

        finishZ?.let {
            return loc.z >= it
        }

        finishX?.let {
            return loc.x >= it
        }
        return false
    }

    fun calculateResult(player: Player){
        if (!finished.add(player.uniqueId)) return

        result[player.uniqueId] = 1.0
    }

    override fun finish() {
        warden?.remove()
        warden = null

        state = GameState.FINISH
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}