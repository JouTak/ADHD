package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.PillarsModeMeta
import ru.joutak.minigames.MiniGamesAPI.plugin

class PillarsModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val interval = section.getDouble("interval")

        val items = section.getStringList("items")

        val setsSection = section.getConfigurationSection("item_sets")
        val sets: MutableMap<String, MutableMap<String, MutableList<Material>>> = mutableMapOf()

        if (setsSection != null) {
            for (setName in setsSection.getKeys(false)) {
//                val itemNames = setsSection.getStringList(setName)
//                val materials = itemNames.mapNotNull { itemName ->
//                    try {
//                        Material.valueOf(itemName)
//                    } catch (e: IllegalArgumentException) {
//                        ADHDPlugin.instance.logger.warning("Предмет '$itemName' в наборе '$setName' не найден")
//                        null
//                    }
//                }
//                itemSets[setName] = materials

                val setNode = setsSection.getConfigurationSection(setName) ?: continue
                val typeMap = mutableMapOf<String, MutableList<Material>>()

                for (typeName in setNode.getKeys(false)) {
                    val rawList: List<String> = setNode.getStringList(typeName)

                    val materials = rawList.mapNotNull { raw ->
                        val mat = Material.matchMaterial(raw)
                        if (mat == null) {
                            plugin.logger.warning("Неизвестный Material: '$raw' в $setName.$typeName")
                        }
                        mat
                    }.toMutableList()

                    typeMap[typeName] = materials
                }

                sets[setName] = typeMap
            }
        }

        return PillarsModeMeta(sets, interval)
    }
}