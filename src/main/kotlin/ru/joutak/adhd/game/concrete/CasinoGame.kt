package ru.joutak.adhd.game.concrete

import kotlinx.serialization.descriptors.PrimitiveKind
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minecraft.network.chat.OutgoingChatMessage
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.CasinoModeMeta
import ru.joutak.adhd.world.Arena
import java.util.UUID
import java.time.Duration

class CasinoGame : Game() {
    lateinit var arena: Arena
    lateinit var worldName: String
    lateinit var members: Set<UUID>

    var state = GameState.START

    private val result = mutableMapOf<UUID, Double>()
    private var currentBet: ColorBet? = null
    private var playerBalance = 10
    private var playerGoal = 30

    private var isSpinning = false
    private var isFinished = false

    private var spinTicks = 0
    private var currentDisplayNumber = 0
    private var finalNumber = 0

    private val redNumbers = setOf(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36)
    private val blackNumbers = setOf(2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35)

    private val colorSymbols = mapOf(
        "красное" to "§c●",
        "черное" to "§8●",
        "зеленое" to "§a●"
    )

    override fun start(worldName: String, arena: Arena, members: Set<UUID>, modeMeta: ModeMeta?) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        val meta = modeMeta as? CasinoModeMeta



        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            setPlayer(player)
            if (meta != null) {
                playerBalance = meta.initialBalance
                playerGoal = meta.goalBalance
            }

