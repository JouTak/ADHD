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
                val typeMap = mutableMapOf<String, MutableList<Material>>()
                val setNode = setsSection.getConfigurationSection(setName)

                if (setNode == null) {
                    typeMap["items"] = parseMaterials(setsSection.getStringList(setName), setName, "items")
                } else {
                    for (typeName in setNode.getKeys(false)) {
                        typeMap[typeName] = parseMaterials(setNode.getStringList(typeName), setName, typeName)
                    }
                }

                sets[setName] = typeMap
            }
        }

        return PillarsModeMeta(sets, interval)
    }

    private fun parseMaterials(rawList: List<String>, setName: String, typeName: String): MutableList<Material> {
        return rawList.mapNotNull { raw ->
            val mat = Material.matchMaterial(raw)
            if (mat == null) {
                plugin.logger.warning("Неизвестный Material: '$raw' в $setName.$typeName")
            }
            mat
        }.toMutableList()
    }
}
