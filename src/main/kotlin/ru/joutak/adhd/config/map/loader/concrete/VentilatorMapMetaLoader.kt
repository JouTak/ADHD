package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.VentilatorMapMeta
import ru.joutak.adhd.world.SpawnPoint
import kotlin.math.floor

class VentilatorMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val frames = mutableListOf<SpawnPoint>()

        val placementX = section.getDouble("placement.x")
        val placementY = section.getDouble("placement.y")
        var placementZ = section.getDouble("placement.z")
        placementZ -= floor(placementZ / 512) * 512

        val placement = SpawnPoint(placementX, placementY, placementZ, 0.0f, 0.0f)

        val framesSection = section.getConfigurationSection("frames")!!

        for (fId in framesSection.getKeys(false)) {
            val frame = framesSection.getConfigurationSection(fId)!!

            val centerX = frame.getDouble("center.x")
            val centerY = frame.getDouble("center.y")
            var centerZ = frame.getDouble("center.z")
            centerZ -= floor(centerZ / 512) * 512

            frames.add(SpawnPoint(centerX, centerY, centerZ, 0.0f, 0.0f))
        }

        return VentilatorMapMeta(frames, placement)
    }
}