package ru.joutak.adhd.game.concrete

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ChargedProjectiles
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.FireworkEffect
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.map.meta.concrete.VentilatorMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID
import kotlin.math.floor
import kotlin.random.Random

class SnipersGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var lSpawn: SpawnPoint? = null

    var state = GameState.START

    var result = mutableMapOf<UUID, Double>()

    lateinit var ventilatorFrames: List<Map<BlockDisplay, BlockData>>

    lateinit var ventilatorUsed: Set<BlockDisplay>

    var ventilator = false

    var ventilatorFrame = 0

    var ventilatorFrameTick = 4L

    var gravityChangeTick = 160L

    var gravityDirection = "DOWN"

    val FAN_GRAVITY = NamespacedKey(ADHDPlugin.instance, "fan_gravity")

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

        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            teleportToSpawn(player)

            giveLayout(player)

            restoreStats(player)
        }

        val ventilatorMapMeta = arena.metas["ventilator"] as? VentilatorMapMeta

        if (ventilatorMapMeta != null) {
            val offsetX = floor(arena.spawnPoints[0].x / 512) * 512

            val offsetZ = floor(arena.spawnPoints[0].z / 512) * 512

            val displays = mutableListOf(mutableMapOf<BlockDisplay, BlockData>())

            val usedDisplays = mutableMapOf<String, BlockDisplay>()

            val placement = ventilatorMapMeta.placement

            for (center in ventilatorMapMeta.frames) {
                for (y in -3..3) {
                    for (z in -18..18) {
                        for (x in -18..18) {
                            val block = world.getBlockAt(Location(world, center.x + offsetX + x, center.y + y, center.z + offsetZ + z))

                            if (block.type == Material.AIR) continue

                            val aX = placement.x + offsetX + x
                            val aY = placement.y + y
                            val aZ = placement.z + offsetZ + z

                            val token = "${aX.toInt()} ${aY.toInt()} ${aZ.toInt()}"

                            val targetLocation = Location(world, aX, aY, aZ)

                            val display = usedDisplays[token] ?: world.spawn(targetLocation, BlockDisplay::class.java)

                            display.interpolationDuration = 0
                            display.interpolationDelay = 0

                            usedDisplays[token] = display

                            displays[displays.size - 1][display] = block.blockData

                            if (displays.size == 1) {
                                display.block = block.blockData
                            }
                        }
                    }
                }

                displays.add(mutableMapOf())
            }

            displays.removeLast()

            ventilatorFrames = displays

            ventilatorUsed = usedDisplays.values.toSet()

            ventilator = true

            changeGravity()
        }

        state = GameState.RUN
    }

    fun teleportToSpawn(player: Player) {
        val world = Bukkit.getWorld(worldName)!!

        val spawns = arena.spawnPoints.toMutableSet()

        if (lSpawn != null) {
            spawns -= mutableSetOf(lSpawn!!)
        }

        val chosen: SpawnPoint = if (spawns.isEmpty()) {
            lSpawn!!
        } else {
            spawns.random()
        }

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

        val crossbow = ItemStack(Material.CROSSBOW, 1)

        val firework = buildFirework()

        crossbow.setData(
            DataComponentTypes.CHARGED_PROJECTILES,
            ChargedProjectiles.chargedProjectiles()
                .add(firework)
                .build()
        )

        player.inventory.setItem(0, crossbow)

        player.inventory.heldItemSlot = 0
    }

    fun buildFirework(): ItemStack {
        val firework = ItemStack(Material.FIREWORK_ROCKET, 1)

        val fireworkMeta = firework.itemMeta as FireworkMeta

        fireworkMeta.power = 2

        fireworkMeta.addEffect(
            FireworkEffect.builder()
                .with(FireworkEffect.Type.BURST)
                .trail(true)
                .build()
        )

        firework.itemMeta = fireworkMeta

        return firework
    }

    fun calculateResult(player: Player) {
        members.filter { uUID -> uUID != player.uniqueId }.forEach { uUID -> result[uUID] = 1.0 }
    }

    override fun update() {
        if (gravityChangeTick <= 0L) {
            changeGravity()

            gravityChangeTick = Random.nextLong(80L, 160L)
        }

        gravityChangeTick -= 2L

        if (ventilator) {
            updateVentilator()
        }
    }

    fun changeGravity() {
        if (gravityDirection == "UP") {
            gravityDirection = "DOWN"

            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                player.removePotionEffect(PotionEffectType.JUMP_BOOST)

                player.removePotionEffect(PotionEffectType.SLOW_FALLING)

                val gravity = player.getAttribute(Attribute.GRAVITY)

                gravity?.addModifier(
                    AttributeModifier(
                        FAN_GRAVITY,
                        0.08,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }
        } else {
            gravityDirection = "UP"

            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.JUMP_BOOST,
                        -1,
                        2,
                        false,
                        false,
                        true
                    )
                )

                player.addPotionEffect(
                    PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        -1,
                        2,
                        false,
                        false,
                        true
                    )
                )

                player.getAttribute(Attribute.GRAVITY)?.removeModifier(FAN_GRAVITY)
            }
        }
    }

    fun updateVentilator() {
        if (ventilatorFrameTick <= 0) {
            for (it in ventilatorFrames[ventilatorFrame]) {
                it.key.block = it.value
            }

            for (display in ventilatorUsed.subtract(ventilatorFrames[ventilatorFrame].keys)) {
                display.block = Material.AIR.createBlockData()
            }

            if (gravityDirection == "UP") {
                ventilatorFrame++

                if (ventilatorFrame == ventilatorFrames.size) ventilatorFrame = 0
            } else {
                ventilatorFrame--

                if (ventilatorFrame == -1) ventilatorFrame = ventilatorFrames.size - 1
            }

            ventilatorFrameTick = 4L
        }

        if (gravityChangeTick % 5L == 0L) {
            val message = if (gravityDirection == "UP") {
                Component.text("Вентилятор ▲").color(NamedTextColor.BLUE)
            } else {
                Component.text("Вентилятор ▼").color(NamedTextColor.GOLD)
            }

            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                player.sendActionBar(message)
            }
        }

        ventilatorFrameTick -= 2L
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH

        if (ventilator) {
            ventilatorUsed.forEach { display -> display.remove() }
        }

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            player.activePotionEffects.forEach { player.removePotionEffect(it.type) }

            player.getAttribute(Attribute.GRAVITY)?.removeModifier(FAN_GRAVITY)
        }
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}