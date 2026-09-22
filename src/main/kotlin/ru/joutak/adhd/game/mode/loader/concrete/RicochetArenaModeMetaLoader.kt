package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.RicochetArenaModeMeta

class RicochetArenaModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val pointsToWin = section.getInt("points_to_win")

        val projectileSpeed = section.getDouble("projectile.speed")

        val maxBounces = section.getInt("projectile.max_bounces")

        val lifetimeTicks = section.getLong("projectile.lifetime_ticks")

        val projectileDamage = section.getDouble("projectile.damage")

        val cooldownTicks = section.getInt("weapon.cooldown_ticks")

        return RicochetArenaModeMeta(pointsToWin, projectileSpeed, maxBounces, lifetimeTicks, cooldownTicks, projectileDamage)
    }
}