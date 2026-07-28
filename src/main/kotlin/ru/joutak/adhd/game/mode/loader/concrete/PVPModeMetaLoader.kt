package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.PVPModeMeta

class PVPModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val inventories = mutableMapOf<Int, List<Material>>()

        val inventoriesSection = section.getConfigurationSection("inventories")!!

        for (id in inventoriesSection.getKeys(false)) {
            val materials = inventoriesSection.getStringList(id)
                .map(Material::valueOf)

            inventories[id.toInt()] = materials
        }

        return PVPModeMeta(inventories)
    }
}