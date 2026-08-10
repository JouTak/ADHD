package ru.joutak.adhd.tournament

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.KnightsGame
import ru.joutak.adhd.game.concrete.PVPGame
import ru.joutak.adhd.game.concrete.PillarsGame
import ru.joutak.adhd.game.concrete.SnipersGame
import ru.joutak.adhd.ui.GameScoreboardManager
import ru.joutak.adhd.ui.TimeBossBar
import ru.joutak.adhd.world.Arena
import ru.joutak.adhd.tournament.schedule.GameVariant
import ru.joutak.adhd.tournament.schedule.TournamentSchedulePlanner
import java.util.*

class Tournament(
    val participants: MutableList<UUID>,
    val gameSequence: List<GameVariant>,
) {

    lateinit var worldName: String

    lateinit var arenas: Map<Int, List<Arena>>

    var status = TournamentStatus.GENERATE

    var currentTick = 0L

    var round = 0

    var generated = false

    val games = mutableSetOf<Game>()

    val playerGames = mutableMapOf<UUID, Game>()

    val results = mutableMapOf<UUID, Double>()

    val announced = mutableListOf<Game>()

    val timeBossBar = TimeBossBar()

    val gameScoreboardManager = GameScoreboardManager(this)

    val fakeAnnounced = mutableMapOf<UUID, Boolean>()

    private var previousPairs = emptySet<Set<UUID>>()

    val ticker = object : BukkitRunnable() {
        override fun run() {
            tick()
        }
    }

    fun start(worldName: String, arenas: Map<Int, List<Arena>>) {
        this.worldName = worldName
        this.arenas = arenas

        status = TournamentStatus.START

        TournamentManager.removePrepareAnnounce(this)

        timeBossBar.load(this)

        for (uuid in participants) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            timeBossBar.add(player)

            gameScoreboardManager.add(player)

            results[uuid] = 0.0
        }

        ticker.runTaskTimer(ADHDPlugin.instance, 2L, 2L)

        ADHDPlugin.instance.logger.info("Начат турнир $this")
    }

    fun tick() {
        when (status) {
            TournamentStatus.GENERATE -> status = TournamentStatus.START
            TournamentStatus.START -> status = TournamentStatus.PREPARE
            TournamentStatus.PREPARE -> {
                if (round == gameSequence.size) {
                    prepareCeremony()

                    return
                }

                games.clear()

                playerGames.clear()

                announced.clear()

                currentTick = 0L

                resetGameRules()

                val variant = gameSequence[round]
                val modeName = variant.modeName

                val gArenas = arenas[round]!!

                val stagePlan = TournamentSchedulePlanner.createStage(
                    variant = variant,
                    participants = participants,
                    previousPairs = previousPairs,
                )
                previousPairs = stagePlan.matches.map { it.participants }.toSet()

                val assignedMembers = stagePlan.matches.mapIndexed { index, match ->
                    match.participants to gArenas[index]
                }

                stagePlan.bye?.let { Bukkit.getPlayer(it)?.sendMessage(
                    Component.text("Игроков не хватает. Вы пропускаете эту игру...").color(NamedTextColor.YELLOW))
                }

                val description = Component.text("[").color(NamedTextColor.GRAY)
                    .append(Component.text(ADHDConfig.modes[modeName]!!.displayName).color(NamedTextColor.GOLD))
                    .append(Component.text("] ").color(NamedTextColor.GRAY))
                    .append(Component.text(ADHDConfig.modes[modeName]!!.description).color(NamedTextColor.WHITE))

                for ((members, arena) in assignedMembers) {
                    val game = when (modeName) {
                        "PVP" -> PVPGame()
                        "Pillars" -> PillarsGame()
                        "Knights" -> KnightsGame()
                        "Snipers" -> SnipersGame()
                        else -> error("No such mode...")
                    }

                    games.add(game)

                    members.forEach { uUID -> playerGames[uUID] = game }

                    for (uuid in members) {
                        val player = Bukkit.getPlayer(uuid) ?: continue

                        player.spectatorTarget = null

                        val title = Title.title(Component.text(ADHDConfig.modes[modeName]!!.displayName).color(NamedTextColor.GOLD), Component.text(""))

                        player.showTitle(title)

                        player.sendMessage(description)

                        Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)})
                    }

                    game.start(
                        worldName = worldName,
                        arena = arena,
                        members = members,
                        modeMeta = ADHDConfig.modes[modeName]!!.meta,
                        variantParameters = variant.parameters,
                    )
                }

                timeBossBar.update()


                status = TournamentStatus.RUN
            }
            TournamentStatus.RUN -> {
                tryAnnounce()

                val variant = gameSequence[round]
                if ((currentTick >= variant.durationSeconds * 20L) ||
                    games.all { game -> game.getGameState() == GameState.FINISH }
                ) {
                    status = TournamentStatus.PREPARE

                    round++

                    games.filter { it.getGameState() != GameState.FINISH }.forEach { it.finish() }

                    tryAnnounce()

                    calculate()

                    if (checkWin()) prepareCeremony()

                    return
                }

                games.filter { game -> game.getGameState() == GameState.RUN }.forEach { game -> game.update() }

                timeBossBar.update()

                if (currentTick % 10L == 0L) gameScoreboardManager.updateAll()

                currentTick += 2L
            }
            TournamentStatus.CEREMONY -> {
                if (currentTick >= ADHDConfig.ceremonyDuration * 20L) {
                    status = TournamentStatus.FINISH

                    return
                }

                currentTick += 2L
            }
            TournamentStatus.FINISH -> {
                finish()
            }
        }
    }

    fun tryAnnounce() {
        games.filter { game -> !announced.contains(game) && game.getGameState() == GameState.FINISH }.forEach { game -> run {
            announced.add(game)

            val members = playerGames.filter { it.value == game }.keys

            val winners = game.summarize().keys

            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                player.gameMode = GameMode.SPECTATOR

                if (winners.contains(uuid) || (fakeAnnounced[uuid] ?: false)) {
                    player.sendMessage(Component.text("Вы выиграли в этом раунде!").color(NamedTextColor.GREEN))

                    fakeAnnounced[uuid] = false
                } else {
                    player.sendMessage(Component.text("Вы проиграли в этом раунде...").color(NamedTextColor.YELLOW))
                }
            }
        } }

        moveFinishedPlayersToActiveMatches()
    }

    private fun moveFinishedPlayersToActiveMatches() {
        val activeMatches = games
            .filter { it.getGameState() == GameState.RUN }
            .map { game ->
                playerGames
                    .filterValues { it == game }
                    .keys
                    .filter { Bukkit.getPlayer(it)?.isOnline == true }
                    .toSet()
            }
            .filter { it.isNotEmpty() }

        val finishedParticipants = announced
            .flatMap { game -> playerGames.filterValues { it == game }.keys }
            .toSet()

        val targets = activeMatches.flatten()
        if (targets.isEmpty()) return

        finishedParticipants.forEachIndexed { index, finished ->
            val player = Bukkit.getPlayer(finished) ?: return@forEachIndexed
            val targetPlayer = Bukkit.getPlayer(targets[index % targets.size]) ?: return@forEachIndexed
            player.gameMode = GameMode.SPECTATOR
            player.spectatorTarget = targetPlayer
        }
    }

    fun calculate() {
        games.forEach { game -> game.summarize().forEach { (uUID, p) -> run {
            results.putIfAbsent(uUID, 0.0)

            results[uUID] = results[uUID]!! + p
        } } }
    }

    fun checkWin(): Boolean {
        return results.any { it.value >= ADHDConfig.pointsGoal }
    }

    fun <T> resetGameRule(world: World, rule: GameRule<T>) {
        world.setGameRule(rule, rule.defaultValue)
    }

    fun resetGameRules() {
        val world = Bukkit.getWorld(worldName)!!

        for (rule in Registry.GAME_RULE) {
            resetGameRule(world, rule)
        }
    }

    fun prepareCeremony() {
        if (!ADHDConfig.ceremonyEnabled) {
            status = TournamentStatus.FINISH

            return
        }

        status = TournamentStatus.CEREMONY

        timeBossBar.removeAll()

        gameScoreboardManager.removeAll()

        val winners = calculateWinners()

        val message = Component.text("Турнир окончен! Победители: ")
                .color(NamedTextColor.GOLD)
            .append(Component.text(winners.joinToString(", ") {uUID -> Bukkit.getPlayer(uUID)?.displayName ?: "?"})
                .color(NamedTextColor.GREEN))

        currentTick = 0L

        val world = Bukkit.getWorld(worldName)!!

        val spawn = Location(world, ADHDConfig.ceremonySpawnPoint.x, ADHDConfig.ceremonySpawnPoint.y, ADHDConfig.ceremonySpawnPoint.z, ADHDConfig.ceremonySpawnPoint.yaw, ADHDConfig.ceremonySpawnPoint.pitch)

        for (uuid in participants) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            if (winners.contains(player.uniqueId)) {
                player.isGlowing = true

                player.sendMessage(Component.text("Турнир окончен! Вы выиграли ★").color(NamedTextColor.GOLD))
            } else {
                player.sendMessage(message)
            }

            player.gameMode = GameMode.ADVENTURE

            player.inventory.clear()

            player.teleport(spawn)
        }
    }

    fun calculateWinners(): Set<UUID> {
        val maxScore = results.filter { participants.contains(it.key) }.values.maxOrNull()

        val winners = results.filter { participants.contains(it.key) }.filterValues { it == maxScore }.keys

        return winners
    }

    fun getGame(player: Player): Game? {
        if (status != TournamentStatus.RUN) return null

        return playerGames[player.uniqueId]
    }

    fun remove(player: Player) {
        val game = playerGames.remove(player.uniqueId)

        if (game != null && status == TournamentStatus.RUN) {
            game.finish()

            playerGames.filter { it.value == game }.keys.forEach { uUID -> run {
                results.putIfAbsent(uUID, 0.0)

                results[uUID] = results[uUID]!! + 1.0

                fakeAnnounced[uUID] = true
            } }
        }

        participants.remove(player.uniqueId)

        timeBossBar.remove(player)

        gameScoreboardManager.remove(player)

        if (participants.isEmpty()) {
            finish()
        } else if (participants.size == 1) {
            tryAnnounce()

            if (generated) {
                prepareCeremony()
            } else {
                finish()
            }
        }
    }

    fun finish() {
        ADHDPlugin.instance.logger.info("Завершён турнир $this")

        status = TournamentStatus.FINISH

        games.filter { it.getGameState() != GameState.FINISH }.forEach { it.finish() }

        timeBossBar.removeAll()

        gameScoreboardManager.removeAll()

        try {
            ticker.cancel()
        } catch (_: Exception) {}

        TournamentManager.finish(this)
    }
}
