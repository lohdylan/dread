package com.dread.mixin;

import com.dread.client.DeathCinematicClientHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies cinematic camera shake and position control during death sequence.
 * Injects at render time (after entity rotation finalized) to avoid
 * feedback loop where entity AI fights with shake offsets.
 */
@Mixin(Camera.class)
public class CameraMixin {

    @Shadow
    private float pitch;

    @Shadow
    private float yaw;

    @Shadow
    private Vec3d pos;

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

    /**
     * Apply cinematic position offsets to camera location.
     * Uses order = 900 to coordinate with rotation injection.
     * Modifies camera position during cinematic to create pull-back and face close-up shots.
     */
    @Inject(method = "update", at = @At("TAIL"), order = 900)
    private void dread$applyCinematicPosition(
        BlockView area, Entity focusedEntity, boolean thirdPerson,
        boolean inverseView, float tickDelta, CallbackInfo ci
    ) {
        if (!DeathCinematicClientHandler.isCinematicActive()) {
            return;
        }

        Vec3d positionOffset = DeathCinematicClientHandler.getCameraPositionOffset();
        this.pos = this.pos.add(positionOffset);
    }
}
