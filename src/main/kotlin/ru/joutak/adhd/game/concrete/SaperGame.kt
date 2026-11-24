package ru.joutak.adhd.game.concrete

import org.bukkit.entity.Player
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.SaperModeMeta
import ru.joutak.adhd.world.Arena
import java.util.*

class SaperGame : Game() {
    lateinit var arena: Arena
    lateinit var worldName: String
    lateinit var members: Set<UUID>

    var state = GameState.START

    private val result: Map<UUID, Double> = mutableMapOf()

    private var fieldWidth: Int = 0
    private var fieldHeight: Int = 0
    private var mines: Int = 0
    private var closedMaterial: String = ""
    private var freeMaterial: String = ""
    private var digitMaterial: String = ""
    private var mineMaterial: String = ""
    private val colors: MutableList<String> = mutableListOf()

    private val field: MutableList<MutableList<Int>> = mutableListOf()
    lateinit var player: Player

    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.arena = arena
        this.worldName = worldName
        this.members = members

        val meta = modeMeta as? SaperModeMeta

        state = GameState.RUN
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        TODO("Not yet implemented")
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }

    override fun update() {
        TODO("Not yet implemented")
    }
}