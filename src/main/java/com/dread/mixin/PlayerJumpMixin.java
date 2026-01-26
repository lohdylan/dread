package com.dread.mixin;

import com.dread.death.DownedPlayersState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side mixin that prevents jumping when player is downed.
 * Reinforces the helpless crawling state.
 */
@Mixin(LivingEntity.class)
public class PlayerJumpMixin {

    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void dread$preventJumpWhenDowned(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            DownedPlayersState state = DownedPlayersState.getOrCreate(player.getServerWorld());
            if (state.isDowned(player)) {
                ci.cancel();
            }
        }
    }
}
