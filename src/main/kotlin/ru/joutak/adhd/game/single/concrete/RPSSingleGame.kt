package ru.joutak.adhd.game.single.concrete

import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.single.SingleGame
import ru.joutak.adhd.world.Arena
import java.util.UUID

class RPSSingleGame : SingleGame() {

    var state = GameState.START

    var result = false

    override fun start(
        worldName: String,
        arena: Arena,
        member: UUID,
        modeMeta: ModeMeta?
    ) {

    }

    override fun update() {

    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH
    }

    override fun summarize(): Boolean {
        return result
    }
}