package ru.joutak.adhd.world

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.ChunkPos

data class RegionData(val regionId: Int, val chunks: Map<ChunkPos, CompoundTag>)