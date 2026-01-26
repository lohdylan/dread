package com.dread.mixin;

import com.dread.client.DownedStateClientHandler;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side mixin that prevents attack animations and sounds when player is downed.
 * Supplements server-side blocking for smooth UX (no visual glitches).
 */
@Mixin(MinecraftClient.class)
public class ClientAttackMixin {

    /**
     * Cancels attack processing at the earliest point when player is downed.
     * This prevents arm swing animation and any attack sounds from playing.
     */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void dread$preventAttackWhenDowned(CallbackInfoReturnable<Boolean> cir) {
        if (DownedStateClientHandler.isDownedEffectActive()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
