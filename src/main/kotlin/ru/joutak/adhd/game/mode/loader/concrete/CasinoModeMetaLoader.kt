package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.CasinoModeMeta
import ru.joutak.minigames.config.ConfigKey

class CasinoModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val initialBalance = section.getInt("initialBalance")
        val goalBalance = section.getInt("goalBalance")

        return CasinoModeMeta(initialBalance, goalBalance)
    }
}