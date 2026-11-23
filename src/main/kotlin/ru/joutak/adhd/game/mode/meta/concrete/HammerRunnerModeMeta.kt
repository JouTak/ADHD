package ru.joutak.adhd.game.mode.meta.concrete

import org.bukkit.Location
import ru.joutak.adhd.game.mode.meta.ModeMeta

class HammerRunnerModeMeta (val hammerMaterial: String, val hammerName: String, val winHits: Int, val tickInterval: Int, val rabbitPoints: MutableSet<Location>) : ModeMeta()