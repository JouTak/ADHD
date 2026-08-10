package ru.joutak.adhd.tournament.schedule

data class GameVariant(
    val modeName: String,
    val mapId: Int,
    val durationSeconds: Int,
    val parameters: Map<String, String> = emptyMap(),
)

data class StagePlan<T>(
    val variant: GameVariant,
    val matches: List<StageMatch<T>>,
    val bye: T?,
)

data class StageMatch<T>(
    val participants: Set<T>,
    val variant: GameVariant,
)
