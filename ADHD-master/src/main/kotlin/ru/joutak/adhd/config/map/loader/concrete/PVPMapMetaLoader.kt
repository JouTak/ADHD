package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.PVPMapMeta

class PVPMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val loots = mutableMapOf<Int, List<Material>>()

        val lootsSection = section.getConfigurationSection("loots")!!

        for (id in lootsSection.getKeys(false)) {
            val materials = lootsSection.getStringList(id)
                .map(Material::valueOf)

            loots[id.toInt()] = materials
        }

        return PVPMapMeta(loots)
    }
}