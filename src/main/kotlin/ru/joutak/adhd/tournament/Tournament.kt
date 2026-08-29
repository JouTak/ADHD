package ru.joutak.adhd.tournament

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import ru.joutak.adhd.ADHDPlugin
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.concrete.KnightsGame
import ru.joutak.adhd.game.concrete.MemoryGame
import ru.joutak.adhd.game.concrete.PVPGame
import ru.joutak.adhd.game.concrete.PillarsGame
import ru.joutak.adhd.game.concrete.RPSGame
import ru.joutak.adhd.game.concrete.SnipersGame
import ru.joutak.adhd.ui.GameScoreboardManager
import ru.joutak.adhd.ui.TimeBossBar
import ru.joutak.adhd.world.Arena
import ru.joutak.minigames.MiniGamesAPI
import ru.joutak.minigames.config.ConfigKeys
import ru.joutak.minigames.results.model.CompletionStatus
import ru.joutak.minigames.results.model.MatchContext
import ru.joutak.minigames.results.model.MatchResult
import ru.joutak.minigames.results.model.Metric
import ru.joutak.minigames.results.model.PlayerResult
import ru.joutak.minigames.results.model.ResultPlacementResolver
import ru.joutak.minigames.results.model.TeamResult
import java.util.*

class Tournament(
    val participants: MutableList<UUID>,
    val pool: List<String>,
    private val teamIdByPlayer: Map<UUID, Int>,
    private val teamKeysByTeamId: Map<Int, String>,
) {

    val originalParticipants: List<UUID> = participants.toList()

    private val leftAtMs = mutableMapOf<UUID, Long>()

    private val playerNames = mutableMapOf<UUID, String>()

    private val matchId: UUID = UUID.randomUUID()

    private var startedAtMs: Long = 0L

    private var resultRecorded = false

    lateinit var worldName: String

    lateinit var arenas: Map<Int, List<Arena>>

    lateinit var singleArenas: Map<String, List<Arena>>

    var status = TournamentStatus.GENERATE

    var currentTick = 0L

    var round = 0

    var generated = false

    val games = mutableSetOf<Game>()

    val playerGames = mutableMapOf<UUID, Game>()

    val results = originalParticipants.associateWith { 0.0 }.toMutableMap()

    val announced = mutableListOf<Game>()

    val timeBossBar = TimeBossBar()

    val gameScoreboardManager = GameScoreboardManager(this)

    val fakeAnnounced = mutableMapOf<UUID, Boolean>()

    var single: Pair<UUID, String>? = null

    var singleGame: Game? = null

    private val pairs = mutableListOf<Pair<UUID, UUID>>()

    private val singles = mutableListOf<UUID>()

    val ticker = object : BukkitRunnable() {
        override fun run() {
            tick()
        }
    }

    fun start(worldName: String, arenas: Map<Int, List<Arena>>, singleArenas: Map<String, List<Arena>>) {
        this.worldName = worldName
        this.arenas = arenas
        this.singleArenas = singleArenas

        status = TournamentStatus.START
        startedAtMs = System.currentTimeMillis()

        TournamentManager.removePrepareAnnounce(this)

        timeBossBar.load(this)

        for (uuid in participants) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            timeBossBar.add(player)

            gameScoreboardManager.add(player)

            playerNames[uuid] = player.name
        }

        ticker.runTaskTimer(ADHDPlugin.instance, 2L, 2L)

        ADHDPlugin.instance.logger.info("Начат турнир $this")
    }

    fun tick() {
        when (status) {
            TournamentStatus.GENERATE -> status = TournamentStatus.START
            TournamentStatus.START -> status = TournamentStatus.PREPARE
            TournamentStatus.PREPARE -> {
                status = TournamentStatus.WAIT

                Bukkit.getScheduler().runTaskLater(ADHDPlugin.instance, Runnable {prepare()}, 30L)
            }
            TournamentStatus.RUN -> {
                tryAnnounce()

                if ((currentTick >= ADHDConfig.modes[pool[round]]!!.duration * 20L) || (games.filter { game -> game != singleGame }.all { game -> game.getGameState() == GameState.FINISH })) {
                    status = TournamentStatus.PREPARE

                    round++

                    games.filter { it.getGameState() != GameState.FINISH }.forEach { it.finish() }

                    timeBossBar.prepareTitle()

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
            TournamentStatus.WAIT -> return
        }
    }

    fun prepare() {
        if (status == TournamentStatus.WAIT) {
            if (round == pool.size) {
                prepareCeremony()

                return
            }

            games.clear()

            playerGames.clear()

            announced.clear()

            currentTick = 0L

            resetGameRules()

            val singlePlayer = single?.let{
                Bukkit.getPlayer(it.first)
            }

            singlePlayer?.let {
                timeBossBar.switchSingle(it, false)
            }

            val modeName = pool[round]

            val gArenas = arenas[round]!!

            val assignedMembers = mutableMapOf<MutableSet<UUID>, Arena>()

            val tournamentRound = generateRound()

            tournamentRound.pairs.forEachIndexed { index, (f, s) -> assignedMembers[mutableSetOf(f, s)] = gArenas[index] }

            if (tournamentRound.single != null) {
                val member = tournamentRound.single

                val name = ADHDConfig.singleModeNames.random()

                val arena = singleArenas[name]!!.random()

                val sGame = when (name) {
                    "RPS" -> RPSGame()
                    "Memory" -> MemoryGame()
                    else -> error("No such single mode...")
                }

                games.add(sGame)

                singleGame = sGame

                playerGames[member] = sGame

                single = Pair(member, name)

                timeBossBar.switchSingle(Bukkit.getPlayer(member)!!, true)

                Bukkit.getPlayer(member)?.sendMessage(Component.text("Игроков не хватает. Вы играете против компьютера...").color(
                    NamedTextColor.YELLOW))

                val description = Component.text("[").color(NamedTextColor.GRAY)
                    .append(Component.text(ADHDConfig.modes[name]!!.displayName).color(NamedTextColor.GOLD))
                    .append(Component.text("] ").color(NamedTextColor.GRAY))
                    .append(Component.text(ADHDConfig.modes[name]!!.description).color(NamedTextColor.WHITE))

                Bukkit.getPlayer(member)?.sendMessage(description)

                val title = Title.title(Component.text(ADHDConfig.modes[name]!!.displayName).color(NamedTextColor.GOLD), Component.text(""))

                Bukkit.getPlayer(member)?.showTitle(title)

                Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {
                    val player = Bukkit.getPlayer(member) ?: return@Runnable

                    player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                })

                sGame.start(worldName, arena, setOf(member), ADHDConfig.modes[name]!!.meta)
            }

            val description = Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text(ADHDConfig.modes[modeName]!!.displayName).color(NamedTextColor.GOLD))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Component.text(ADHDConfig.modes[modeName]!!.description).color(NamedTextColor.WHITE))

            for (mS in assignedMembers.keys) {
                val game = when (modeName) {
                    "PVP" -> PVPGame()
                    "Pillars" -> PillarsGame()
                    "Knights" -> KnightsGame()
                    "Snipers" -> SnipersGame()
                    else -> error("No such mode...")
                }

                games.add(game)

                mS.forEach { uUID -> playerGames[uUID] = game }

                for (uuid in mS) {
                    val player = Bukkit.getPlayer(uuid) ?: continue

                    val title = Title.title(Component.text(ADHDConfig.modes[modeName]!!.displayName).color(NamedTextColor.GOLD), Component.text(""))

                    player.showTitle(title)

                    player.sendMessage(description)

                    Bukkit.getScheduler().runTask(ADHDPlugin.instance, Runnable {player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)})
                }

                game.start(worldName, assignedMembers[mS]!!, mS, ADHDConfig.modes[pool[round]]!!.meta)
            }

            timeBossBar.update()

            status = TournamentStatus.RUN
        }
    }

    data class Round(
        val pairs: List<Pair<UUID, UUID>>,
        val single: UUID?
    )

    fun generateRound(): Round {
        val members = participants.toSet()

        if ((members - singles.toSet()).isEmpty()) {
            singles.clear()
        }

        val single = if (members.size % 2 != 0) {
            (members - singles.toSet()).random()
        } else {
            null
        }

        single?.let {
            singles += it
            pairs.removeIf { pair ->
                pair.first == it || pair.second == it
            }
        }

        val remaining = (members - setOfNotNull(single)).toMutableSet()
        val newPairs = mutableListOf<Pair<UUID, UUID>>()

        while (remaining.isNotEmpty()) {
            val a = remaining.random()
            val b = (remaining - a).random()

            pairs.removeIf {
                it.first == a || it.second == a ||
                        it.first == b || it.second == b
            }

            val pair = if (a < b) a to b else b to a

            pairs += pair
            newPairs += pair

            remaining.removeAll(setOf(a, b))
        }

        return Round(newPairs, single)
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

        world.setGameRule(GameRules.LOCATOR_BAR, false)
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

        world.setGameRule(GameRules.PVP, false)

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
        if (player.uniqueId !in originalParticipants || player.uniqueId in leftAtMs) return
        leftAtMs[player.uniqueId] = System.currentTimeMillis()
        playerNames.putIfAbsent(player.uniqueId, player.name)

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

        recordResultIfNeeded()

        try {
            ticker.cancel()
        } catch (_: Exception) {}

        TournamentManager.finish(this)
    }

    private fun recordResultIfNeeded() {
        if (resultRecorded || startedAtMs <= 0L || TournamentManager.shutdownFlag) return
        resultRecorded = true

        val placements = ResultPlacementResolver.resolve(
            originalParticipants.map { uuid ->
                ResultPlacementResolver.Entry(
                    key = uuid,
                    score = results[uuid] ?: 0.0,
                    completionStatus = if (uuid in leftAtMs) CompletionStatus.LEFT else CompletionStatus.FINISHED,
                )
            }
        )

        val resolvedTeamIdByPlayer = originalParticipants.mapIndexed { index, uuid ->
            uuid to (teamIdByPlayer[uuid] ?: index + 1)
        }.toMap()

        val teamResults = originalParticipants.map { uuid ->
            val status = if (uuid in leftAtMs) CompletionStatus.LEFT else CompletionStatus.FINISHED
            val place = placements[uuid] ?: originalParticipants.size
            TeamResult(
                teamId = resolvedTeamIdByPlayer.getValue(uuid),
                placement = place,
                isWinner = status == CompletionStatus.FINISHED && place == 1,
                score = results[uuid] ?: 0.0,
                metrics = listOf(Metric.real("adhd_points", results[uuid] ?: 0.0)),
                completionStatus = status,
            )
        }

        val playerResults = originalParticipants.map { uuid ->
            val place = placements[uuid] ?: originalParticipants.size
            PlayerResult(
                playerUuid = uuid,
                playerName = playerNames[uuid] ?: Bukkit.getOfflinePlayer(uuid).name,
                teamId = resolvedTeamIdByPlayer.getValue(uuid),
                isWinner = uuid !in leftAtMs && place == 1,
                joinedAtMs = startedAtMs,
                leftAtMs = leftAtMs[uuid],
                metrics = listOf(Metric.int("left", if (uuid in leftAtMs) 1L else 0L)),
            )
        }

        val context = if (MiniGamesAPI.isTournamentEnabled()) {
            val eventId = MiniGamesAPI.config.get(ConfigKeys.TOURNAMENT_EVENT_ID).trim()
            val stage = MiniGamesAPI.config.get(ConfigKeys.TOURNAMENT_STAGE).trim()
            if (eventId.isNotBlank() && stage.isNotBlank()) MatchContext(eventId, stage) else null
        } else {
            null
        }

        val result = MatchResult(
            matchId = matchId,
            modeKey = "adhd",
            mapKey = "adhd",
            startedAtMs = startedAtMs,
            endedAtMs = System.currentTimeMillis(),
            context = context,
            teams = teamResults,
            players = playerResults,
        )

        val ratingKeys = resolvedTeamIdByPlayer.map { (uuid, teamId) ->
            teamId to (teamKeysByTeamId[teamId] ?: "player:$uuid")
        }.toMap()

        MiniGamesAPI.recordMatchResult(result, ratingKeys)
    }
}
