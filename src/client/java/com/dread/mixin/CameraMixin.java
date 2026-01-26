package com.dread.mixin;

import com.dread.client.DeathCinematicClientHandler;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies cinematic camera shake during death sequence.
 * Injects at render time (after entity rotation finalized) to avoid
 * feedback loop where entity AI fights with shake offsets.
 */
@Mixin(Camera.class)
public class CameraMixin {

    @Shadow
    private float pitch;

    @Shadow
    private float yaw;

    /**
     * Apply cinematic shake offsets to camera rotation.
     * Uses order = 900 to run before CrawlCameraMixin (default 1000).
     */
    @Inject(method = "setRotation", at = @At("TAIL"), order = 900)
    private void dread$applyCinematicShake(float yaw, float pitch, CallbackInfo ci) {
        if (!DeathCinematicClientHandler.isCinematicActive()) {
            return;
        }

        this.yaw += DeathCinematicClientHandler.getShakeYawOffset();
        this.pitch += DeathCinematicClientHandler.getShakePitchOffset();
    }
}
