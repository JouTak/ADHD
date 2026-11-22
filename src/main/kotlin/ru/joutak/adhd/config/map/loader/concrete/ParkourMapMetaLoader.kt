package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.ParkourFinish
import ru.joutak.adhd.config.map.meta.concrete.ParkourMapMeta
import kotlin.math.floor

class ParkourMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val finishSection = section.getConfigurationSection("finish")!!

        val finish = mutableSetOf<ParkourFinish>()

        for (i in finishSection.getKeys(false)) {
            val pointSection = finishSection.getConfigurationSection(i) ?: continue

            val x = pointSection.getDouble("x")
            val y = pointSection.getDouble("y")
            var z = pointSection.getDouble("z")
            z -= floor(z / 512) * 512

            finish.add(
                ParkourFinish(
                    x = x,
                    y = y,
                    z = z,
                    width = pointSection.getDouble("width"),
                    height = pointSection.getDouble("height"),
                ),
            )
        }

        val borderY = section.getDouble("borderY")

        return ParkourMapMeta(finish, borderY)
    }
}
