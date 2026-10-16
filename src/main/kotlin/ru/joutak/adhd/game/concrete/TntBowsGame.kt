package ru.joutak.adhd.game.concrete

import net.minecraft.world.phys.Vec3
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.config.map.meta.concrete.TntBowsMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.TntBowsModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import ru.joutak.minigames.MiniGamesAPI.plugin
import java.util.*
import kotlin.math.floor

class TntBowsGame : Game() {

    lateinit var worldName: String
    lateinit var arena: Arena
    lateinit var members: Set<UUID>
    var result = mutableMapOf<UUID, Double>()
    var state = GameState.START
    var lSpawn: SpawnPoint? = null

    var gameMeta: TntBowsModeMeta? = null

    var activeEntity = mutableListOf<Entity>()
    var activeArmorStands = mutableListOf<Entity>()

    var counterTicks = 1L;

    val keyX = NamespacedKey(plugin, "tntbows_dir_x")
    val keyZ = NamespacedKey(plugin, "tntbows_dir_z")

    val endX = NamespacedKey(plugin, "tntbows_end_x")
    val endY = NamespacedKey(plugin, "tntbows_end_y")
    val endZ = NamespacedKey(plugin, "tntbows_end_z")

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members
        gameMeta = modeMeta as? TntBowsModeMeta

        activeEntity.clear()
        activeArmorStands.clear()

        val world = Bukkit.getWorld(worldName)!!
        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            teleportToSpawn(player)

            restoreStats(player)

            giveLayout(player)
        }

        state = GameState.RUN
        object : BukkitRunnable() {
            override fun run() {
                if (state != GameState.RUN) {
                    cancel()
                    return
                }
                for (stand in activeArmorStands) {
                    moveTnt(stand)
                }
            }
        }.runTaskTimer(ru.joutak.adhd.ADHDPlugin.instance, 0L, 1L)
    }

    fun moveTnt(stand: Entity) {
        val tntSpeed = gameMeta?.movementSpeedTnt ?: 0.0

        val pdc = stand.persistentDataContainer

        val curX = stand.x
        val curY = stand.y
        val curZ = stand.z

        val endX = pdc.get(endX, PersistentDataType.DOUBLE) ?: 0.0
        val endY = pdc.get(endY, PersistentDataType.DOUBLE) ?: 0.0
        val endZ = pdc.get(endZ, PersistentDataType.DOUBLE) ?: 0.0

        val diff = Vec3(endX-curX, endY-curY, endZ-curZ).length()

        if (diff <= 1){
            val passenger = stand.passengers.firstOrNull()
            stand.remove()
            passenger?.remove()
            return
        }

        val dirX = pdc.get(keyX, PersistentDataType.DOUBLE) ?: 0.0
        val dirZ = pdc.get(keyZ, PersistentDataType.DOUBLE) ?: 0.0

        val nextLoc = Location(
            stand.world,
            stand.x + (tntSpeed/10)*dirX,
            stand.y,
            stand.z + (tntSpeed/10)*dirZ)

        val passenger = stand.passengers.firstOrNull()
        stand.eject()
        stand.teleport(nextLoc)
        if (passenger != null && passenger.isValid) {
            stand.addPassenger(passenger)
        }
    }

    fun spawnTnt(start: SpawnPoint, end: SpawnPoint) {
        val world = Bukkit.getWorld(worldName) ?: return
        val offsetX = floor(arena.spawnPoints[0].x / 512) * 512
        val offsetZ = floor(arena.spawnPoints[0].z / 512) * 512
        val startLoc = Location(world, start.x + offsetX, start.y, start.z + offsetZ)

        val armorStand = startLoc.world.spawn(startLoc, ArmorStand::class.java) { stand ->
            stand.isVisible = false
            stand.setGravity(false)
            stand.setBasePlate(false)
            stand.setArms(false)
            stand.isSmall = true
        }

        val direction = Vec3(end.x - start.x, 0.0, end.z - start.z).normalize()

        val pdc = armorStand.persistentDataContainer
        pdc.set(keyX, PersistentDataType.DOUBLE, direction.x)
        pdc.set(keyZ, PersistentDataType.DOUBLE, direction.z)
        pdc.set(endX, PersistentDataType.DOUBLE, end.x + offsetX)
        pdc.set(endY, PersistentDataType.DOUBLE, end.y)
        pdc.set(endZ, PersistentDataType.DOUBLE, end.z + offsetZ)

        val tntFallingBlock = startLoc.world.spawn(startLoc, FallingBlock::class.java) { fb ->
            fb.blockData = Material.TNT.createBlockData()
            fb.setGravity(false)
            fb.dropItem = false
            fb.cancelDrop = true
        }

        armorStand.addPassenger(tntFallingBlock)

        activeEntity.add(tntFallingBlock)
        activeArmorStands.add(armorStand)
    }

    fun teleportToSpawn(player: Player) {
        val world = Bukkit.getWorld(worldName)!!

        val spawns = arena.spawnPoints.toMutableSet()

        if (lSpawn != null) spawns -= mutableSetOf(lSpawn!!)

        val chosen: SpawnPoint = if (spawns.isEmpty())
            lSpawn!!
        else
            spawns.random()

        lSpawn = chosen

        player.teleport(Location(world, chosen.x, chosen.y, chosen.z, chosen.yaw, chosen.pitch))
    }

    fun restoreStats(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
    }

    fun giveLayout(player: Player) {
        player.inventory.clear()

        val bow = ItemStack(Material.BOW)
        val bowMeta = bow.itemMeta
        bowMeta.isUnbreakable = true
        bowMeta.addEnchant(Enchantment.INFINITY, 1, true)
        bowMeta.addEnchant(Enchantment.FLAME, 1, true)
        bowMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)

        bow.itemMeta = bowMeta

        player.inventory.setItem(0, bow)
        player.inventory.setItem(8, ItemStack(Material.ARROW, 1))

        player.inventory.heldItemSlot = 0
    }

    fun calculateResult(player: Player) {
        members.filter { uUID -> uUID != player.uniqueId }.forEach { uUID -> result[uUID] = 1.0 }
    }

    override fun update() {
        if (state != GameState.RUN) return
        val tntBowsMapMeta = arena.metas["tnt_bows"] as? TntBowsMapMeta ?: return

        if (counterTicks <= 0){
            counterTicks = (gameMeta?.spawnSpeedTnt ?: 0) * 10L

            tntBowsMapMeta.tntBounds.forEachIndexed { index, linePoints ->
                val startPoint = linePoints.getOrNull(0) ?: return@forEachIndexed
                val endPoint = linePoints.getOrNull(1) ?: return@forEachIndexed

                spawnTnt(startPoint, endPoint)
            }
        }
        counterTicks--

        activeArmorStands.removeAll { !it.isValid }
        activeEntity.removeAll { !it.isValid }
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH

        activeEntity.forEach { it.remove() }
        activeArmorStands.forEach { it.remove() }

        activeEntity.clear()
        activeArmorStands.clear()
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}
