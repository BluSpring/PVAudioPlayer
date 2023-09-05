package su.plo.voice.discs.utils.extend

import net.minecraft.tags.ItemTags
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.item.ItemStack
import su.plo.voice.discs.DiscsPlugin

fun ItemStack.isCustomDisc(plugin: DiscsPlugin) = this
        .takeIf { this.`is`(ItemTags.MUSIC_DISCS) }
        ?.tag?.contains(plugin.identifierKey)
    ?: false

fun ItemStack.isCustomHorn(plugin: DiscsPlugin) = this
        .takeIf { this.item is InstrumentItem }
        ?.tag?.contains(plugin.identifierKey)
    ?: false

fun ItemStack.customDiscIdentifier(plugin: DiscsPlugin): String? =
    this.takeIf { isCustomDisc(plugin) }
        ?.tag?.getString(plugin.identifierKey)

fun ItemStack.customHornIdentifier(plugin: DiscsPlugin): String? =
    this.takeIf { isCustomHorn(plugin) }
        ?.tag?.getString(plugin.identifierKey)