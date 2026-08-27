package ru.joutak.adhd.game.concrete

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.map.meta.concrete.MemoryMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID
import kotlin.math.floor
import kotlin.random.Random

class MemoryGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var state = GameState.START

    val result = mutableMapOf<UUID, Double>()

    lateinit var points: List<SpawnPoint>

    val stands = mutableSetOf<ArmorStand>()

    val colorStands = mutableMapOf<ArmorStand, Color>()

    val soundStands = mutableMapOf<ArmorStand, Float>()

    val pool = mutableListOf<ArmorStand>()

    val countDownTask = object : BukkitRunnable() {
        var seconds = 5

        override fun run() {
            if (seconds == 0) {
                cancel()

                acknowledgeTask.runTaskTimer(ADHDPlugin.instance, 20L, 20L)
            }

            val title = Title.title(Component.text(seconds).color(NamedTextColor.GOLD), Component.text(""), 5, 10, 5)

            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                player.showTitle(title)

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            }

            seconds -= 1
        }
    }

    val acknowledgeTask = object : BukkitRunnable() {
        var pointer = 0

        var last: ArmorStand? = null

        override fun run() {
            last?.let { it.isGlowing = false }

            if (pointer == pool.size) {
                cancel()

                acknowledged = true

                return
            }

            val stand = pool[pointer]

            last = stand

            stand.isGlowing = true

            Bukkit.getWorld(worldName)?.playSound(stand.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, soundStands[stand]!!)

            pointer++
        }
    }

    var acknowledged = false

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            setPlayer(player)
        }

        val meta = arena.metas["memory"] as? MemoryMapMeta ?: error("Arena must have meta for this mode to operate...")

        points = meta.points

        spawnStands()

        fillPool()

        countDownTask.runTaskTimer(ADHDPlugin.instance, 100L, 20L)
    }

    fun spawnStands() {
        val world = Bukkit.getWorld(worldName)!!

        val offsetX = floor(arena.spawnPoints[0].x / 512) * 512

        val offsetZ = floor(arena.spawnPoints[0].z / 512) * 512

        for (p in points) {
            val loc = Location(world, p.x + offsetX, p.y, p.z + offsetZ)

            val stand = world.spawn(loc, ArmorStand::class.java)

            stands.add(stand)

            stand.setGravity(false)
            stand.isInvisible = false
            stand.isInvulnerable = true
            stand.setArms(false)
            stand.setBasePlate(true)

            equipStand(stand)
        }
    }

    fun colorItem(material: Material, color: Color): ItemStack {
        val item = ItemStack(material, 1)

        val meta = item.itemMeta as LeatherArmorMeta

        meta.setColor(color)

        item.itemMeta = meta

        return item
    }

    fun equipStand(stand: ArmorStand) {
        val color = Color.fromRGB(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))

        stand.equipment.setHelmet(colorItem(Material.LEATHER_HELMET, color))

        stand.equipment.setChestplate(colorItem(Material.LEATHER_CHESTPLATE, color))

        stand.equipment.setLeggings(colorItem(Material.LEATHER_LEGGINGS, color))

        stand.equipment.setBoots(colorItem(Material.LEATHER_BOOTS, color))

        soundStands[stand] = (0.5 + 0.1 * soundStands.size).toFloat().coerceAtMost(2.0f)

        colorStands[stand] = color
    }

    fun fillPool() {
        var last: ArmorStand? = null

        for (i in 0..Random.nextInt(5, 7)) {
            val current = (stands - mutableSetOf(last)).random()!!

            last = current

            pool.add(current)
        }
    }

    fun setPlayer(player: Player) {
        player.inventory.clear()

        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20

        val spawn = arena.spawnPoints.random()

        player.teleport(Location(Bukkit.getWorld(worldName)!!, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
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