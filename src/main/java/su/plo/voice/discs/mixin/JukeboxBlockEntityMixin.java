package su.plo.voice.discs.mixin;

import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.plo.voice.discs.DiscsPlugin;
import su.plo.voice.discs.utils.extend.ItemStackKt;

@Mixin(JukeboxBlockEntity.class)
public class JukeboxBlockEntityMixin {
    @Inject(method = "shouldSendJukeboxPlayingEvent", at = @At("HEAD"), cancellable = true)
    private static void preventSendingJukeboxPlayingIfPVDisc(JukeboxBlockEntity jukeboxBlockEntity, CallbackInfoReturnable<Boolean> cir) {
        var disc = jukeboxBlockEntity.getRecord();

        if (ItemStackKt.isCustomDisc(disc, DiscsPlugin.instance)) {
            cir.setReturnValue(false);
        }
    }
}
