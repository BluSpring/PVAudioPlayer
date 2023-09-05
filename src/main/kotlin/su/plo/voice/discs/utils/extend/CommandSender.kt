package su.plo.voice.discs.utils.extend

import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.player.VoicePlayer

fun MinecraftServerPlayer.sendTranslatable(key: String, vararg args: Any?) = this.getInstance<Player>().sendSystemMessage(
    Component.translatable(key, *args)
)

fun Player.asVoicePlayer(voiceServer: PlasmoVoiceServer): VoicePlayer? {
    return this.let { voiceServer.playerManager.getPlayerById(it.uuid).orElse(null) }
}