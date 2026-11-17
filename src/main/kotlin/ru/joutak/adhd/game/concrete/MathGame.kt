package ru.joutak.adhd.game.concrete

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.game.mode.meta.concrete.MathModeMeta
import ru.joutak.adhd.world.Arena
import java.util.UUID
import kotlin.random.Random

class MathGame : Game() {
    lateinit var arena: Arena
    lateinit var worldName: String
    lateinit var members: Set<UUID>
    lateinit var meta: MathModeMeta

    var state = GameState.START
    private val result = mutableMapOf<UUID, Double>()

    val operations = mapOf<String, (Int, Int) -> Int>(
        "+" to {a, b -> a + b},
        "-" to {a, b -> a - b},
        "*" to {a, b -> a * b}
    )

    data class Expression(val expString: String, val result: Int)
    lateinit var exp: Expression

    var total = -1
    var right = 0
    var questionTicks = 0

    //val timeBar = Bukkit.createBossBar("ПОДГОТОВКА", BarColor.RED, BarStyle.SOLID)
    val timeBar = BossBar.bossBar(Component.text("ПОДГОТОВКА"), 0F,
        BossBar.Color.RED, BossBar.Overlay.PROGRESS)

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?
    ) {
        this.arena = arena
        this.worldName = worldName
        this.members = members
        this.meta = modeMeta as MathModeMeta

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid)!!

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
            player.showBossBar(timeBar)
        }

        nextExp()

        state = GameState.RUN
    }

    fun generateExp2(): Expression {
        val a = Random.nextInt(1, 11)
        val b = Random.nextInt(1, 11)
        val op = operations.keys.random()

        val text = "$a $op $b = ?"
        val result = operations[op]!!(a, b)

        return Expression(text, result)
    }

    fun generateExp3(): Expression {
        val a = Random.nextInt(1, 11)
        val b = Random.nextInt(1, 11)
        val c = Random.nextInt(1, 11)

        val opMut = operations.toMutableMap()
        val op1 = opMut.keys.random()
        opMut.remove(op1)
        val op2 = opMut.keys.random()

        val text = "$a $op1 $b $op2 $c = ?"
        val result = if (op2 == "*") {
            operations[op1]!!(a, operations[op2]!!(b, c))
        } else {
            operations[op2]!!(c, operations[op1]!!(a, b))
        }

        return Expression(text, result)
    }

    fun nextExp() {
        total += 1
        exp = listOf(generateExp2(), generateExp3()).random()

        val counterPart = Component.text("($right/${meta.quota}) ").color(NamedTextColor.YELLOW)
        val expPart = Component.text(exp.expString).color(NamedTextColor.GREEN).decorate(
            TextDecoration.BOLD)

        timeBar.name(counterPart.append(expPart))
        questionTicks = (meta.interval * 20).toInt()

        for (uuid in members) {
            Bukkit.getPlayer(uuid)!!.showTitle(
                Title.title(Component.text(exp.expString).color(NamedTextColor.YELLOW),
                Component.text("Введите ответ в чат.").color(NamedTextColor.YELLOW)))
        }
    }

    override fun update() {
        if (right == meta.quota) {
            for (uuid in members) result[uuid] = 1.0
            finish()
        }

        questionTicks -= 1
        timeBar.progress((questionTicks / (meta.interval * 20)).toFloat())
        if (questionTicks <= 0) nextExp()
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH
        for (uuid in members) Bukkit.getPlayer(uuid)!!.hideBossBar(timeBar)
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}