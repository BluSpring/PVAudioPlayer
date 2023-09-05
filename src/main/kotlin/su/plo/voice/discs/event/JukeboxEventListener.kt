package su.plo.voice.discs.event

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.JukeboxBlock
import net.minecraft.world.level.block.entity.JukeboxBlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.Vec3
import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.world.ServerPos3d
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.discs.DiscsPlugin
import su.plo.voice.discs.mixin.JukeboxBlockAccessor
import su.plo.voice.discs.utils.extend.*
import su.plo.voice.discs.utils.suspendSync
import kotlin.math.pow

object JukeboxEventListener {
    private val plugin = DiscsPlugin.instance
    private val jobByBlock: MutableMap<BlockPos, Job> = HashMap()
    private val jobByPlayer: MutableMap<ServerPlayer, Job> = HashMap()

    private val scope = CoroutineScope(Dispatchers.Default)

    private var isStarted = false

    fun init() {
        ServerLifecycleEvents.SERVER_STARTED.register {
            it.execute {
                isStarted = true
            }
        }

        ServerChunkEvents.CHUNK_LOAD.register { level, chunk ->
            if (isStarted)
                onChunkLoad(level, chunk)
        }

        ServerChunkEvents.CHUNK_UNLOAD.register { level, chunk ->
            if (isStarted)
                onChunkUnload(level, chunk)
        }

        PlayerBlockBreakEvents.AFTER.register { level, player, pos, state, blockEntity ->
            if (blockEntity !is JukeboxBlockEntity)
                return@register

            onJukeboxBreak(pos, blockEntity)
        }

        ServerPlayConnectionEvents.DISCONNECT.register { listener, server ->
            jobByPlayer.remove(listener.player)?.cancel()
        }
    }

    fun onHopperInsertToJukebox(level: Level, pos: BlockPos, block: Block, item: ItemStack) {
        if (block !is JukeboxBlock) return

        val identifier = item.customDiscIdentifier(plugin) ?: return

        jobByBlock.remove(pos)?.cancel()
        jobByBlock[pos] = playTrack(identifier, level, block, pos, item)
    }

    fun onChunkLoad(level: ServerLevel, chunk: LevelChunk) {
        chunk.blockEntities.filter { it.value is JukeboxBlockEntity }
            .forEach {
                val jukebox = it.value as JukeboxBlockEntity? ?: return@forEach
                if (!jukebox.record.isCustomDisc(plugin)) return@forEach

                jukebox.stopPlayingWithUpdate()
            }
    }

    fun onChunkUnload(level: ServerLevel, chunk: LevelChunk) {
        chunk.blockEntities.filter { it.value is JukeboxBlockEntity }
            .forEach {
                jobByBlock.remove(it.key)?.cancel()

                val jukebox = it.value as JukeboxBlockEntity? ?: return@forEach
                jukebox.stopPlayingWithUpdate()
            }
    }

    fun onDiscInsert(level: Level, pos: BlockPos, item: ItemStack) {
        val state = level.getBlockState(pos)

        val identifier = item.customDiscIdentifier(plugin) ?: return

        /*
        voicePlayer.instance.sendActionBar(
            MinecraftTextComponent.translatable("pv.addon.discs.actionbar.loading")
                .withStyle(MinecraftTextStyle.YELLOW)
        )*/

        jobByBlock[pos]?.cancel()
        jobByBlock[pos] = playTrack(identifier, level, state.block, pos, item)
    }

    fun onDiskEject(level: Level, pos: BlockPos) {
        val jukebox = level.getBlockEntity(pos) as JukeboxBlockEntity? ?: return

        jukebox.stopPlayingWithUpdate()

        jobByBlock.remove(pos)?.cancel()
    }

    fun onJukeboxBreak(pos: BlockPos, jukebox: JukeboxBlockEntity) {
        jukebox
            .also {
                it.stopPlayingWithUpdate()
            }
            .let { jobByBlock.remove(pos) }
            ?.cancel()
    }

    /*fun onJukeboxExplode(event: EntityExplodeEvent) {
        event.blockList()
            .filter { it.isJukebox() }
            .forEach { jobByBlock.remove(it)?.cancel() }
    }*/

    fun onPlayHorn(level: Level, item: ItemStack, player: ServerPlayer) {
        val identifier = item.customHornIdentifier(plugin) ?: return

        jobByPlayer[player]?.cancel()
        jobByPlayer[player] = playGoatHorn(identifier, level, item, player)
    }

