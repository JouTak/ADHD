package ru.joutak.adhd.game.concrete

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Player
import ru.joutak.adhd.game.Game
import ru.joutak.adhd.game.GameState
import ru.joutak.adhd.game.mode.meta.ModeMeta
import ru.joutak.adhd.world.Arena
import java.util.UUID

class RPSGame : Game() {

    lateinit var worldName: String

    lateinit var arena: Arena

    lateinit var members: Set<UUID>

    var state = GameState.START

    val result = mutableMapOf<UUID, Double>()

    var choice = ""

    var computerChoice = setOf("камень", "ножницы", "бумага").random()

    var announceTick = 20L

    var announceCycle = 6

    var announced = false

    val tokens = setOf("Камень", "Ножницы", "Бумага")

    override fun start(
        worldName: String,
        arena: Arena,
        members: Set<UUID>,
        modeMeta: ModeMeta?,
        variantParameters: Map<String, String>,
    ) {
        this.worldName = worldName
        this.arena = arena
        this.members = members

        for (uuid in members) {
            val player = Bukkit.getPlayer(uuid) ?: continue

            setPlayer(player)
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

        player.teleport(Location(Bukkit.getWorld(worldName)!!, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch))
    }

    override fun update() {
        if (choice.isNotEmpty() && !announced) {
            if (announceTick <= 0) {
                if (announceCycle == 0) {
                    announced = true

                    if (calculate()) {
                        val title = Title.title(Component.text(computerChoice.capitalize()).color(NamedTextColor.GREEN),
                            Component.text("Вы победили робота \uD83E\uDD16").color(NamedTextColor.GRAY))

                        for (uuid in members) {
                            val player = Bukkit.getPlayer(uuid) ?: continue

                            player.showTitle(title)

                            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
                        }
                    } else {
                        val title = Title.title(Component.text(computerChoice.capitalize()).color(NamedTextColor.RED),
                            Component.text("Вы проиграли роботу \uD83E\uDD16").color(NamedTextColor.GRAY))

                        for (uuid in members) {
                            val player = Bukkit.getPlayer(uuid) ?: continue

                            player.showTitle(title)

                            player.playSound(player.location, Sound.ENTITY_ENDERMAN_DEATH, 1.0f, 1.0f)
                        }
                    }

                    finish()
                } else {
                    val title = Title.title(Component.text(tokens.random()).color(NamedTextColor.YELLOW),
                        Component.text("Компьютер думает \uD83E\uDD16").color(NamedTextColor.GRAY),
                        6,
                        8,
                        6
                    )

                    for (uuid in members) {
                        val player = Bukkit.getPlayer(uuid) ?: continue

                        player.showTitle(title)

                        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)
                    }
                }

                announceTick = 20L

                announceCycle--
            }

            announceTick -= 2L
        }
    }

    fun calculate(): Boolean {
        val won = ((choice == "камень") && (computerChoice == "ножницы")) ||
                     ((choice == "бумага") && (computerChoice == "камень")) ||
                     ((choice == "ножницы") && (computerChoice == "бумага"))

        if (won) {
            for (uuid in members) {
                result[uuid] = 1.0
            }
        }

        return won
    }

    override fun getGameState(): GameState {
        return state
    }

    override fun finish() {
        state = GameState.FINISH

        if (!announced) calculate()
    }

    override fun summarize(): Map<UUID, Double> {
        return result
    }
}
