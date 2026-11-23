package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.HammerRunnerMapMeta
import ru.joutak.adhd.world.SpawnPoint

class HammerRunnerMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val temp: MutableSet<SpawnPoint> = mutableSetOf()
        var i: Int = 0
        while (section.contains("spawns.${i}")){
            val x: Double = section.getDouble("offsets.${i}.x", 0.0)
            val y: Double = section.getDouble("offsets.${i}.y", 0.0)
            val z: Double = section.getDouble("offsets.${i}.z", 0.0)
            val yaw: Double = section.getDouble("offsets.${i}.yaw", 0.0)
            val pitch: Double = section.getDouble("offsets.${i}.pitch", 0.0)
            temp.add(SpawnPoint(x, y, z, yaw.toFloat(), pitch.toFloat()))
            i++
        }
        return HammerRunnerMapMeta(spawns = temp)
    }
}