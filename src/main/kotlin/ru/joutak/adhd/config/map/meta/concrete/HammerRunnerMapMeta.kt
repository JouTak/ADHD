package ru.joutak.adhd.config.map.meta.concrete

import ru.joutak.adhd.config.map.meta.MapMeta

class HammerRunnerMapMeta (val offsets: MutableMap<Int, MutableMap<String, Double>> = mutableMapOf()) : MapMeta()