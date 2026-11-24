package ru.joutak.adhd.game.concrete

import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.world.Arena
import java.util.*

class SaperGame : Game() {
    lateinit var arena: Arena
    lateinit var worldName: String
    lateinit var members: Set<UUID>

    var state = GameState.START
    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        TODO("Not yet implemented")
    }

    override fun summarize(): Map<UUID, Double> {
        TODO("Not yet implemented")
    }
}