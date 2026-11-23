package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.HammerRunnerModeMeta

class HammerRunnerModeMetaLoader: ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val hammerMaterial: String = section.getString("hammer.material")!!
        val hammerName: String = section.getString("hammer.name")!!
        val winHits: Int = section.getInt("game.winHits")
        val tickInterval: Int = section.getInt("game.tickInterval")
        val rabbitPoints = mutableSetOf<Location>()
        return HammerRunnerModeMeta(hammerMaterial, hammerName, winHits, tickInterval, rabbitPoints)
    }
}