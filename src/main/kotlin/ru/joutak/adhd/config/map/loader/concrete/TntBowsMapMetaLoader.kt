package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.TntBowsMapMeta
import ru.joutak.adhd.world.SpawnPoint
import kotlin.math.floor

class TntBowsMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val conveyorLines = mutableListOf<List<SpawnPoint>>()

        for (lineId in section.getKeys(false)) {
            val lineSection = section.getConfigurationSection(lineId)!!

            val coords = mutableListOf<SpawnPoint>()

            val startX = lineSection.getDouble("start.x")
            val startY = lineSection.getDouble("start.y")
            var startZ = lineSection.getDouble("start.z")
            startZ -= floor(startZ / 512) * 512

            coords.add(SpawnPoint(startX, startY, startZ, 0.0f, 0.0f))

            val endX = lineSection.getDouble("end.x")
            val endY = lineSection.getDouble("end.y")
            var endZ = lineSection.getDouble("end.z")

            endZ -= floor(endZ / 512) * 512
            coords.add(SpawnPoint(endX, endY, endZ, 0.0f, 0.0f))

            conveyorLines.add(coords)
        }

        return TntBowsMapMeta(conveyorLines)
    }

}