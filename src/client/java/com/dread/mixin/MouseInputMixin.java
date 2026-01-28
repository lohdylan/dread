package com.dread.mixin;

import com.dread.client.DeathCinematicClientHandler;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables mouse look input during death cinematic.
 * Prevents the camera from fighting with player mouse movement.
 */
@Mixin(Mouse.class)
public class MouseInputMixin {

    /**
     * Cancel mouse movement processing during death cinematic.
     * This prevents the jarring camera fight between cinematic control and player input.
     */
    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    private void dread$blockMouseDuringCinematic(CallbackInfo ci) {
        if (DeathCinematicClientHandler.isCinematicActive()) {
            ci.cancel();
        }
    }
}
