package ru.joutak.adhd.config.map.loader.concrete

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.config.map.loader.MapMetaLoader
import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.config.map.meta.concrete.HammerRunnerMapMeta

class HammerRunnerMapMetaLoader : MapMetaLoader {
    override fun load(section: ConfigurationSection): MapMeta {
        val temp: MutableMap<Int, MutableMap<String, Double>> = mutableMapOf()
        var i: Int = 0
        while (section.contains("offsets.${i}")){
            val x: Double = section.getDouble("offsets.${i}.x", 0.0)
            val y: Double = section.getDouble("offsets.${i}.y", 0.0)
            val z: Double = section.getDouble("offsets.${i}.z", 0.0)
            val yaw: Double = section.getDouble("offsets.${i}.yaw", 0.0)
            val pitch: Double = section.getDouble("offsets.${i}.pitch", 0.0)
            temp.put(i, mutableMapOf("x" to x, "y" to y, "z" to z, "yaw" to yaw, "pitch" to pitch))
            i++
        }
        return HammerRunnerMapMeta(offsets = temp)
    }
}