            player.sendMessage("Ваш начальный баланс: $playerBalance. Цель заработать $playerGoal")
        }


        state = GameState.RUN
    }

    fun setPlayer(player: Player) {
        player.inventory.clear()
        player.gameMode = GameMode.ADVENTURE
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20

        val spawn = arena.spawnPoints.random()
        player.teleport(
            Location(
                Bukkit.getWorld(worldName)!!,
                spawn.x, spawn.y, spawn.z,
                spawn.yaw, spawn.pitch
            )
        )
    }

    fun getPlayerBalance(uuid: UUID): Int {
        return playerBalance ?: 0
    }

    fun canPlaceBet(player: Player): Boolean {
        val uuid = player.uniqueId
        if (!members.contains(uuid)) return false
        if (isSpinning) return false
        return true
    }

    fun placeBet(player: Player,color: String, amount: Int): Boolean {
        val uuid = player.uniqueId

        if (!members.contains(uuid)) {
            return false
        }

        if (isSpinning) {
            player.sendMessage(Component.text("Не торопись, ковбой. Рулетка уже крутится").color(NamedTextColor.RED))
            return false
        }

        currentBet = ColorBet(color, amount)
        val balance = playerBalance ?: 0
        playerBalance = balance - amount

        startSpin()

        return true
    }

    private fun startSpin(){
        if (isSpinning) return

        finalNumber = (0..36).random()
        currentDisplayNumber = 0
        spinTicks = 0
        isSpinning = true

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            sendActionBar(player, "Рулетка крутится...")
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)

        }

    }

    override fun update() {
        if (state != GameState.RUN || isFinished) return

        if (!isSpinning) {
            return
        }

        spinTicks++

        if (spinTicks % 4 == 0){
            val displayNumber = (0..36).random()
            val progress = spinTicks / 4
            val totalSteps = 20

            val speed = when {
                progress < 10 -> 4
                progress < 15 -> 6
                else -> 10
            }

            if (spinTicks % speed == 0){
                val titleText = "$displayNumber"
                val subtitleText = "Вращение..."

                val title = Title.title(
                    Component.text(titleText)
                        .color(getAdventureColor(displayNumber))
                        .decorate(TextDecoration.BOLD),
                    Component.text(subtitleText)
                        .color(NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(400),Duration.ofMillis(200))
                )
                for (uuid in members) {
                    val player = Bukkit.getPlayer(uuid) ?: continue

                    player.showTitle(title)

                    if (spinTicks % 8 == 0) {
                        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.0f)
                    }
                }
            }

            if (spinTicks >= 80) {
                finishSpin()
            }
        }
    }

    private fun finishSpin(){
        isSpinning = false

        val resultColor = getColor(finalNumber)
        val colorDisplay = when (resultColor) {
            "красное" -> "§cКРАСНОЕ"
            "черное" -> "§8ЧЕРНОЕ"
            "зеленое" -> "§aЗЕЛЕНОЕ"
            else -> "§7НЕИЗВЕСТНО"
        }

        val resultTitle = Title.title(
            Component.text("$finalNumber")
                .color(getAdventureColor(finalNumber))
                .decorate(TextDecoration.BOLD),
            Component.text(colorDisplay)
                .color(getAdventureColor(finalNumber))
                .decorate(TextDecoration.BOLD),
            Title.Times.times(
                Duration.ofMillis(0),
                Duration.ofMillis(3000),
                Duration.ofMillis(500)
            )
        )
        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            player.showTitle(resultTitle)
        }

        processBet(resultColor)
        currentBet = null

        if (playerBalance <= 0 ) {
            isFinished = true

            val title = Title.title(Component.text("Вы проиграли все свои гроши :(").color(NamedTextColor.RED),
                Component.text("В следующий раз повезёт").color(NamedTextColor.GRAY))
            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                player.showTitle(title)

                player.playSound(player.location, Sound.ENTITY_ENDERMAN_DEATH, 0.5f, 1.0f)
            }
            finish()
        } else if (playerBalance >= playerGoal) {
            isFinished = true

            val title = Title.title(Component.text("Вы обыграли казино!!!!").color(NamedTextColor.GOLD),
                Component.text("Вы ушли, забрав с собой $playerBalance").color(NamedTextColor.YELLOW))
            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                player.showTitle(title)

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            }
            finish()
        } else {
            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue

                player!!.sendMessage(Component.text("Вы еще не достигли цели в $playerGoal. Продолжай играть, мучачо").color(NamedTextColor.GOLD))
                player!!.sendMessage(Component.text("Напишите в чат: <цвет> <сумма>").color(NamedTextColor.GRAY))
            }
        }
    }

    private fun processBet(resultColor: String) {
        val bet = currentBet ?: return
        var winAmount = 0

        if (bet.color == resultColor) {
            if (resultColor == "черное" || resultColor == "красное"){
                winAmount = bet.amount * 2
            }
            else if (resultColor == "зеленое"){
                winAmount = bet.amount * 35
            }

            playerBalance += winAmount

            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                player.sendMessage(Component.text("Выигрыш +$winAmount").color(NamedTextColor.GREEN))
                player.sendMessage(Component.text("Новый баланс: $playerBalance").color(NamedTextColor.YELLOW))

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)

                if (playerBalance >= playerGoal) {
                    result[player.uniqueId] = 1.0
                }
            }
        } else {
            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                player.sendMessage(Component.text("Проигрыш -${bet.amount}").color(NamedTextColor.RED))
                player.sendMessage(Component.text("Новый баланс: $playerBalance").color(NamedTextColor.YELLOW))
            }
        }
    }

    private fun getColor(number: Int): String {
        return when {
            number == 0 -> "зеленое"
            number in redNumbers -> "красное"
            number in blackNumbers -> "черное"
            else -> "зеленое"
        }
    }

    private fun getAdventureColor(number: Int): NamedTextColor {
        return when {
            number == 0 -> NamedTextColor.GREEN
            number in redNumbers -> NamedTextColor.RED
            number in blackNumbers -> NamedTextColor.DARK_GRAY
            else -> NamedTextColor.WHITE
        }
    }

    private fun sendActionBar(player: Player, message: String) {
        player.sendActionBar(Component.text(message))
    }
    override fun getGameState(): GameState {
        return state
    }
    override fun summarize(): Map<UUID, Double> {
        return result
    }

    override fun finish() {
        state = GameState.FINISH

        if (!isFinished){
            val title = Title.title(Component.text("К сожалению вы не успели").color(NamedTextColor.GOLD),
                Component.text("Вы ушли, забрав с собой $playerBalance").color(NamedTextColor.YELLOW))
            for (uuid in members) {
                val player = Bukkit.getPlayer(uuid) ?: continue
                player.showTitle(title)

                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            }
        }

        isFinished = true
    }

    data class ColorBet(
        val color: String,
        val amount: Int
    )

}