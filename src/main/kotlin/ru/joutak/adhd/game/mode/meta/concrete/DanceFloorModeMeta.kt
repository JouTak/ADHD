package ru.joutak.adhd.game.mode.meta.concrete

import ru.joutak.adhd.game.mode.meta.ModeMeta


class DanceFloorModeMeta (val width: Int, val length: Int, val interval_ticks: Int, val green_chacne: Double, val win_points: Int, val green_points: Int, val red_penalty: Int, val neutral: String?, val red: String?, val green: String?) : ModeMeta()