package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.KnightsModeMeta

class KnightsModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val horseSpeed = section.getDouble("horse.speed", 0.16875)

        val weapons = section.getStringList("weapons").map(Material::valueOf)

        return KnightsModeMeta(horseSpeed, weapons)
    }
}