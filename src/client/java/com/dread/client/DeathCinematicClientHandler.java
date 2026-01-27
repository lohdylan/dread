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
 * Client-side handler for death cinematic camera control (v2.0).
 * Two-phase cinematic with position-based camera control:
 * - THIRD_PERSON_PULLBACK (0-30 ticks / 1.5s): Camera behind and above player, framing both player and Dread
 * - FACE_CLOSEUP (30-90 ticks / 3.0s): Camera locked on Dread's face with eyes centered
 *
 * Total duration: 4.5 seconds (90 ticks)
 */
public class DeathCinematicClientHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeathCinematicClientHandler.class);

    // ===== Phase System =====

    /**
     * Cinematic phases for v2.0 camera control system.
     * Third-person pull-back followed by jump cut to face close-up.
     */
    private enum CinematicPhase {
        THIRD_PERSON_PULLBACK, // 0-30 ticks (1.5s): Camera behind and above player, framing both
        FACE_CLOSEUP           // 30-90 ticks (3.0s): Camera locked on Dread's face, eyes centered
    }

    // Phase tick boundaries
    private static final int PULLBACK_END_TICKS = 30;     // 1.5 seconds
    private static final int CINEMATIC_DURATION_TICKS = 90; // 4.5 seconds total

    // Camera positioning constants
    private static final double PULLBACK_DISTANCE = 5.0;  // Blocks behind player
    private static final double PULLBACK_HEIGHT = 2.0;    // Blocks above player
    private static final double FACE_DISTANCE = 0.4;      // Blocks from Dread's face (avoid clipping)

    // Yaw tracking during pull-back (smooth look toward Dread)
    private static final float PULLBACK_YAW_LERP = 0.08f;

    private static boolean cinematicActive = false;
    private static int cinematicTimer = 0;
    private static int dreadEntityId = -1;
    private static CinematicPhase currentPhase = CinematicPhase.THIRD_PERSON_PULLBACK;
    private static CinematicPhase previousPhase = null;

    // Target rotation to look at Dread (used during pull-back)
    private static float targetYaw = 0;
    private static float targetPitch = 0;
    // Current smoothed rotation
    private static float currentYaw = 0;
    private static float currentPitch = 0;

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
     * Start the death cinematic with v2.0 camera control.
     * Camera jumps to third-person immediately, pulls back, then jump-cuts to face close-up.
     *
     * @param payload Cinematic trigger packet from server
     */
    public static void startCinematic(CinematicTriggerS2C payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        // Find Dread entity in world
        dreadEntityId = payload.dreadEntityId();
        Entity dreadEntity = client.world.getEntityById(dreadEntityId);

        if (dreadEntity != null) {
            // Store current rotation as starting point
            currentYaw = client.player.getYaw();
            currentPitch = client.player.getPitch();

            // Calculate direction from player to Dread for yaw tracking
            Vec3d playerEyePos = client.player.getEyePos();
            Vec3d dreadPos = dreadEntity.getPos().add(0, dreadEntity.getHeight() / 2, 0);
            Vec3d direction = dreadPos.subtract(playerEyePos).normalize();

            targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            targetPitch = (float) Math.toDegrees(-Math.asin(direction.y));

            // Trigger death_grab animation on entity
            if (dreadEntity instanceof DreadEntity dread) {
                dread.setPlayingDeathGrab(true);
            }

            // Initialize v2.0 phase system
            currentPhase = CinematicPhase.THIRD_PERSON_PULLBACK;

            // Start cinematic timer
            cinematicActive = true;
            cinematicTimer = 0;
        }
    }

    /**
     * Tick the cinematic timer and update phase.
     * Camera position/rotation controlled via mixins reading getCameraPositionOffset().
     */
    private static void tick() {
        cinematicTimer++;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Update current phase based on timer
        updatePhase();

        // Phase-specific camera rotation control
        if (currentPhase == CinematicPhase.THIRD_PERSON_PULLBACK) {
            // During pull-back, smoothly rotate player to look at Dread
            Entity dreadEntity = client.world.getEntityById(dreadEntityId);
            if (dreadEntity != null) {
                // Recalculate direction to Dread
                Vec3d playerEyePos = client.player.getEyePos();
                Vec3d dreadPos = dreadEntity.getPos().add(0, dreadEntity.getHeight() / 2, 0);
                Vec3d direction = dreadPos.subtract(playerEyePos).normalize();

                targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
                targetPitch = (float) Math.toDegrees(-Math.asin(direction.y));

                // Smooth yaw tracking
                currentYaw = lerpAngle(currentYaw, targetYaw, PULLBACK_YAW_LERP);
                currentPitch = MathHelper.lerp(PULLBACK_YAW_LERP, currentPitch, targetPitch);

                // Apply to player
                client.player.setYaw(currentYaw);
                client.player.setPitch(currentPitch);
                client.player.prevYaw = currentYaw;
                client.player.prevPitch = currentPitch;
            }
        } else if (currentPhase == CinematicPhase.FACE_CLOSEUP) {
            // During face close-up, lock camera rotation on Dread's eyes (frozen terror aesthetic)
            float[] rotation = getFaceCloseupRotation();
            if (rotation != null) {
                client.player.setYaw(rotation[0]);
                client.player.setPitch(rotation[1]);
                client.player.prevYaw = rotation[0];
                client.player.prevPitch = rotation[1];
            }
        }

        if (cinematicTimer >= CINEMATIC_DURATION_TICKS) {
            endCinematic();
        }
    }

    /**
     * Update the current cinematic phase based on timer.
     */
    private static void updatePhase() {
        CinematicPhase newPhase;
        if (cinematicTimer < PULLBACK_END_TICKS) {
            newPhase = CinematicPhase.THIRD_PERSON_PULLBACK;
        } else {
            newPhase = CinematicPhase.FACE_CLOSEUP;
        }

        // Log phase transitions
        if (previousPhase != newPhase) {
            LOGGER.debug("Cinematic phase transition: {} -> {} at tick {}",
                previousPhase, newPhase, cinematicTimer);
            previousPhase = currentPhase;
        }

        currentPhase = newPhase;
    }

    /**
     * Calculate camera position offset for third-person pull-back.
     * Camera positioned behind and above player.
     */
    private static Vec3d calculatePullbackPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return Vec3d.ZERO;
        }

        // Get player's yaw (horizontal rotation)
        float yaw = client.player.getYaw();
        double yawRadians = Math.toRadians(yaw);

        // Calculate offset behind player
        // In Minecraft: yaw 0 = south (-Z), yaw 90 = west (-X)
        double offsetX = -Math.sin(yawRadians) * PULLBACK_DISTANCE;
        double offsetZ = Math.cos(yawRadians) * PULLBACK_DISTANCE;
        double offsetY = PULLBACK_HEIGHT;

        return new Vec3d(offsetX, offsetY, offsetZ);
    }

    /**
     * Calculate camera position for face close-up.
     * Camera positioned just in front of Dread's eyes, looking back at player.
     */
    private static Vec3d calculateFaceCloseupPosition() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return Vec3d.ZERO;
        }

        // Get Dread entity
        Entity dreadEntity = client.world.getEntityById(dreadEntityId);
        if (dreadEntity == null) {
            return Vec3d.ZERO;
        }

        // Get positions
        Vec3d dreadEyes = dreadEntity.getEyePos();
        Vec3d playerPos = client.player.getPos();

        // Calculate direction from Dread to player (normalized)
        Vec3d direction = playerPos.subtract(dreadEyes).normalize();

        // Position camera just in front of Dread's face, looking back toward player
        Vec3d cameraTarget = dreadEyes.add(direction.multiply(FACE_DISTANCE));

        // Return offset from player position
        return cameraTarget.subtract(playerPos);
    }

    /**
     * Get camera rotation to look at Dread's eyes during face close-up.
     * Returns null if not in face close-up phase.
     *
     * @return [yaw, pitch] array or null if not in close-up phase
     */
    public static float[] getFaceCloseupRotation() {
        if (!cinematicActive || currentPhase != CinematicPhase.FACE_CLOSEUP) {
            return null;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return null;

        Entity dread = client.world.getEntityById(dreadEntityId);
        if (dread == null) return null;

        // Camera looks FROM close-up position TO Dread's eyes
        Vec3d cameraPos = client.player.getEyePos().add(getCameraPositionOffset());
        Vec3d dreadEyes = dread.getEyePos();
        Vec3d direction = dreadEyes.subtract(cameraPos).normalize();

        // Convert to yaw/pitch
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(-Math.asin(direction.y));

        return new float[]{yaw, pitch};
    }

    /**
     * Lerp between angles, handling wrap-around at 180/-180.
     */
    private static float lerpAngle(float from, float to, float factor) {
        float diff = MathHelper.wrapDegrees(to - from);
        return from + diff * factor;
    }

    /**
     * End the cinematic - release camera control and stop animations.
     */
    private static void endCinematic() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Log cinematic duration
        LOGGER.debug("Cinematic ended after {} ticks ({} seconds)",
            cinematicTimer, cinematicTimer / 20.0f);

        // Stop death_grab animation on entity
        if (client.world != null && dreadEntityId != -1) {
            Entity entity = client.world.getEntityById(dreadEntityId);
            if (entity instanceof DreadEntity dread) {
                dread.setPlayingDeathGrab(false);
            }
        }

        // Camera position/rotation control released - player can move camera again

        // Reset cinematic state
        cinematicActive = false;
        cinematicTimer = 0;
        dreadEntityId = -1;
        currentPhase = CinematicPhase.THIRD_PERSON_PULLBACK;
        previousPhase = null;
        targetYaw = 0;
        targetPitch = 0;
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
     *
     * @return Timer tick (0-90) or -1 if cinematic inactive
     */
    public static int getCinematicTimer() {
        return cinematicActive ? cinematicTimer : -1;
    }

    /**
     * Check if cinematic is in face close-up phase.
     * Used by texture animation to trigger eye reveal.
     *
     * @return true if in FACE_CLOSEUP phase, false otherwise
     */
    public static boolean isInFaceCloseup() {
        return cinematicActive && currentPhase == CinematicPhase.FACE_CLOSEUP;
    }

    /**
     * Get yaw shake offset for Camera mixin.
     * Returns 0 - death grab cinematic uses phased motion, not random shake.
     */
    public static float getShakeYawOffset() {
        // Phased cinematic system handles all camera motion - no shake needed
        return 0.0f;
    }

    /**
     * Get pitch shake offset for Camera mixin.
     * Returns 0 - death grab cinematic uses phased motion, not random shake.
     */
    public static float getShakePitchOffset() {
        // Phased cinematic system handles all camera motion - no shake needed
        return 0.0f;
    }

    /**
     * Get locked yaw for camera enforcement during cinematic.
     */
    public static float getLockedYaw() {
        return targetYaw;
    }

    /**
     * Get locked pitch for camera enforcement during cinematic.
     */
    public static float getLockedPitch() {
        return targetPitch;
    }

    /**
     * Get the Dread entity ID being watched during cinematic.
     */
    public static int getDreadEntityId() {
        return dreadEntityId;
    }

    /**
     * Get camera position offset for current cinematic phase.
     * Called by CameraMixin to position camera during cinematic.
     *
     * @return Position offset from player location
     */
    public static Vec3d getCameraPositionOffset() {
        if (!cinematicActive) {
            return Vec3d.ZERO;
        }

        return switch (currentPhase) {
            case THIRD_PERSON_PULLBACK -> calculatePullbackPosition();
            case FACE_CLOSEUP -> calculateFaceCloseupPosition();
        };
    }
}
