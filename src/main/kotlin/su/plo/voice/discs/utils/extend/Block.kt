package su.plo.voice.discs.utils.extend

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.JukeboxBlock

fun Block.isJukebox() = this is JukeboxBlock

fun Block.isBeaconBaseBlock() = when (this) {
    Blocks.IRON_BLOCK,
    Blocks.GOLD_BLOCK,
    Blocks.DIAMOND_BLOCK,
    Blocks.NETHERITE_BLOCK,
    Blocks.EMERALD_BLOCK -> true
    else -> false
}

fun Block.asJukebox() = this.takeIf { it.isJukebox() }?.let { it as JukeboxBlock }