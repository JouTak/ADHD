package ru.joutak.adhd.game.concrete

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask
import ru.joutak.adhd.ADHDPlugin
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

    var right = 0
    var questionTicks = 0

    val timeBar = BossBar.bossBar(Component.text("ПОДГОТОВКА"), 0F,
        BossBar.Color.RED, BossBar.Overlay.PROGRESS)

    val boom = mutableListOf<BukkitTask>()

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
            val player = Bukkit.getPlayer(uuid) ?: continue

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

            boom.add(Bukkit.getScheduler().runTaskTimer(ADHDPlugin.instance, Runnable {
                player.spawnParticle(
                    Particle.EXPLOSION_EMITTER,
                    player.location,
                    1,
                    0.1, 0.1, 0.1
                )
                player.playSound(player.location, Sound.ENTITY_GENERIC_EXPLODE, 2F, 1F)
            }, 1L, 3L))
        }

        nextExp()

        state = GameState.RUN
    }

    fun generateExp3(): Expression {
        val expQueue = mutableListOf<Expression>()
        val size = setOf(3, 4).random()
        val opQueue = operations.keys.shuffled().take(size-1).toMutableList()

        for (i in 0..<size) {
            val n = Random.nextInt(3, 16)
            expQueue.add(Expression(n.toString(), n))
        }

        while (opQueue.isNotEmpty()) {
            val e1 = expQueue.removeFirst()
            val e2 = expQueue.removeFirst()
            val part = generatePart(e1, e2, opQueue.removeFirst())
            expQueue.addFirst(part)
        }

        return expQueue.first()
    }

    fun generatePart(a: Expression, b: Expression, op: String): Expression {
        val text = if (op == "*" && a.expString.contains(" ")) "(${a.expString}) $op ${b.expString}"
        else "${a.expString} $op ${b.expString}"
        val result = operations[op]!!(a.result, b.result)

        return Expression(text, result)
    }

    fun nextExp() {
        exp = generateExp3()

        val counterPart = Component.text("($right/${meta.quota}) ").color(NamedTextColor.YELLOW)
        val expPart = Component.text("${exp.expString} = ?").color(NamedTextColor.GREEN).decorate(
            TextDecoration.BOLD)

        timeBar.name(counterPart.append(expPart))
        questionTicks = (meta.interval * 20).toInt()

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.showTitle(
                Title.title(Component.text("${exp.expString} = ?").color(NamedTextColor.YELLOW),
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
        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue
            player.hideBossBar(timeBar)
        }
        for (task in boom) task.cancel()
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}