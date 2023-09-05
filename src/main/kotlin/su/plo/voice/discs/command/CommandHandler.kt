package su.plo.voice.discs.command

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.voice.discs.DiscsPlugin
import su.plo.voice.discs.command.subcommand.BurnCommand
import su.plo.voice.discs.command.subcommand.EraseCommand
import su.plo.voice.discs.command.subcommand.SearchCommand

class CommandHandler(
    val plugin: DiscsPlugin,
) {
    val builder: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("disc")
        .requires {
            it.hasPermission(2)
        }
        .then(
            Commands.literal("burn")
                .then(
                    Commands.argument("identifier", StringArgumentType.string())
                        .then(
                            Commands.argument("name", StringArgumentType.string())
                                .executes(BurnCommand(this))
                        )
                        .executes(BurnCommand(this))
                )
        )
        .then(
            Commands.literal("erase")
                .executes(EraseCommand(this))
        )
        .then(
            Commands.literal("search")
                .then(
                    Commands.argument("query", StringArgumentType.greedyString())
                        .executes(SearchCommand(this))
                )
        )

    fun getTranslationStringByKey(key: String, source: MinecraftCommandSource): String {
        return plugin.voiceServer.languages.getServerLanguage(source)[key] ?: key
    }
}
