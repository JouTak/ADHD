package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.DanceFloorModeMeta

class DanceFloorModeMetaLoader: ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val width = section.getInt("rect.width")
        val length = section.getInt("rect.length")

        val interval_ticks = section.getInt("generation.interval_ticks")
        val green_chance = section.getDouble("generation.green_chance")

        val win_points = section.getInt("scoring.win_points")
        val green_points = section.getInt("scoring.green_points")
        val red_penalty = section.getInt("scoring.red_penalty")

        val neutral = section.getString("materials.neutral")
        val green = section.getString("materials.green")
        val red = section.getString("materials.red")

        return DanceFloorModeMeta(width, length, interval_ticks, green_chance, win_points, green_points, red_penalty, neutral, red, green)
    }
}