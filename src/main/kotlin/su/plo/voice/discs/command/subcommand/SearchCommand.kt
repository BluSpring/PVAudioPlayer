package su.plo.voice.discs.command.subcommand

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.minecraft.commands.CommandSourceStack
import su.plo.lib.api.chat.MinecraftTextClickEvent
import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.chat.MinecraftTextHoverEvent
import su.plo.voice.discs.command.CommandHandler
import su.plo.voice.discs.utils.extend.asVoicePlayer
import su.plo.voice.discs.utils.extend.sendTranslatable

class SearchCommand(val handler: CommandHandler) : Command<CommandSourceStack> {
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun run(context: CommandContext<CommandSourceStack>): Int {
        scope.launch {
            val sender = context.source
            val voicePlayer = sender.player?.asVoicePlayer(handler.plugin.voiceServer) ?: return@launch

            if (!voicePlayer.instance.hasPermission("pv.addon.discs.search")) {
                voicePlayer.instance.sendTranslatable("pv.addon.discs.error.no_permission")
                return@launch
            }

            val query = StringArgumentType.getString(context, "query")

            val tracks = try {
                handler.plugin.audioPlayerManager.getPlaylist("ytsearch:$query").await().tracks
            } catch (e: Exception) {
                voicePlayer.instance.sendTranslatable("pv.addon.discs.error.search_fail", e.message)
                return@launch
            }

            if (tracks.isEmpty()) {
                voicePlayer.instance.sendTranslatable("pv.addon.discs.error.search_fail", "No results")
                return@launch
            }

            tracks.take(5).forEach {
                val command = "/disc burn ${it.identifier}"
                val component = MinecraftTextComponent.translatable("pv.addon.discs.format.search_entry", it.info.title, it.info.author)
                    .hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal(command)))
                    .clickEvent(MinecraftTextClickEvent.suggestCommand(command))
                voicePlayer.instance.sendMessage(component)
            }
        }

        return 1
    }
}
