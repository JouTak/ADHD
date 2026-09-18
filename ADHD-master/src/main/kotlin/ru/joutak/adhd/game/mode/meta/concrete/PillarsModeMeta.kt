package ru.joutak.adhd.game.mode.meta.concrete

import org.bukkit.Material
import ru.joutak.adhd.game.mode.meta.ModeMeta

class PillarsModeMeta(val itemSets: Map<String, List<Material>>, val interval: Double) : ModeMeta() {
    fun getAllItems(): List<Material> {
        return itemSets.values.flatten()
    }

    fun getItemsFromSets(setNames: List<String>): List<Material> {
        return setNames.flatMap { itemSets[it] ?: emptyList() }
    }

    fun getItemsExcludingSets(bannedSetNames: List<String>): List<Material> {
        val bannedItems = getItemsFromSets(bannedSetNames).toSet()
        return getAllItems().filter { it !in bannedItems }
    }
}