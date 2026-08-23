package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.PillarsModeMeta

class PillarsModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val interval = section.getDouble("interval")

        val setsSection = section.getConfigurationSection("item_sets")
        val itemSets = mutableMapOf<String, List<Material>>()

        if (setsSection != null) {
            for (setName in setsSection.getKeys(false)) {
                val itemNames = setsSection.getStringList(setName)
                val materials = itemNames.mapNotNull { itemName ->
                    try {
                        Material.valueOf(itemName)
                    } catch (e: IllegalArgumentException) {
                        ADHDPlugin.instance.logger.warning("Предмет '$itemName' в наборе '$setName' не найден")
                        null
                    }
                }
                itemSets[setName] = materials
            }
        }

        return PillarsModeMeta(itemSets, interval)
    }
}