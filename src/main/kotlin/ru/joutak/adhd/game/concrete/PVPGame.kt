package ru.joutak.adhd.game.concrete

import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.Mode
import ru.joutak.adhd.world.Arena
import java.util.UUID

class PVPGame : Game() {
    override fun start(
        mode: Mode,
        assignedMembers: Map<UUID, Arena>,
        worldName: String
    ) {

    }

    override fun update() {

    }

    override fun finish(): Map<UUID, Double> {
        return emptyMap()
    }
}