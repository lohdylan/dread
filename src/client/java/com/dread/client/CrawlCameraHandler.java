package com.dread.client;

import net.minecraft.util.math.MathHelper;

/**
 * Handles camera pitch limiting when player is downed/crawling.
 * Reinforces the "on the ground" feeling by preventing looking straight up.
 * Supports gradual transition for smooth cinematic-to-downed handoff.
 */
public class CrawlCameraHandler {

    // Pitch limits when crawling (can't look straight up)
    // Vanilla range is -90 (up) to +90 (down)
    private static final float VANILLA_MIN_PITCH = -90.0f;  // Normal: full look up
    private static final float DOWNED_MIN_PITCH = -30.0f;   // Downed: slightly above horizon
    private static final float MAX_PITCH = 90.0f;           // Full look down always allowed

    // Transition progress: 0.0 = vanilla limits, 1.0 = full downed limits
    private static float pitchLimitTransition = 1.0f;

    /**
     * Clamp pitch value when player is downed.
     * Uses gradual transition if setPitchLimitTransition was called.
     * @param pitch Original pitch value
     * @return Clamped pitch if downed, original otherwise
     */
    public static float clampPitchIfDowned(float pitch) {
        if (DownedStateClientHandler.isDownedEffectActive()) {
            // Interpolate min pitch based on transition progress
            float currentMinPitch = MathHelper.lerp(pitchLimitTransition, VANILLA_MIN_PITCH, DOWNED_MIN_PITCH);
            return MathHelper.clamp(pitch, currentMinPitch, MAX_PITCH);
        }
        return pitch;
    }

    /**
     * Set pitch limit transition progress.
     * Called by DeathCinematicClientHandler during SETTLE phase.
     *
     * @param progress 0.0 = vanilla limits (-90 to +90), 1.0 = downed limits (-30 to +90)
     */
    public static void setPitchLimitTransition(float progress) {
        pitchLimitTransition = Math.max(0.0f, Math.min(1.0f, progress));
    }

    /**
     * Reset pitch limit transition to full downed limits.
     * Called when cinematic ends or downed effects are removed.
     */
    public static void resetPitchLimitTransition() {
        pitchLimitTransition = 1.0f;
    }
}
