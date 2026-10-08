package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.HammerRunnerModeMeta
import ru.joutak.adhd.world.SpawnPoint

class HammerRunnerModeMetaLoader: ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val hammerMaterial: String = section.getString("hammer.material")!!
        val hammerName: String = section.getString("hammer.name")!!
        val winHits: Int = section.getInt("game.winHits")
        val tickInterval: Int = section.getInt("game.tickInterval")
        val rabbitMessage: String = section.getString("game.message")!!
        val rabbitPoints = mutableSetOf<SpawnPoint>()
        var i: Int = 0
        while(section.contains("$i")){
            var x: Double = if(section.contains("$i.x")) section.getDouble("$i.x") else 0.0
            var y: Double = if(section.contains("$i.y")) section.getDouble("$i.y") else 0.0
            var z: Double = if(section.contains("$i.z")) section.getDouble("$i.z") else 0.0
            var yaw: Double = if(section.contains("$i.yaw")) section.getDouble("$i.yaw") else 0.0
            var pitch: Double = if(section.contains("$i.pitch")) section.getDouble("$i.pitch") else 0.0
            rabbitPoints.add(SpawnPoint(x, y, z, yaw.toFloat(), pitch.toFloat()))
            i++
        }
        return HammerRunnerModeMeta(hammerMaterial, hammerName, winHits, tickInterval, rabbitMessage, rabbitPoints)
    }
}
