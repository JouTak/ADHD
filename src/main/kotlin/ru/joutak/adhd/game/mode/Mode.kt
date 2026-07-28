package ru.joutak.adhd.game.mode

import ru.joutak.adhd.game.mode.meta.ModeMeta

data class Mode(val duration: Int, val maps: List<Int>, val meta: ModeMeta?, val displayName: String, val description: String)