    private fun playGoatHorn(identifier: String, level: Level, item: ItemStack, player: ServerPlayer): Job = scope.launch {
        val track = try {
            plugin.audioPlayerManager.getTrack(identifier).await()
        } catch (e: Exception) {
            if (e is CancellationException)
                return@launch

            player.sendSystemMessage(
                    Component.translatable("pv.addon.discs.actionbar.track_not_found", e.message)
                        .withStyle(ChatFormatting.RED), true
                )
            e.printStackTrace()
            return@launch
        }

        val trackName = Component.Serializer.fromJson(item
            .tag
            ?.getCompound("display")
            ?.getList("Lore", StringTag.TAG_STRING.toInt())
            ?.getOrNull(0)
            ?.asString ?: "\"${track.info.title}\"")
            ?: Component.literal(track.info.title)

        val distance = DiscsPlugin.instance.addonConfig.distance.goatHornDistance

        //val pvPlayer = plugin.voiceServer.playerManager.getPlayerById(player.uuid).orElse(null) ?: return@launch

        val entity = EntityType.ARMOR_STAND.create(level) ?: return@launch
        entity.absMoveTo(player.x, player.y, player.z)
        entity.entityData.set(ArmorStand.DATA_CLIENT_FLAGS, (ArmorStand.CLIENT_FLAG_SMALL or ArmorStand.CLIENT_FLAG_NO_BASEPLATE).toByte())
        entity.isInvisible = true
        entity.isInvulnerable = true
        entity.isNoGravity = true
        level.addFreshEntity(entity)

        val source = plugin.sourceLine.createEntitySource(plugin.voiceServer.minecraftServer.getEntity(entity), true)

        player.sendSystemMessage(Component.translatable("pv.addon.discs.actionbar.playing", trackName), true)

        val job = plugin.audioPlayerManager.startTrackJob(track, source, distance)
        try {
            var lastTick = System.currentTimeMillis()

            while (job.isActive) {
                player.server.execute {
                    entity.teleportTo(player.x, player.y + 0.15, player.z)
                }

                // every 30 seconds we need to reset record state
                if (System.currentTimeMillis() - lastTick < 30_000L) {
                    delay(100L)
                    continue
                }

                lastTick = System.currentTimeMillis()
            }
        } finally {
            job.cancelAndJoin()

            player.server.execute {
                entity.remove(Entity.RemovalReason.DISCARDED)
            }

            suspendSync {
                jobByPlayer.remove(player)
            }
        }
    }

    private fun playTrack(
        identifier: String,
        level: Level,
        block: Block,
        blockPos: BlockPos,
        item: ItemStack,
        voicePlayer: VoicePlayer? = null,
    ): Job = scope.launch {
        val distance = when (plugin.addonConfig.distance.enableBeaconLikeDistance) {
            true -> plugin.addonConfig.distance.beaconLikeDistanceList[getBeaconLevel(level, blockPos)]
            false -> plugin.addonConfig.distance.jukeboxDistance
        }

        val world = plugin.voiceServer.minecraftServer.getWorld(level)
        val pos = ServerPos3d(
            world,
            blockPos.x.toDouble() + 0.5,
            blockPos.y.toDouble() + 1.5,
            blockPos.z.toDouble() + 0.5
        )

        val track = try {
            plugin.audioPlayerManager.getTrack(identifier).await()
        } catch (e: Exception) {
            if (e is CancellationException)
                return@launch

            // todo: send error to who?
            suspendSync { level.players().filter { it.distanceToSqr(Vec3(pos.x, pos.y, pos.z)) <= distance.toDouble().pow(2) } }
                .map { it as ServerPlayer }
                .forEach { it.sendSystemMessage(
                    Component.translatable("pv.addon.discs.actionbar.track_not_found", e.message)
                        .withStyle(ChatFormatting.RED), true
                ) }
            e.printStackTrace()
            suspendSync {
                (block.asJukebox() as JukeboxBlockAccessor).callDropRecording(level, blockPos)
            }
            return@launch
        }

        val trackName = Component.Serializer.fromJson(item
            .tag
            ?.getCompound("display")
            ?.getList("Lore", StringTag.TAG_STRING.toInt())
            ?.getOrNull(0)
            ?.asString ?: "\"${track.info.title}\"")
            ?: Component.literal(track.info.title)

        val source = plugin.sourceLine.createStaticSource(pos, true)
        source.setName(trackName.string)

        val actionbarMessage = MinecraftTextComponent.translatable(
            "pv.addon.discs.actionbar.playing", trackName
        )

        // todo: visualize distance to who?
        voicePlayer?.visualizeDistance(
            pos.toPosition(),
            distance.toInt(),
            0xf1c40f
        )

        suspendSync { level.players().filter { it.distanceToSqr(Vec3(pos.x, pos.y, pos.z)) <= distance.toDouble().pow(2) } }
            .map { it.asVoicePlayer(plugin.voiceServer) }
            .forEach { it?.sendAnimatedActionBar(actionbarMessage) }

        val job = plugin.audioPlayerManager.startTrackJob(track, source, distance)
        try {
            var lastTick = System.currentTimeMillis()

            while (job.isActive) {
                // every 30 seconds we need to reset record state
                if (System.currentTimeMillis() - lastTick < 30_000L) {
                    delay(100L)
                    continue
                }

                suspendSync {
                    val jukeboxEntity = level.getBlockEntity(blockPos) as JukeboxBlockEntity? ?: return@suspendSync

                    jukeboxEntity.record = jukeboxEntity.record
                    jukeboxEntity.playRecord()
                }
                lastTick = System.currentTimeMillis()

            }
        } finally {
            job.cancelAndJoin()

            suspendSync {
                val jukebox = level.getBlockEntity(blockPos) as JukeboxBlockEntity? ?: return@suspendSync
                jukebox.stopPlayingWithUpdate()
                jobByBlock.remove(blockPos)
            }
        }
    }

    private fun getBeaconLevel(world: Level, pos: BlockPos): Int {
        val blockPos = BlockPos.MutableBlockPos()

        return (1 until plugin.addonConfig.distance.beaconLikeDistanceList.size).takeWhile { level ->
            (-level..level).all { xOffset ->
                (-level..level).all { zOffset ->
                    blockPos.set(pos.x + xOffset, pos.y - level, pos.z + zOffset)
                    world.getBlockState(blockPos).block.isBeaconBaseBlock()
                }
            }
        }.count()
    }
}
