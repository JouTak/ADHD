package ru.joutak.adhd.config.map.meta.concrete

import ru.joutak.adhd.config.map.meta.MapMeta

class RicochetArenaMapMeta(
    val pointsToWin: Int,
    val projectileSpeed: Double,
    val maxBounces: Int,
    val lifetimeTicks: Long,
    val cooldownTicks: Int,
    val projectileDamage: Double
) : MapMeta()