package com.dread.client;

import com.dread.config.DreadConfigLoader;
import com.dread.network.packets.CinematicTriggerS2C;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

/**
 * Client-side handler for death cinematic camera lock.
 * Locks camera onto Dread entity for 4.5 seconds before transitioning to downed state.
 */
public class DeathCinematicClientHandler {

    private static final int CINEMATIC_DURATION_TICKS = 90; // 4.5 seconds

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

            // Start cinematic timer
            cinematicActive = true;
            cinematicTimer = 0;

            // Get shake intensity from config (0-100 -> 0.0-1.0)
            var config = DreadConfigLoader.getConfig();
            float intensity = Math.clamp(config.cameraShakeIntensity, 0, 100) / 100.0f;
            cameraShake.startShake(intensity);
        }
    }

    /**
     * Tick the cinematic timer and check if duration is complete.
     */
    private static void tick() {
        cinematicTimer++;

        MinecraftClient client = MinecraftClient.getInstance();
        Entity cameraEntity = client.getCameraEntity();

        // Update shake (deltaTime = 1 tick = 0.05 seconds)
        cameraShake.tick(0.05f);

        // Apply shake offset to camera entity rotation
        if (cameraEntity != null && cameraShake.isActive()) {
            float baseYaw = cameraEntity.getYaw();
            float basePitch = cameraEntity.getPitch();
            cameraEntity.setYaw(baseYaw + cameraShake.getYawOffset());
            cameraEntity.setPitch(basePitch + cameraShake.getPitchOffset());
        }

        if (cinematicTimer >= CINEMATIC_DURATION_TICKS) {
            endCinematic();
        }
    }

    /**
     * End the cinematic - restore camera to player and apply downed effects.
     */
    private static void endCinematic() {
        MinecraftClient client = MinecraftClient.getInstance();

        // CRITICAL: Reset shake before ending
        cameraShake.reset();

        // Restore camera to original entity (player)
        if (originalCameraEntity != null) {
            client.setCameraEntity(originalCameraEntity);
            originalCameraEntity = null;
        }

        // Apply downed state effects
        DownedStateClientHandler.applyDownedEffects();

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
}
