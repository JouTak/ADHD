package ru.joutak.adhd.game.single

import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import java.util.UUID

abstract class SingleGame {
    abstract fun start(worldName: String, arena: Arena, member: UUID, modeMeta: ModeMeta?)

    abstract fun update()

    abstract fun getGameState(): GameState

    abstract fun finish()

    abstract fun summarize(): Boolean
}