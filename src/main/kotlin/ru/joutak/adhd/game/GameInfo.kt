package ru.joutak.adhd.game

import ru.joutak.adhd.world.Arena
import java.util.UUID

data class GameInfo(val members: Set<UUID>, val arena: Arena)
