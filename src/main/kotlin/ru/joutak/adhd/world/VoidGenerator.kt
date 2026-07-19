package ru.joutak.adhd.world

import org.bukkit.generator.ChunkGenerator

class VoidGenerator : ChunkGenerator() {

    override fun shouldGenerateNoise(): Boolean {
        return false
    }

    override fun shouldGenerateSurface(): Boolean {
        return false
    }

    override fun shouldGenerateBedrock(): Boolean {
        return false
    }

    override fun shouldGenerateCaves(): Boolean {
        return false
    }

    override fun shouldGenerateDecorations(): Boolean {
        return false
    }

    override fun shouldGenerateMobs(): Boolean {
        return false
    }

    override fun shouldGenerateStructures(): Boolean {
        return false
    }
}