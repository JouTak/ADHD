package ru.joutak.adhd.game.concrete

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Interaction
import ru.joutak.adhd.config.map.meta.concrete.ParkourMapMeta
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import java.util.UUID

class ParkourGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var state = GameState.START

    var result = mutableMapOf<UUID, Double>()

    val finishes = mutableSetOf<Interaction>()

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        val meta = arena.metas["parkour"] as? ParkourMapMeta ?: error("Arena must have parkour meta for this mode to operate...")

        val world = Bukkit.getWorld(worldName)!!

        for (p in meta.finish) {
            val loc = Location(world, p.x, p.y, p.z)

            val interaction = world.spawn(loc, Interaction::class.java) {
                it.interactionWidth = 1f
                it.interactionHeight = 1f
            }

            finishes.add(interaction)
        }
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