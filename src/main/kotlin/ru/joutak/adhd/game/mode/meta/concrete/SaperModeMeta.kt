package ru.joutak.adhd.game.mode.meta.concrete

import ru.joutak.adhd.game.mode.meta.ModeMeta

class SaperModeMeta(val fieldWidth: Int, val fieldHeight: Int, val mines: Int, val closeMaterial: String, val freeMaterial: String, val digitMaterial: String, val mineMaterial: String, val color: MutableList<String>) : ModeMeta()