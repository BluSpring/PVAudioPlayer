package su.plo.voice.discs.command.subcommand

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.tags.ItemTags
import su.plo.voice.discs.command.CommandHandler
import su.plo.voice.discs.utils.extend.asVoicePlayer
import su.plo.voice.discs.utils.extend.sendTranslatable

class EraseCommand(val handler: CommandHandler) : Command<CommandSourceStack> {
    override fun run(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source

        val voicePlayer = source.player?.asVoicePlayer(handler.plugin.voiceServer) ?: return 0

        if (!voicePlayer.instance.hasPermission("pv.addon.discs.erase")) {
            voicePlayer.instance.sendTranslatable("pv.addon.discs.error.no_permission")
            return 0
        }

        val player = source.player ?: run {
            voicePlayer.instance.sendTranslatable("pv.error.player_only_command")
            return 0
        }

        val item = player.mainHandItem
            .takeIf { it.`is`(ItemTags.MUSIC_DISCS) && it.tag != null }
            ?: run {
                voicePlayer.instance.sendTranslatable("pv.addon.discs.error.erase_wrong_item")
                return 0
            }

        item.tag!!.remove(handler.plugin.identifierKey)

        voicePlayer.instance.sendTranslatable("pv.addon.discs.success.erase")

        return 1
    }
}
