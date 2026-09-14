package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.ParkourMapMeta
import ru.joutak.adhd.world.SpawnPoint
import kotlin.math.floor

class ParkourMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val finishSection = section.getConfigurationSection("finish")!!

        val finish = mutableSetOf<SpawnPoint>()

        for (i in finishSection.getKeys(false)) {
            val pointSection = finishSection.getConfigurationSection(i) ?: continue

            val x = pointSection.getDouble("x")
            val y = pointSection.getDouble("y")
            var z = pointSection.getDouble("z")
            z -= floor(z / 512) * 512

            finish.add(SpawnPoint(x, y, z, 0.0f, 0.0f))
        }

        return ParkourMapMeta(finish)
    }
}