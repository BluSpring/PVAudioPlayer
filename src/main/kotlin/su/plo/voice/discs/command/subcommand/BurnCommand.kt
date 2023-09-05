package su.plo.voice.discs.command.subcommand

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.discs.command.CommandHandler
import su.plo.voice.discs.utils.extend.asVoicePlayer
import su.plo.voice.discs.utils.extend.sendTranslatable
import su.plo.voice.discs.utils.suspendSync

class BurnCommand(val handler: CommandHandler) : Command<CommandSourceStack> {

    val name = "burn"

    private val scope = CoroutineScope(Dispatchers.Default)

    /*override fun suggest(source: CommandSender, arguments: Array<out String>): List<String> {

        val voicePlayer = source.asPlayer()?.asVoicePlayer(handler.plugin.voiceServer) ?: return listOf()

        if (!voicePlayer.instance.hasPermission("pv.addon.discs.burn")) return listOf()

        if (arguments.size < 2 ) return listOf()
        if (arguments.size == 2) return listOf(
            handler.getTranslationStringByKey("pv.addon.discs.arg.url", voicePlayer.instance)
        )
        return listOf(
            handler.getTranslationStringByKey("pv.addon.discs.arg.label", voicePlayer.instance)
        )
    }*/

    private fun checkBurnable(voicePlayer: VoicePlayer, item: ItemStack): Boolean {
        if (!item.`is`(ItemTags.MUSIC_DISCS) && item.item !is InstrumentItem) {
            voicePlayer.instance.sendTranslatable("pv.addon.discs.error.not_a_record")
            return false
        }

        if (
            handler.plugin.addonConfig.burnableTag.requireBurnableTag &&
            (
                !item.orCreateTag.contains(handler.plugin.burnableKey) &&
                !voicePlayer.instance.hasPermission("pv.addon.discs.burn.burnable_check_bypass")
            )
        ) {
            voicePlayer.instance.sendTranslatable("pv.addon.discs.error.not_burnable")
            return false
        }

        return true
    }

    override fun run(context: CommandContext<CommandSourceStack>): Int {
        scope.launch {
            val voicePlayer = context.source.player?.asVoicePlayer(handler.plugin.voiceServer) ?: return@launch

            if (!voicePlayer.instance.hasPermission("pv.addon.discs.burn")) {
                voicePlayer.instance.sendTranslatable("pv.addon.discs.error.no_permission")
                return@launch
            }

            val identifier = StringArgumentType.getString(context, "identifier")

            val track = try {
                handler.plugin.audioPlayerManager.getTrack(identifier).await()
            } catch (e: Exception) {
                voicePlayer.instance.sendTranslatable("pv.addon.discs.error.get_track_fail", e.message)
                return@launch
            }

            val name = try { StringArgumentType.getString(context, "name") } catch (_: Exception) { track.info.title }

            val player = context.source.player ?: run {
                voicePlayer.instance.sendTranslatable("pv.error.player_only_command")
                return@launch
            }

            val item = suspendSync { player.mainHandItem }

            if (!checkBurnable(voicePlayer, item)) return@launch

            if (handler.plugin.addonConfig.addGlintToCustomDiscs) {
                handler.plugin.forbidGrindstone(item)
            }

            //val meta = DiscsPlugin.server.itemFactory.getItemMeta(item.type)

            val loreName = Component
                .literal(name)
                .withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.GRAY))

            suspendSync {
                if (!checkBurnable(voicePlayer, item)) return@suspendSync

                if (handler.plugin.addonConfig.addGlintToCustomDiscs) {
                    item.enchant(Enchantments.MENDING, 1)
                }

                val tag = item.orCreateTag
                tag.put("display", CompoundTag().apply {
                    this.put("Lore", ListTag().apply {
                        this.add(StringTag.valueOf(Component.Serializer.toJson(loreName)))
                    })
                })
                tag.putString(handler.plugin.identifierKey, identifier)
                tag.putString("instrument", "")
                tag.putInt("HideFlags", ItemStack.TooltipPart.ADDITIONAL.mask)

                voicePlayer.instance.sendTranslatable("pv.addon.discs.success.burn", name)
            }
        }
        return 1
    }
}
