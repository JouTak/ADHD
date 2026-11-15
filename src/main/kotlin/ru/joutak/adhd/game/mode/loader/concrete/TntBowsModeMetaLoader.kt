package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.TntBowsModeMeta

class TntBowsModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val fallY = section.getDouble("tnt.fall_y")

        val movementSpeedTnt = section.getDouble("tnt.movement_speed_tnt")

        val spawnSpeedTnt = section.getLong("tnt.spawn_speed_tnt")

        return TntBowsModeMeta(fallY, spawnSpeedTnt, movementSpeedTnt)
    }
}