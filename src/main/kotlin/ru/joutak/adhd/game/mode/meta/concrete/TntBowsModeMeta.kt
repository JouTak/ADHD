package ru.joutak.adhd.game.mode.meta.concrete

import ru.joutak.adhd.game.mode.meta.ModeMeta

class TntBowsModeMeta(
    val fallY: Double,
    val explosionRadius: Float,
    val spawnSpeedTnt: Double,
    val movementSpeedTnt: Double
) : ModeMeta() {}