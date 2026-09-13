package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.SnipersModeMeta

class SnipersModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val jumpBoost = section.getInt("gravity.up.jumpBoost", 8)

        val fallBoost = section.getInt("gravity.up.fallBoost", 3)

        return SnipersModeMeta(jumpBoost, fallBoost)
    }
}