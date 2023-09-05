package su.plo.voice.discs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import su.plo.voice.discs.event.JukeboxEventListener;

@Mixin(JukeboxBlock.class)
public class JukeboxBlockMixin {
    @Inject(method = "dropRecording", at = @At("TAIL"))
    public void jukeboxDropRecording(Level level, BlockPos blockPos, CallbackInfo ci) {
        JukeboxEventListener.INSTANCE.onDiskEject(level, blockPos);
    }

    @Inject(method = "setRecord", at = @At("TAIL"))
    public void jukeboxInsertRecord(Entity entity, LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, ItemStack itemStack, CallbackInfo ci) {
        JukeboxEventListener.INSTANCE.onDiscInsert((Level) levelAccessor, blockPos, itemStack);
    }
}
