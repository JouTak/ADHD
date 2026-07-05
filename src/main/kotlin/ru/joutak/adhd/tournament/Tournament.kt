package ru.joutak.adhd.tournament

import java.util.*

class Tournament(val participants: MutableList<UUID>) {

    lateinit var worldName: String

    fun start(worldName: String) {
        this.worldName = worldName
    }
}