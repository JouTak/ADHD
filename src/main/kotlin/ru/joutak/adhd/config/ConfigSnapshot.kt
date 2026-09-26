package ru.joutak.adhd.config

import ru.joutak.adhd.game.mode.Mode
import ru.joutak.adhd.world.ConfigMap
import ru.joutak.adhd.world.SpawnPoint

data class ConfigSnapshot(
    val maxPlayers: Int,
    val pointsGoal: Double,
    val templateWorldName: String,
    val lobbyWorld: String,
    val ceremonyEnabled: Boolean,
    val ceremonyDuration: Int,
    val ceremonySpawnPoint: SpawnPoint,
    val configMaps: Map<Int, ConfigMap>,
    val modes: Map<String, Mode>,
    val singleModeNames: Set<String>
)