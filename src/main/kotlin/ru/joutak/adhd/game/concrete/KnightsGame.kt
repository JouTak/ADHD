package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.KnightsModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID
import kotlin.random.Random

class KnightsGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var state = GameState.START

    var result = mutableMapOf<UUID, Double>()

    var lSpawn: SpawnPoint? = null

    var wMaterial = Material.DIAMOND_SPEAR

    val rEnchantments = mutableSetOf<Pair<Enchantment, Int>>()

    var horseSpeed = 0.16875

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        val meta = modeMeta as? KnightsModeMeta

        if (meta != null) {
            horseSpeed = meta.horseSpeed

            wMaterial = meta.weapons.random()
        }

        generateRandomEnchantments()

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            sitOnHorse(player)

            restoreStats(player)

            giveLayout(player)
        }

        state = GameState.RUN
    }

    override fun update() {

    }

    fun sitOnHorse(player: Player) {
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

        val loc = Location(world, chosen.x, chosen.y, chosen.z, chosen.yaw, chosen.pitch)

        player.teleport(loc)

        val horse = world.spawn(loc, Horse::class.java)

        horse.setAdult()
        horse.isInvulnerable = false

        horse.isTamed = true
        horse.owner = player
        horse.inventory.saddle = ItemStack(Material.SADDLE)

        horse.jumpStrength = 0.7

        horse.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = horseSpeed
        horse.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 40.0

        horse.health = 40.0

        horse.addPassenger(player)
    }

    fun restoreStats(player: Player) {
        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
    }

    fun giveLayout(player: Player) {
        player.inventory.clear()

        val weapon = ItemStack(wMaterial, 1)

        val wMeta = weapon.itemMeta

        if (wMaterial == Material.CROSSBOW) {
            wMeta.addEnchant(Enchantment.QUICK_CHARGE, 3, true)
        }

        rEnchantments.forEach { (e, l) -> wMeta.addEnchant(e, l, true) }

        weapon.itemMeta = wMeta

        player.inventory.setItem(0, weapon)

        if (wMaterial == Material.CROSSBOW || wMaterial == Material.BOW) {
            player.inventory.setItem(8, ItemStack(Material.ARROW, 64))
        }

        player.inventory.heldItemSlot = 0
    }

    fun generateRandomEnchantments() {
        val eVariants: MutableSet<Pair<Enchantment, Int>> = mutableSetOf()

        when (wMaterial) {
            Material.BOW -> {
                eVariants.add(Pair(Enchantment.POWER, 5))
                eVariants.add(Pair(Enchantment.PUNCH, 2))
                eVariants.add(Pair(Enchantment.FLAME, 1))
            }
            Material.CROSSBOW -> {
                eVariants.add(Pair(Enchantment.MULTISHOT, 1))
                eVariants.add(Pair(Enchantment.PIERCING, 4))
            }
            else -> {}
        }

        if (wMaterial.name.lowercase().contains("spear")) {
            eVariants.add(Pair(Enchantment.SHARPNESS, 5))
            eVariants.add(Pair(Enchantment.FIRE_ASPECT, 2))
            eVariants.add(Pair(Enchantment.KNOCKBACK, 2))
            eVariants.add(Pair(Enchantment.LUNGE, 3))
        }

        val chosen = mutableSetOf<Pair<Enchantment, Int>>()

        for ((e, l) in eVariants.shuffled().take(Random.nextInt(1, 3))) {
            chosen.add(Pair(e, Random.nextInt(1, l + 1)))
        }

        rEnchantments.addAll(chosen)
    }

    fun calculateResult(player: Player) {
        members.filter { uUID -> uUID != player.uniqueId }.forEach { uUID -> result[uUID] = 1.0 }
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            player.leaveVehicle()
        }
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}