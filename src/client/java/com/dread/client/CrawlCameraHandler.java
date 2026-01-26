package com.dread.client;

import net.minecraft.util.math.MathHelper;

/**
 * Handles camera pitch limiting when player is downed/crawling.
 * Reinforces the "on the ground" feeling by preventing looking straight up.
 */
public class CrawlCameraHandler {

    // Pitch limits when crawling (can't look straight up)
    // Vanilla range is -90 (up) to +90 (down)
    private static final float MIN_PITCH = -30.0f;  // Slightly above horizon
    private static final float MAX_PITCH = 90.0f;   // Full look down allowed

    /**
     * Clamp pitch value when player is downed.
     * @param pitch Original pitch value
     * @return Clamped pitch if downed, original otherwise
     */
    public static float clampPitchIfDowned(float pitch) {
        if (DownedStateClientHandler.isDownedEffectActive()) {
            return MathHelper.clamp(pitch, MIN_PITCH, MAX_PITCH);
        }
        return pitch;
    }
}
