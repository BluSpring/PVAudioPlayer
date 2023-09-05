package su.plo.voice.discs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import su.plo.voice.discs.DiscsPlugin;
import su.plo.voice.discs.event.JukeboxEventListener;
import su.plo.voice.discs.utils.extend.ItemStackKt;

@Mixin(RecordItem.class)
public class RecordItemMixin {
    @Inject(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/player/Player;ILnet/minecraft/core/BlockPos;I)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void cancelIfCustom(UseOnContext useOnContext, CallbackInfoReturnable<InteractionResult> cir, Level level, BlockPos blockPos, BlockState blockState, ItemStack itemStack) {
        if (useOnContext.getLevel().isClientSide)
            return;

        if (!ItemStackKt.isCustomDisc(itemStack, DiscsPlugin.Companion.getInstance()))
            return;

        JukeboxEventListener.INSTANCE.onDiscInsert(level, blockPos, itemStack);

        itemStack.shrink(1);
        Player player = useOnContext.getPlayer();
        if (player != null) {
            player.awardStat(Stats.PLAY_RECORD);
        }

        cir.setReturnValue(InteractionResult.sidedSuccess(false));
    }
}
