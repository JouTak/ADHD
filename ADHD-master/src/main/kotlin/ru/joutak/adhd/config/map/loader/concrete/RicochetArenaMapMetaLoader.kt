package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.RicochetArenaMapMeta

class RicochetArenaMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val pointsToWin = section.getInt("points_to_win")

        val projectileSpeed = section.getDouble("projectile.speed")

        val maxBounces = section.getInt("projectile.max_bounces")

        val lifetimeTicks = section.getLong("projectile.lifetime_ticks")

        val projectileDamage = section.getDouble("projectile.damage")

        val cooldownTicks = section.getInt("weapon.cooldown_ticks")

        return RicochetArenaMapMeta(
            pointsToWin = pointsToWin,
            projectileSpeed = projectileSpeed,
            maxBounces = maxBounces,
            lifetimeTicks = lifetimeTicks,
            cooldownTicks = cooldownTicks,
            projectileDamage = projectileDamage
        )
    }
}