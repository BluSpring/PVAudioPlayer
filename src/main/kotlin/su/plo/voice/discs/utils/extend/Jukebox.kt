package su.plo.voice.discs.utils.extend

import net.minecraft.world.level.block.entity.JukeboxBlockEntity
import net.minecraft.world.level.gameevent.GameEvent

fun JukeboxBlockEntity.stopPlayingWithUpdate() {
    val level = this.level ?: return
    val oldRecord = record

    record = null
    level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, blockPos, GameEvent.Context.of(blockState))

    record = oldRecord
    level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, blockPos, GameEvent.Context.of(blockState))
}
