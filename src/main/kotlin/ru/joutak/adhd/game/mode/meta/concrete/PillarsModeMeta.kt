package ru.joutak.adhd.game.mode.meta.concrete

import org.bukkit.Material
import ru.joutak.adhd.game.mode.meta.ModeMeta

class PillarsModeMeta(val itemSets: MutableMap<String, MutableMap<String, MutableList<Material>>>, val interval: Double) : ModeMeta() {
    fun getAllSets(): MutableMap<String, MutableMap<String, MutableList<Material>>> {
        return itemSets
    }

    fun getSetsExcluding(bannedSets: List<String>): Map<String, MutableMap<String, MutableList<Material>>> {
        return getAllSets().filter { it.key !in bannedSets }
    }
}
