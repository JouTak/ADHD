package ru.joutak.adhd.game

import ru.joutak.adhd.world.Arena
import java.util.UUID

abstract class Game {
    abstract fun start(mode: Mode, assignedMembers: Map<UUID, Arena>, worldName: String)

    abstract fun update()

    abstract fun finish(): Map<UUID, Double>

    abstract fun remove(uuid: UUID)
}