package com.dread.client;

import com.dread.entity.DreadEntity;
import com.dread.network.packets.CinematicTriggerS2C;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side handler for death cinematic (v3.1 - simple first-person).
 * Stays in first-person. Camera smoothly locks onto Dread's face.
 * Mouse input is blocked by MouseInputMixin during cinematic.
 *
 * Total duration: 6 seconds (120 ticks)
 */
public class DeathCinematicClientHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeathCinematicClientHandler.class);

    // Cinematic timing
    private static final int CINEMATIC_DURATION_TICKS = 120; // 6 seconds total helplessness
    private static final int DREAD_ANIMATION_TICKS = 100;    // 5 seconds - Dread attacks then leaves
    private static final int STARE_PHASE_START = 24;         // 1.2s - eyes open texture during grab/hold phase

    // Camera control - smooth but not too slow
    private static final float CAMERA_LERP_SPEED = 0.1f;

    // Current interpolated camera values
    private static float currentYaw = 0;
    private static float currentPitch = 0;

    private static boolean cinematicActive = false;
    private static int cinematicTimer = 0;
    private static int dreadEntityId = -1;

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
     * Start the death cinematic.
     * Camera locks onto Dread's face. Mouse input blocked by mixin.
     *
     * @param payload Cinematic trigger packet from server
     */
    public static void startCinematic(CinematicTriggerS2C payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        dreadEntityId = payload.dreadEntityId();
        Entity dreadEntity = client.world.getEntityById(dreadEntityId);

        if (dreadEntity != null) {
            // Trigger death_grab animation on entity
            if (dreadEntity instanceof DreadEntity dread) {
                dread.setPlayingDeathGrab(true);
            }

            // Store starting camera rotation
            currentYaw = client.player.getYaw();
            currentPitch = client.player.getPitch();

            // Start cinematic
            cinematicActive = true;
            cinematicTimer = 0;

            LOGGER.debug("Death cinematic started (v3.1 - simple first-person, 6 seconds)");
        }
    }

    /**
     * Tick the cinematic - smoothly look at Dread's face.
     */
    private static void tick() {
        cinematicTimer++;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            endCinematic();
            return;
        }

        Entity dreadEntity = client.world.getEntityById(dreadEntityId);
        if (dreadEntity == null) {
            endCinematic();
            return;
        }

        // Look at Dread's face (upper center of entity)
        Vec3d dreadFace = dreadEntity.getPos().add(0, dreadEntity.getHeight() * 0.7, 0);
        Vec3d playerEyes = client.player.getEyePos();
        Vec3d toTarget = dreadFace.subtract(playerEyes).normalize();

        float targetYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float targetPitch = (float) Math.toDegrees(-Math.asin(toTarget.y));

        // Smooth rotation interpolation
        currentYaw = lerpAngle(currentYaw, targetYaw, CAMERA_LERP_SPEED);
        currentPitch = MathHelper.lerp(CAMERA_LERP_SPEED, currentPitch, MathHelper.clamp(targetPitch, -80, 80));

        // Apply rotation to player
        client.player.setYaw(currentYaw);
        client.player.setPitch(currentPitch);
        client.player.prevYaw = currentYaw;
        client.player.prevPitch = currentPitch;

        if (cinematicTimer >= CINEMATIC_DURATION_TICKS) {
            endCinematic();
        }
    }

    /**
     * Lerp between angles, handling wrap-around at 180/-180.
     */
    private static float lerpAngle(float from, float to, float factor) {
        float diff = MathHelper.wrapDegrees(to - from);
        return from + diff * factor;
    }

    /**
     * End the cinematic - stop animations and reset state.
     */
    private static void endCinematic() {
        MinecraftClient client = MinecraftClient.getInstance();

        LOGGER.debug("Cinematic ended after {} ticks ({} seconds)",
            cinematicTimer, cinematicTimer / 20.0f);

        // Stop death_grab animation on entity
        if (client.world != null && dreadEntityId != -1) {
            Entity entity = client.world.getEntityById(dreadEntityId);
            if (entity instanceof DreadEntity dread) {
                dread.setPlayingDeathGrab(false);
            }
        }

        // Reset cinematic state
        cinematicActive = false;
        cinematicTimer = 0;
        dreadEntityId = -1;
        currentYaw = 0;
        currentPitch = 0;
    }

    /**
     * Check if cinematic is currently active.
     */
    public static boolean isCinematicActive() {
        return cinematicActive;
    }

    /**
     * Get current cinematic timer tick for texture animation synchronization.
     * Returns -1 if cinematic not active.
     */
    public static int getCinematicTimer() {
        return cinematicActive ? cinematicTimer : -1;
    }

    /**
     * Check if cinematic is in the stare phase (for texture animation).
     * Eyes open texture kicks in at 3 seconds.
     */
    public static boolean isInFaceCloseup() {
        return cinematicActive && cinematicTimer >= STARE_PHASE_START;
    }

    /**
     * Get yaw shake offset - returns 0 (no shake).
     */
    public static float getShakeYawOffset() {
        return 0.0f;
    }

    /**
     * Get pitch shake offset - returns 0 (no shake).
     */
    public static float getShakePitchOffset() {
        return 0.0f;
    }

    /**
     * Get locked yaw for camera enforcement.
     */
    public static float getLockedYaw() {
        return currentYaw;
    }

    /**
     * Get locked pitch for camera enforcement.
     */
    public static float getLockedPitch() {
        return currentPitch;
    }

    /**
     * Get the Dread entity ID being watched during cinematic.
     */
    public static int getDreadEntityId() {
        return dreadEntityId;
    }

    /**
     * Get camera position offset - returns zero for first-person mode.
     *
     * @return Always Vec3d.ZERO for first-person
     */
    public static Vec3d getCameraPositionOffset() {
        return Vec3d.ZERO;
    }
}
