package ru.joutak.adhd.game.mode.meta.concrete

import ru.joutak.adhd.game.mode.meta.ModeMeta

class RicochetArenaModeMeta(    val pointsToWin: Int,
                                val projectileSpeed: Double,
                                val maxBounces: Int,
                                val lifetimeTicks: Long,
                                val cooldownTicks: Int,
                                val projectileDamage: Double) : ModeMeta() {


}