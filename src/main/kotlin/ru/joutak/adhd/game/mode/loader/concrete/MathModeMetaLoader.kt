package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.MathModeMeta

class MathModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val quantity = section.getInt("quantity")
        val interval = section.getDouble("interval")
        val minimum = section.getInt("minimum")

        return MathModeMeta(quantity, interval, minimum)
    }
}