package com.dread.mixin;

import com.dread.client.DownedStateClientHandler;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side mixin that maintains swimming/crawling pose when player is downed.
 * Prevents vanilla updatePose() from resetting pose based on water/sneaking state.
 */
@Mixin(PlayerEntity.class)
public class PlayerPoseMixin {

    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void dread$forceSwimmingPoseWhenDowned(CallbackInfo ci) {
        // Only apply on client side when downed effects are active
        if (DownedStateClientHandler.isDownedEffectActive()) {
            PlayerEntity player = (PlayerEntity)(Object)this;
            player.setPose(EntityPose.SWIMMING);
            ci.cancel(); // Prevent vanilla pose logic from running
        }
    }
}
