package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import java.util.UUID

class RPSGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var state = GameState.START

    val result = mutableMapOf<UUID, Double>()

    var choice = ""

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

        state = GameState.RUN
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