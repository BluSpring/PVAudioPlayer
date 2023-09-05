package su.plo.voice.discs.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import su.plo.voice.discs.DiscsPlugin;
import su.plo.voice.discs.event.JukeboxEventListener;
import su.plo.voice.discs.utils.extend.ItemStackKt;

@Mixin(InstrumentItem.class)
public class InstrumentItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void useOn(Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;

        var item = player.getItemInHand(interactionHand);

        if (!ItemStackKt.isCustomHorn(item, DiscsPlugin.Companion.getInstance()))
            return;

        JukeboxEventListener.INSTANCE.onPlayHorn(level, item, serverPlayer);
        player.startUsingItem(interactionHand);
        player.getCooldowns().addCooldown(item.getItem(), 140);
        level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
        cir.setReturnValue(InteractionResultHolder.consume(item));
    }
}
