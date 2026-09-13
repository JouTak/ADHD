package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.MemoryMapMeta
import ru.joutak.adhd.world.SpawnPoint
import kotlin.math.floor

class MemoryMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val pointsSection = section.getConfigurationSection("points")!!

        val points = mutableListOf<SpawnPoint>()

        for (i in pointsSection.getKeys(false)) {
            val pointSection = pointsSection.getConfigurationSection(i) ?: continue

            val x = pointSection.getDouble("x")
            val y = pointSection.getDouble("y")
            var z = pointSection.getDouble("z")
            z -= floor(z / 512) * 512

            points.add(SpawnPoint(x, y, z, 0.0f, 0.0f))
        }

        return MemoryMapMeta(points)
    }
}