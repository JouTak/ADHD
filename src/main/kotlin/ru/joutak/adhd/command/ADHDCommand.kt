package ru.joutak.adhd.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.ConsoleCommandSender
import ru.joutak.adhd.config.ADHDConfig
import ru.joutak.adhd.tournament.TournamentManager

object ADHDCommand {

    fun create(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("adhd")
            .requires { it.sender.isOp || it.sender is ConsoleCommandSender }
            .then(
                Commands.literal("reload")
                    .executes { context ->
                        if (TournamentManager.activeTournaments.isNotEmpty()) {
                            context.source.sender.sendMessage(Component.text("Не удалось перезагрузить конфиг: есть активные игры...").color(
                                NamedTextColor.RED))

                            return@executes -1
                        }

                        ADHDConfig.load()

                        context.source.sender.sendMessage(Component.text("Конфиг успешно перезагружен!").color(
                            NamedTextColor.GREEN))

                        Command.SINGLE_SUCCESS
                    }
            )
            .build()
    }
}