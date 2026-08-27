package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
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