package ru.joutak.adhd.config.map.loader

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.meta.MapMeta

interface MapMetaLoader {
    fun load(section: ConfigurationSection): MapMeta
}