package ru.joutak.adhd.game.concrete

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ChargedProjectiles
import org.bukkit.Bukkit
import org.bukkit.FireworkEffect
import org.bukkit.GameMode
import org.bukkit.GameRules
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.FireworkMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.world.SpawnPoint
import java.util.UUID

class SnipersGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var lSpawn: SpawnPoint? = null

    var state = GameState.START

    var result = mutableMapOf<UUID, Double>()

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