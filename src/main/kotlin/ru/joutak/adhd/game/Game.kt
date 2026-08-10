package ru.joutak.adhd.game

import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import java.util.UUID

abstract class Game {
    abstract fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?,
        variantParameters: Map<String, String>,
    )

    abstract fun update()

    abstract fun getGameState(): GameState

    abstract fun finish()

    abstract fun summarize(): Map<UUID, Double>
}
