package com.dread.client;

import java.util.Random;

/**
 * Camera shake handler using exponential decay for smooth, frame-rate independent shake.
 * Used during death cinematic for visceral impact.
 */
public class CameraShakeHandler {
    private static final float SHAKE_MAGNITUDE = 2.5f; // degrees (subtle but noticeable)
    private static final float DECAY_SPEED = 8.0f;     // medium decay (0.7-1s feel)

    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private boolean isActive = false;
    private final Random random = new Random();

    /**
     * Start camera shake with given intensity multiplier (0.0 to 1.0).
     * Creates sharp violent jolts in random directions.
     */
    public void startShake(float intensity) {
        if (intensity <= 0.0f) {
            return; // Shake disabled via config
        }

        // Pure chaos direction - random in all directions
        targetYaw = (random.nextFloat() - 0.5f) * 2.0f * SHAKE_MAGNITUDE * intensity;
        targetPitch = (random.nextFloat() - 0.5f) * 2.0f * SHAKE_MAGNITUDE * intensity;
        currentYaw = 0.0f;
        currentPitch = 0.0f;
        isActive = true;
    }

    /**
     * Update shake state using exponential decay.
     * Call once per tick during cinematic.
     *
     * @param deltaTime Time since last tick in seconds (typically 0.05 for 20 TPS)
     */
    public void tick(float deltaTime) {
        if (!isActive) return;

        // Exponential decay formula - frame-rate independent
        // Source: https://lisyarus.github.io/blog/posts/exponential-smoothing.html
        float decay = 1.0f - (float)Math.exp(-DECAY_SPEED * deltaTime);
        currentYaw += (targetYaw - currentYaw) * decay;
        currentPitch += (targetPitch - currentPitch) * decay;

        // Decay target toward zero (returns to normal)
        float targetDecay = (float)Math.exp(-DECAY_SPEED * deltaTime);
        targetYaw *= targetDecay;
        targetPitch *= targetDecay;

        // Stop when negligible (prevent floating point drift)
        if (Math.abs(currentYaw) < 0.01f && Math.abs(currentPitch) < 0.01f &&
            Math.abs(targetYaw) < 0.01f && Math.abs(targetPitch) < 0.01f) {
            reset();
        }
    }

    /**
     * Reset all shake state. Call when cinematic ends.
     */
    public void reset() {
        currentYaw = 0.0f;
        currentPitch = 0.0f;
        targetYaw = 0.0f;
        targetPitch = 0.0f;
        isActive = false;
    }

    /**
     * Get yaw offset to add to camera rotation.
     */
    public float getYawOffset() {
        return currentYaw;
    }

    /**
     * Get pitch offset to add to camera rotation.
     */
    public float getPitchOffset() {
        return currentPitch;
    }

    /**
     * Check if shake is currently active.
     */
    public boolean isActive() {
        return isActive;
    }
}
