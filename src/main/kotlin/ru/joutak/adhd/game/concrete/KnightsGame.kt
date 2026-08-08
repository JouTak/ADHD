package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
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

class KnightsGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var state = GameState.START

    var result = mutableMapOf<UUID, Double>()

    var lSpawn: SpawnPoint? = null

    var wMaterial = Material.DIAMOND_SPEAR

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

        val world = Bukkit.getWorld(worldName)!!

        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)

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
        horse.isInvulnerable = true

        horse.isTamed = true
        horse.owner = player
        horse.inventory.saddle = ItemStack(Material.SADDLE)

        horse.jumpStrength = 0.7

        horse.getAttribute(Attribute.MOVEMENT_SPEED)?.baseValue = horseSpeed

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

        player.inventory.setItem(0, ItemStack(wMaterial, 1))

        if (wMaterial == Material.CROSSBOW || wMaterial == Material.BOW) {
            player.inventory.setItem(8, ItemStack(Material.ARROW, 64))
        }

        player.inventory.heldItemSlot = 0
    }

    fun calculateResult(player: Player) {
        members.filter { uUID -> uUID != player.uniqueId }.forEach { uUID -> result[uUID] = 1.0 }
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