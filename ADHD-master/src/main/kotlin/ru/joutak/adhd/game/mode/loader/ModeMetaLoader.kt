package ru.joutak.adhd.game.mode.loader

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.meta.ModeMeta

interface ModeMetaLoader {
    fun load(section: ConfigurationSection): ModeMeta
}