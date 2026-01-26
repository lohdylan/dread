package com.dread.mixin;

import com.dread.death.DownedPlayersState;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side mixin that prevents sprinting when player is downed.
 * Cancels at the input setter level to prevent input ghosting on revival.
 */
@Mixin(Entity.class)
public class PlayerSprintMixin {

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void dread$preventSprintWhenDowned(boolean sprinting, CallbackInfo ci) {
        // Only block when trying to START sprinting, allow turning it off
        if (!sprinting) return;

        if ((Object)this instanceof ServerPlayerEntity player) {
            DownedPlayersState state = DownedPlayersState.getOrCreate(player.getServerWorld());
            if (state.isDowned(player)) {
                ci.cancel();
            }
        }
    }
}
