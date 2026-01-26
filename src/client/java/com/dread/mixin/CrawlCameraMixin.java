package com.dread.mixin;

import com.dread.client.CrawlCameraHandler;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side mixin that limits camera pitch when player is downed.
 * Prevents looking straight up to reinforce ground-level crawling feeling.
 */
@Mixin(Camera.class)
public class CrawlCameraMixin {

    @Shadow
    private float pitch;

    @Inject(method = "setRotation", at = @At("TAIL"))
    private void dread$limitPitchWhenCrawling(float yaw, float pitch, CallbackInfo ci) {
        // Clamp pitch after rotation is set
        this.pitch = CrawlCameraHandler.clampPitchIfDowned(this.pitch);
    }
}
