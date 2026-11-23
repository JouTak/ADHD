package ru.joutak.adhd.config.map.meta.concrete

import ru.joutak.adhd.config.map.meta.MapMeta
import ru.joutak.adhd.world.SpawnPoint

class HammerRunnerMapMeta (val spawns: MutableSet<SpawnPoint> = mutableSetOf()) : MapMeta()