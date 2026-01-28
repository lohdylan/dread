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
 * Client-side handler for death cinematic (v2.2 - gentle horror).
 * Smooth, slow camera pull to third-person while watching Dread's grab animation.
 * No jarring movements - everything interpolates gently.
 *
 * Total duration: 4.5 seconds (90 ticks)
 */
public class DeathCinematicClientHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeathCinematicClientHandler.class);

    // Cinematic timing
    private static final int CINEMATIC_DURATION_TICKS = 90; // 4.5 seconds total
    private static final int PULLBACK_PHASE_TICKS = 30;     // First 1.5s for texture pulse

    // Camera positioning - gentle third-person pull
    private static final double MAX_PULLBACK_DISTANCE = 4.0;  // Final distance behind player
    private static final double MAX_PULLBACK_HEIGHT = 1.5;    // Final height above player
    private static final float CAMERA_LERP_SPEED = 0.02f;     // Very slow, smooth interpolation

    // Current interpolated values
    private static double currentPullbackProgress = 0.0;  // 0.0 = first-person, 1.0 = full third-person
    private static float currentYaw = 0;
    private static float currentPitch = 0;
    private static float startYaw = 0;
    private static float startPitch = 0;

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
     * Start the death cinematic (v2.2 - gentle horror).
     * Camera will slowly pull back while smoothly rotating to face Dread.
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
            startYaw = client.player.getYaw();
            startPitch = client.player.getPitch();
            currentYaw = startYaw;
            currentPitch = startPitch;
            currentPullbackProgress = 0.0;

            // Start cinematic
            cinematicActive = true;
            cinematicTimer = 0;

            LOGGER.debug("Death cinematic started (v2.2 - gentle horror)");
        }
    }

    /**
     * Tick the cinematic - smooth interpolation of camera position and rotation.
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

        // Smoothly increase pullback progress (ease-out curve)
        // Reaches ~95% at halfway point, then slows down
        double targetProgress = Math.min(1.0, (double) cinematicTimer / 60.0); // Full pullback over 3 seconds
        currentPullbackProgress = MathHelper.lerp(0.03, currentPullbackProgress, targetProgress);

        // Calculate target rotation to look at Dread
        Vec3d cameraPos = getCameraWorldPosition(client);
        Vec3d dreadCenter = dreadEntity.getPos().add(0, dreadEntity.getHeight() * 0.6, 0);
        Vec3d toTarget = dreadCenter.subtract(cameraPos).normalize();

        float targetYaw = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float targetPitch = (float) Math.toDegrees(-Math.asin(toTarget.y));

        // Very smooth rotation interpolation
        currentYaw = lerpAngle(currentYaw, targetYaw, CAMERA_LERP_SPEED);
        currentPitch = MathHelper.lerp(CAMERA_LERP_SPEED, currentPitch, MathHelper.clamp(targetPitch, -60, 60));

        // Apply rotation to player (camera follows)
        client.player.setYaw(currentYaw);
        client.player.setPitch(currentPitch);
        client.player.prevYaw = currentYaw;
        client.player.prevPitch = currentPitch;

        if (cinematicTimer >= CINEMATIC_DURATION_TICKS) {
            endCinematic();
        }
    }

    /**
     * Get the camera's world position based on current pullback progress.
     */
    private static Vec3d getCameraWorldPosition(MinecraftClient client) {
        if (client.player == null) return Vec3d.ZERO;

        Vec3d playerEyes = client.player.getEyePos();
        Vec3d offset = getCameraPositionOffset();
        return playerEyes.add(offset);
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
        currentPullbackProgress = 0.0;
        currentYaw = 0;
        currentPitch = 0;
        startYaw = 0;
        startPitch = 0;
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
     * Check if cinematic is past the pulse phase (for texture animation).
     * Eyes open after first 1.5 seconds.
     */
    public static boolean isInFaceCloseup() {
        return cinematicActive && cinematicTimer >= PULLBACK_PHASE_TICKS;
    }

    /**
     * Get yaw shake offset - returns 0 (no shake in gentle horror mode).
     */
    public static float getShakeYawOffset() {
        return 0.0f;
    }

    /**
     * Get pitch shake offset - returns 0 (no shake in gentle horror mode).
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
     * Get camera position offset for current cinematic state.
     * Smoothly interpolates from first-person (0,0,0) to third-person behind player.
     *
     * @return Position offset from player eye position
     */
    public static Vec3d getCameraPositionOffset() {
        if (!cinematicActive) {
            return Vec3d.ZERO;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return Vec3d.ZERO;
        }

        // Calculate offset behind and above player based on current progress
        double distance = MAX_PULLBACK_DISTANCE * currentPullbackProgress;
        double height = MAX_PULLBACK_HEIGHT * currentPullbackProgress;

        // Use current yaw to position camera behind where player is looking
        double yawRadians = Math.toRadians(currentYaw);
        double offsetX = Math.sin(yawRadians) * distance;
        double offsetZ = -Math.cos(yawRadians) * distance;

        return new Vec3d(offsetX, height, offsetZ);
    }
}
