package ru.joutak.adhd.tournament.schedule

import kotlin.random.Random

object TournamentSchedulePlanner {

    fun createGameSequence(
        roundCount: Int,
        variants: List<GameVariant>,
        random: Random = Random.Default,
    ): List<GameVariant> {
        require(roundCount >= 0) { "Round count must not be negative" }
        require(roundCount == 0 || variants.isNotEmpty()) { "At least one game variant is required" }

        val sequence = mutableListOf<GameVariant>()
        val modeNames = variants.map { it.modeName }.toSet()
        val usedModes = mutableSetOf<String>()

        repeat(roundCount) {
            val previous = sequence.lastOrNull()
            var availableModes = modeNames - usedModes

            if (availableModes.isEmpty()) {
                availableModes = (modeNames - setOfNotNull(previous?.modeName)).ifEmpty { modeNames }
                usedModes.clear()
            }

            val availableVariants = variants.filter { it.modeName in availableModes }
            val minimumRepetitions = availableVariants.minOf { repetitionCount(previous, it) }
            val candidates = availableVariants.filter { repetitionCount(previous, it) == minimumRepetitions }
            val selected = candidates.random(random)
            sequence += selected
            usedModes += selected.modeName
        }

        return sequence
    }

    fun <T> createStage(
        variant: GameVariant,
        participants: List<T>,
        previousPairs: Set<Set<T>>,
        random: Random = Random.Default,
    ): StagePlan<T> {
        val uniqueParticipants = participants.distinct().shuffled(random)
        val pairing = findWithoutRepeatedPairs(uniqueParticipants, previousPairs, random)
            ?: fallbackPairing(uniqueParticipants)

        return StagePlan(
            variant = variant,
            matches = pairing.first.map { StageMatch(it, variant) },
            bye = pairing.second,
        )
    }

    private fun repetitionCount(previous: GameVariant?, candidate: GameVariant): Int {
        if (previous == null) return 0

        return (if (previous == candidate) 1 else 0) +
            (if (previous.mapId == candidate.mapId) 1 else 0)
    }

    private fun <T> findWithoutRepeatedPairs(
        participants: List<T>,
        previousPairs: Set<Set<T>>,
        random: Random,
    ): Pair<List<Set<T>>, T?>? {
        if (participants.size % 2 == 0) {
            return findEvenPairing(participants, previousPairs, random)?.let { it to null }
        }

        for (byeIndex in participants.indices.shuffled(random)) {
            val bye = participants[byeIndex]
            val remaining = participants.filterIndexed { index, _ -> index != byeIndex }
            val pairs = findEvenPairing(remaining, previousPairs, random) ?: continue
            return pairs to bye
        }

        return null
    }

    private fun <T> findEvenPairing(
        participants: List<T>,
        previousPairs: Set<Set<T>>,
        random: Random,
    ): List<Set<T>>? {
        if (participants.isEmpty()) return emptyList()

        val first = participants.first()
        val opponents = participants.drop(1).shuffled(random)

        for (opponent in opponents) {
            val pair = setOf(first, opponent)
            if (pair in previousPairs) continue

            val remaining = participants.drop(1).filter { it != opponent }
            val rest = findEvenPairing(remaining, previousPairs, random) ?: continue
            return listOf(pair) + rest
        }

        return null
    }

    private fun <T> fallbackPairing(participants: List<T>): Pair<List<Set<T>>, T?> {
        val pairedCount = participants.size / 2 * 2
        val pairs = participants.take(pairedCount).chunked(2).map { it.toSet() }
        return pairs to participants.getOrNull(pairedCount)
    }
}
