package ru.joutak.adhd.config.map.meta.concrete

import org.bukkit.Material
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.world.SpawnPoint

class RicochetArenaMapMeta(
    val loots: Map<Int, List<Material>>,
    val pointsToWin: Int,
    val projectileSpeed: Double,
    val maxBounces: Int,
    val lifetimeTicks: Int,
    val cooldownTicks: Int
) : MapMeta()