package ru.joutak.adhd.game.mode.meta.concrete

import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.SpawnPoint

class HammerRunnerModeMeta (val hammerMaterial: String, val hammerName: String, val winHits: Int, val tickInterval: Int, val rabbitMessage: String, val rabbitPoints: MutableSet<SpawnPoint>) : ModeMeta()