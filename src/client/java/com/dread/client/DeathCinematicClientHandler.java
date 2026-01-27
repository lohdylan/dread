package com.dread.client;

import com.dread.config.DreadConfigLoader;
import com.dread.entity.DreadEntity;
import com.dread.network.packets.CinematicTriggerS2C;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/**
 * Client-side handler for death cinematic camera lock.
 * Locks camera onto Dread entity for 1.8 seconds (matching death_grab animation)
 * before transitioning to downed state.
 */
public class DeathCinematicClientHandler {

    private static final int CINEMATIC_DURATION_TICKS = 36; // 1.8 seconds (matches death_grab animation)

    private static final CameraShakeHandler cameraShake = new CameraShakeHandler();

    private static boolean cinematicActive = false;
    private static int cinematicTimer = 0;
    private static int dreadEntityId = -1;
    private static Entity originalCameraEntity = null;

    /**
     * Register client tick event for cinematic updates.
     */
    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (cinematicActive) {
                tick();
            }
        });
    }

    /**
     * Start the death cinematic - lock camera onto Dread.
     *
     * @param payload Cinematic trigger packet from server
     */
    public static void startCinematic(CinematicTriggerS2C payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        // Store original camera entity (the player)
        originalCameraEntity = client.getCameraEntity();

        // Find Dread entity in world
        dreadEntityId = payload.dreadEntityId();
        Entity dreadEntity = client.world.getEntityById(dreadEntityId);

        if (dreadEntity != null) {
            // Lock camera onto Dread
            client.setCameraEntity(dreadEntity);

            // Trigger death_grab animation on entity
            if (dreadEntity instanceof DreadEntity dread) {
                dread.setPlayingDeathGrab(true);
            }

            // Calculate shake intensity with FPS adaptation
            var config = DreadConfigLoader.getConfig();
            float configIntensity = Math.clamp(config.cameraShakeIntensity, 0, 100) / 100.0f;
            float adaptiveIntensity = cameraShake.getAdaptiveIntensity(client, configIntensity);

            // Start shake with final intensity
            cameraShake.startShake(adaptiveIntensity);

            // Trigger compensation if shake was reduced
            float compensation = cameraShake.getCompensationAmount(configIntensity, adaptiveIntensity);
            if (compensation > 0.0f) {
                CinematicCompensationRenderer.setCompensation(compensation);
            }

            // Start cinematic timer
            cinematicActive = true;
            cinematicTimer = 0;
        }
    }

    /**
     * Tick the cinematic timer and check if duration is complete.
     */
    private static void tick() {
        cinematicTimer++;

        MinecraftClient client = MinecraftClient.getInstance();
        Entity cameraEntity = client.getCameraEntity();
        if (cameraEntity == null) return;

        // Update shake (deltaTime = 1 tick = 0.05 seconds)
        cameraShake.tick(0.05f);

        // Tick compensation flash timer
        CinematicCompensationRenderer.tick();

        if (cinematicTimer >= CINEMATIC_DURATION_TICKS) {
            endCinematic();
        }
    }

    /**
     * End the cinematic - restore camera to player and apply downed effects.
     */
    private static void endCinematic() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Reset shake and compensation
        cameraShake.reset();
        CinematicCompensationRenderer.stop();

        // Stop death_grab animation on entity
        if (client.world != null && dreadEntityId != -1) {
            Entity entity = client.world.getEntityById(dreadEntityId);
            if (entity instanceof DreadEntity dread) {
                dread.setPlayingDeathGrab(false);
            }
        }

        // Restore camera to original entity (player)
        if (originalCameraEntity != null) {
            client.setCameraEntity(originalCameraEntity);
            originalCameraEntity = null;
        }

        // NOTE: Don't apply downed effects here - server sends DownedStateUpdateS2C packets
        // to sync state. Calling applyDownedEffects() with defaults would overwrite the
        // correct server-synced state (wrong timer, wrong mercy mode).

        // Reset cinematic state
        cinematicActive = false;
        cinematicTimer = 0;
        dreadEntityId = -1;
    }

    /**
     * Check if cinematic is currently active.
     */
    public static boolean isCinematicActive() {
        return cinematicActive;
    }

    /**
     * Get yaw shake offset for Camera mixin.
     * Returns 0 if shake not active.
     */
    public static float getShakeYawOffset() {
        return cameraShake.getYawOffset();
    }

    /**
     * Get pitch shake offset for Camera mixin.
     * Returns 0 if shake not active.
     */
    public static float getShakePitchOffset() {
        return cameraShake.getPitchOffset();
    }
}
