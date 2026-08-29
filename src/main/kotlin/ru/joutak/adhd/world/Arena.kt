package ru.joutak.adhd.world

import ru.joutak.adhd.config.map.meta.MapMeta

data class Arena(val spawnPoints: List<SpawnPoint>, val metas: Map<String, MapMeta>)