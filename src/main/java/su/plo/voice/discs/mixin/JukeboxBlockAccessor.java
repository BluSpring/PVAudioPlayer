package su.plo.voice.discs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(JukeboxBlock.class)
public interface JukeboxBlockAccessor {
    @Invoker
    void callDropRecording(Level level, BlockPos blockPos);
}
