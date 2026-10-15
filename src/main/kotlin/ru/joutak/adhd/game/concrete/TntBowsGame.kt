package ru.joutak.adhd.game.concrete

import net.minecraft.world.phys.Vec3
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.config.map.meta.concrete.TntBowsMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.TntBowsModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
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
    }
    fun moveTnt(tnt: Entity) {
        val tntBowsMapMeta = arena.metas["tnt_bows"] as? TntBowsMapMeta ?: return
        val tntSpeed = gameMeta?.movementSpeedTnt ?: 0.0

        Bukkit.getLogger().info("$tntSpeed")

        tntBowsMapMeta.tntBounds.forEachIndexed { index, linePoints ->
            val startPoint = linePoints.getOrNull(0) ?: return@forEachIndexed
            val endPoint = linePoints.getOrNull(1) ?: return@forEachIndexed

            Bukkit.getLogger().info("$startPoint")
            Bukkit.getLogger().info("$endPoint")

            val startX = startPoint.x
            val startZ = startPoint.z


            //Bukkit.getLogger().info("$startPoint in $endPoint")
            //Bukkit.getLogger().info("$startX in $startZ")

            val endX = endPoint.x
            val endZ = endPoint.z

            val direction = Vec3(endX - startX, 0.0, endZ - startZ).normalize()

            //Bukkit.getLogger().info("${direction.x}, ${direction.y}, ${direction.z}")

            val nextLoc = Location(
                tnt.world,
                tnt.x + (tntSpeed/20)*direction.x,
                tnt.y,
                tnt.z + (tntSpeed/20)*direction.z)
                Bukkit.getWorld("world")
                //Bukkit.getLogger().info("${tnt.x}, ${tnt.y}, ${tnt.z}, ${tnt.world}")

            tnt.teleport(nextLoc)

            Bukkit.getWorld("world")
            //Bukkit.getLogger().info("${tnt.x}, ${tnt.y}, ${tnt.z}, ${tnt.world}")
        }
    }

    fun spawnTnt(start: SpawnPoint, end: SpawnPoint) {
        Bukkit.getLogger().info("connect")
        val world = Bukkit.getWorld(worldName) ?: return
        val offsetX = floor(arena.spawnPoints[0].x / 512) * 512
        val offsetZ = floor(arena.spawnPoints[0].z / 512) * 512
        val startLoc = Location(world, start.x + offsetX, start.y, start.z + offsetZ)
        Bukkit.getLogger().info("${start.x}, ${start.y}, ${start.z}")
        Bukkit.getLogger().info("${Bukkit.getWorld(worldName)}")

        val armorStand = startLoc.world.spawn(startLoc, ArmorStand::class.java) { stand ->
            stand.isVisible = true
            stand.setGravity(false)
            stand.setBasePlate(false)
            stand.setArms(false)
            stand.isSmall = true
        }

        val tntFallingBlock = startLoc.world.spawn(startLoc, FallingBlock::class.java) { fb ->
            fb.blockData = Material.TNT.createBlockData()
            fb.setGravity(false)
            fb.setDropItem(false)
            fb.setCancelDrop(true)
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
        bow.addEnchantment(Enchantment.INFINITY, 1)
        bow.addEnchantment(Enchantment.FLAME, 1)
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
            counterTicks = 50L

            tntBowsMapMeta.tntBounds.forEachIndexed { index, linePoints ->
                val startPoint = linePoints.getOrNull(0) ?: return@forEachIndexed
                val endPoint = linePoints.getOrNull(1) ?: return@forEachIndexed

                spawnTnt(startPoint, endPoint)
            }
        }
        counterTicks--
        for (stand in activeArmorStands) {
            if (!stand.isValid) {
                continue
            }
            moveTnt(stand)
        }


    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH

        activeEntity.forEach { entity ->
            if (entity.isValid) {
                entity.remove()
            }
        }

        activeArmorStands.forEach { entity ->
            if (entity.isValid) {
                entity.remove()
            }
        }

        activeEntity.clear()
        activeArmorStands.clear()
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}
