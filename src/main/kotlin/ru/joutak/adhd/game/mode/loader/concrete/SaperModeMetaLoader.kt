package ru.joutak.adhd.game.mode.loader.concrete

import org.bukkit.configuration.ConfigurationSection
import ru.joutak.adhd.game.mode.loader.ModeMetaLoader
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.SaperModeMeta

class SaperModeMetaLoader : ModeMetaLoader {
    override fun load(section: ConfigurationSection): ModeMeta {
        val fieldWidth: Int = section.getInt("field.width", 2)
        val fieldHeight: Int = section.getInt("field.height", 2)
        val mines: Int = section.getInt("field.mines", 1)
        val closeMaterial: String = section.getString("materials.closed")!!
        val freeMaterial: String = section.getString("materials.free")!!
        val digitMaterial: String = section.getString("materials.digit")!!
        val mineMaterial: String = section.getString("materials.mine")!!
        val colors: MutableList<String> = mutableListOf()
        for(i: Int in 1..8){
            colors.add(section.getString("colors.${i}")!!)
        }
        return SaperModeMeta(fieldWidth, fieldHeight, mines, closeMaterial, freeMaterial, digitMaterial, mineMaterial, colors)
    }
}