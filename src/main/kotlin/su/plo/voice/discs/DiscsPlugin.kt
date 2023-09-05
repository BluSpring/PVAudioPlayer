package su.plo.voice.discs

import com.google.inject.Inject
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.audio.line.ServerSourceLine
import su.plo.voice.api.server.event.config.VoiceServerConfigReloadedEvent
import su.plo.voice.discs.command.CommandHandler
//import su.plo.voice.discs.crafting.BurnableDiscCraft
import su.plo.voice.discs.event.JukeboxEventListener

@Addon(
    id = "pv-addon-discs",
    scope = AddonLoaderScope.SERVER,
    version = "1.0.6",
    authors = ["KPidS"]
)
class DiscsPlugin : ModInitializer, AddonInitializer {

    private val addonName = "discs"

    @Inject
    lateinit var voiceServer: PlasmoVoiceServer

    lateinit var sourceLine: ServerSourceLine

    lateinit var audioPlayerManager: PlasmoAudioPlayerManager

    lateinit var addonConfig: AddonConfig

    // PersistentDataType.String
    val identifierKey = "PVDiscId"

    // PersistentDataType.BYTE 0 or 1 representing boolean
    val burnableKey = "PVDiscBurnable"

    // PersistentDataType.BYTE 0 or 1 representing boolean
    val forbidGrindstoneKey = "PVDiscGrindstoneForbid"

    @EventSubscribe
    fun onConfigReloaded(event: VoiceServerConfigReloadedEvent) {
        loadConfig()
    }

    override fun onInitialize() {
        instance = this
        PlasmoVoiceServer.getAddonsLoader().load(this)

        ServerLifecycleEvents.SERVER_STARTING.register {
            server = it
        }

        JukeboxEventListener.init()
        //ForbidGrindstoneListener(this)

        val handler = CommandHandler(this)

        CommandRegistrationCallback.EVENT.register { dispatcher, context, selection ->
            dispatcher.register(handler.builder)
        }

        val permissions = voiceServer.minecraftServer.permissionsManager

        permissions.register("pv.addon.discs.play", PermissionDefault.TRUE)
    }

    private fun loadConfig() {
        addonConfig = AddonConfig.loadConfig(voiceServer)

        sourceLine = voiceServer.sourceLineManager.createBuilder(
            this,
            addonName,
            "pv.activation.$addonName",
            "plasmovoice:textures/icons/speaker_disc.png",
            addonConfig.sourceLineWeight
        ).apply {
            setDefaultVolume(addonConfig.defaultSourceLineVolume)
        }.build()

        audioPlayerManager = PlasmoAudioPlayerManager(this)
    }

    fun forbidGrindstone(item: ItemStack) {
        item.orCreateTag.putByte(forbidGrindstoneKey, 1)
    }

    companion object {
        lateinit var instance: DiscsPlugin
        lateinit var server: MinecraftServer
    }

    override fun onAddonInitialize() {
        loadConfig()
    }
}