package ru.joutak.adhd.config.map.meta.concrete

import ru.joutak.adhd.config.map.meta.MapMeta

data class ParkourFinish(
    val x: Double,
    val y: Double,
    val z: Double,
    val width: Double,
    val height: Double,
)

class ParkourMapMeta(val finish: Set<ParkourFinish>, val borderY: Double) : MapMeta()
