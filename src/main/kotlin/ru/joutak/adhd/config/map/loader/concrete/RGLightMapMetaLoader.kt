package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.RGLightMapMeta
import ru.joutak.adhd.world.SpawnPoint

class RGLightMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val wardenSpawnPoint: SpawnPoint? = readSpawnPoint(section, "wardenLocation")

        val finishX = if (section.contains("finishX")) section.getDouble("finishX") else null
        val finishZ = if (section.contains("finishZ")) section.getDouble("finishZ") else null

        return RGLightMapMeta(
            wardenSpawnPoint = wardenSpawnPoint,
            finishX = finishX,
            finishZ = finishZ
        )

    }

    private fun readSpawnPoint(parent: ConfigurationSection, path: String): SpawnPoint? {
        val sec = parent.getConfigurationSection(path) ?: return null
        return SpawnPoint(
            x = sec.getDouble("x"),
            y = sec.getDouble("y"),
            z = sec.getDouble("z"),
            yaw = sec.getDouble("yaw", 0.0).toFloat(),
            pitch = sec.getDouble("pitch", 0.0).toFloat()
        )
    }
}