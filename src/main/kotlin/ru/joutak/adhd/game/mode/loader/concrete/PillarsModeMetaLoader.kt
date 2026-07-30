package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.PillarsModeMeta

class PillarsModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val items = section.getStringList("items")

        val materials = items.map { Material.valueOf(it) }

        return PillarsModeMeta(materials)
    }
}