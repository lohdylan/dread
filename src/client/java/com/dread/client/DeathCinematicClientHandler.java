package com.dread.client;

import com.dread.entity.DreadEntity;
import com.dread.network.packets.CinematicTriggerS2C;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Client-side handler for death cinematic camera lock.
 * Uses a phased camera system matching the death_grab animation (1.8s / 36 ticks):
 * - IMPACT (0-3 ticks): Brief downward dip simulating being grabbed
 * - LIFT (3-14 ticks): Camera smoothly raises, feeling of being lifted
 * - HOLD (14-30 ticks): Stable view of Dread's face - face-to-face horror
 * - RELEASE (30-36 ticks): Camera settles as player is released
 */
public class DeathCinematicClientHandler {

    // ===== Phase System =====

    /**
     * Cinematic phases matching death_grab animation timing.
     */
    private enum CinematicPhase {
        IMPACT,   // 0-3 ticks (0.15s): grab impact - quick downward dip
        LIFT,     // 3-14 ticks (0.15s-0.7s): being lifted - slow pitch raise
        HOLD,     // 14-30 ticks (0.7s-1.5s): face-to-face - stable locked view
        RELEASE   // 30-36 ticks (1.5s-1.8s): released - gradual settle
    }

    // Phase tick boundaries
    private static final int IMPACT_END = 3;      // 0.15s
    private static final int LIFT_END = 14;       // 0.7s
    private static final int HOLD_END = 30;       // 1.5s
    private static final int CINEMATIC_DURATION_TICKS = 36; // 1.8s (cinematic end)

    // Phase-specific camera motion parameters
    private static final float IMPACT_DIP_DEGREES = 5.0f;   // Downward dip on grab
    private static final float LIFT_RAISE_DEGREES = 18.0f;  // How much pitch raises during lift
    private static final float RELEASE_LOWER_DEGREES = 5.0f; // Gradual lower on release

    // Yaw lerp speeds per phase (higher = faster snap to target)
    private static final float IMPACT_YAW_LERP = 0.25f;  // Fast snap toward Dread
    private static final float LIFT_YAW_LERP = 0.12f;    // Smooth tracking
    private static final float HOLD_YAW_LERP = 0.08f;    // Very smooth, stable
    private static final float RELEASE_YAW_LERP = 0.05f; // Gradual settle

    // Keep CameraShakeHandler for potential future use, but not used during death grab
    @SuppressWarnings("unused")
    private static final CameraShakeHandler cameraShake = new CameraShakeHandler();

    private static boolean cinematicActive = false;
    private static int cinematicTimer = 0;
    private static int dreadEntityId = -1;
    private static CinematicPhase currentPhase = CinematicPhase.IMPACT;

    // Target rotation to look at Dread
    private static float targetYaw = 0;
    private static float targetPitch = 0;
    // Current smoothed rotation
    private static float currentYaw = 0;
    private static float currentPitch = 0;

    // Phase-specific tracking
    private static float initialPitch = 0;        // Player's pitch when cinematic started
    private static float basePitchOffset = 0;     // Accumulated pitch offset from phases
    private static float impactDipProgress = 0;   // Track impact dip animation (0 to 1 and back)

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
     * Start the death cinematic - force player camera to look at Dread.
     * Player stays as camera entity but their view is locked onto Dread.
     * Uses phased camera motion for smooth, deliberate "grabbed and lifted" feel.
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
            // Store current rotation as starting point for smooth interpolation
            currentYaw = client.player.getYaw();
            currentPitch = client.player.getPitch();
            initialPitch = currentPitch;

            // Calculate direction from player to Dread (so player SEES Dread)
            Vec3d playerEyePos = client.player.getEyePos();
            Vec3d dreadPos = dreadEntity.getPos().add(0, dreadEntity.getHeight() / 2, 0); // Aim at Dread's center
            Vec3d direction = dreadPos.subtract(playerEyePos).normalize();

            // Convert to yaw/pitch
            targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            targetPitch = (float) Math.toDegrees(-Math.asin(direction.y));

            // Trigger death_grab animation on entity
            if (dreadEntity instanceof DreadEntity dread) {
                dread.setPlayingDeathGrab(true);
            }

            // Initialize phase system - no random shake, just smooth phased motion
            currentPhase = CinematicPhase.IMPACT;
            basePitchOffset = 0;
            impactDipProgress = 0;

            // Start cinematic timer
            cinematicActive = true;
            cinematicTimer = 0;
        }
    }

    /**
     * Tick the cinematic timer and apply phased camera motion.
     * Each phase has distinct camera behavior matching the death_grab animation.
     */
    private static void tick() {
        cinematicTimer++;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Update current phase based on timer
        updatePhase();

        // Keep player looking at Dread during cinematic
        Entity dreadEntity = client.world.getEntityById(dreadEntityId);
        if (dreadEntity != null) {
            // Recalculate direction to Dread (in case either moved)
            Vec3d playerEyePos = client.player.getEyePos();
            Vec3d dreadPos = dreadEntity.getPos().add(0, dreadEntity.getHeight() / 2, 0);
            Vec3d direction = dreadPos.subtract(playerEyePos).normalize();

            targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            targetPitch = (float) Math.toDegrees(-Math.asin(direction.y));

            // Apply phase-specific camera motion
            applyPhaseMotion();

            // Apply final rotation to player
            client.player.setYaw(currentYaw);
            client.player.setPitch(currentPitch + basePitchOffset);
            client.player.prevYaw = currentYaw;
            client.player.prevPitch = currentPitch + basePitchOffset;
        }

        if (cinematicTimer >= CINEMATIC_DURATION_TICKS) {
            endCinematic();
        }
    }

    /**
     * Update the current cinematic phase based on timer.
     */
    private static void updatePhase() {
        if (cinematicTimer <= IMPACT_END) {
            currentPhase = CinematicPhase.IMPACT;
        } else if (cinematicTimer <= LIFT_END) {
            currentPhase = CinematicPhase.LIFT;
        } else if (cinematicTimer <= HOLD_END) {
            currentPhase = CinematicPhase.HOLD;
        } else {
            currentPhase = CinematicPhase.RELEASE;
        }
    }

    /**
     * Apply camera motion specific to current phase.
     */
    private static void applyPhaseMotion() {
        switch (currentPhase) {
            case IMPACT -> applyImpactMotion();
            case LIFT -> applyLiftMotion();
            case HOLD -> applyHoldMotion();
            case RELEASE -> applyReleaseMotion();
        }
    }

    /**
     * IMPACT phase (ticks 0-3): Quick downward dip then recovery.
     * Simulates being grabbed - fast, visceral but controlled.
     */
    private static void applyImpactMotion() {
        // Progress through impact phase (0 to 1)
        float phaseProgress = (float) cinematicTimer / IMPACT_END;

        // Dip down quickly then start recovering
        // Use sine curve: peaks at middle of phase, returns toward 0
        impactDipProgress = (float) Math.sin(phaseProgress * Math.PI);
        basePitchOffset = IMPACT_DIP_DEGREES * impactDipProgress;

        // Fast yaw snap toward Dread
        currentYaw = lerpAngle(currentYaw, targetYaw, IMPACT_YAW_LERP);
        currentPitch = MathHelper.lerp(IMPACT_YAW_LERP, currentPitch, targetPitch);
    }

    /**
     * LIFT phase (ticks 3-14): Camera smoothly raises.
     * Feeling of being lifted up by Dread - slow, deliberate with ease-out.
     */
    private static void applyLiftMotion() {
        // Progress through lift phase (0 to 1)
        float phaseProgress = (float) (cinematicTimer - IMPACT_END) / (LIFT_END - IMPACT_END);

        // Apply ease-out for natural deceleration (fast start, slow end)
        float easedProgress = easeOut(phaseProgress);

        // Raise pitch (looking more upward at Dread holding us)
        // Negative pitch = looking up in Minecraft
        basePitchOffset = -LIFT_RAISE_DEGREES * easedProgress;

        // Smooth yaw tracking
        currentYaw = lerpAngle(currentYaw, targetYaw, LIFT_YAW_LERP);
        currentPitch = MathHelper.lerp(LIFT_YAW_LERP, currentPitch, targetPitch);
    }

    /**
     * HOLD phase (ticks 14-30): Completely stable view locked on Dread.
     * Face-to-face horror moment - stillness creates tension.
     */
    private static void applyHoldMotion() {
        // Maintain lift offset - no additional motion
        basePitchOffset = -LIFT_RAISE_DEGREES;

        // Very smooth, stable tracking
        currentYaw = lerpAngle(currentYaw, targetYaw, HOLD_YAW_LERP);
        currentPitch = MathHelper.lerp(HOLD_YAW_LERP, currentPitch, targetPitch);
    }

    /**
     * RELEASE phase (ticks 30-36): Camera slowly settles.
     * Being released/dropped - smooth ease-out to downed position.
     */
    private static void applyReleaseMotion() {
        // Progress through release phase (0 to 1)
        float phaseProgress = (float) (cinematicTimer - HOLD_END) / (CINEMATIC_DURATION_TICKS - HOLD_END);

        // Apply ease-in-out for smooth settle
        float easedProgress = easeInOut(phaseProgress);

        // Transition from lifted position to slight downward (being dropped)
        // Start at -LIFT_RAISE_DEGREES, end at +RELEASE_LOWER_DEGREES
        float startOffset = -LIFT_RAISE_DEGREES;
        float endOffset = RELEASE_LOWER_DEGREES;
        basePitchOffset = MathHelper.lerp(easedProgress, startOffset, endOffset);

        // Gradual, gentle tracking
        currentYaw = lerpAngle(currentYaw, targetYaw, RELEASE_YAW_LERP);
        currentPitch = MathHelper.lerp(RELEASE_YAW_LERP, currentPitch, targetPitch);
    }

    // ===== Easing Functions =====

    /**
     * Ease-out: fast start, slow end.
     * Good for impact recovery and lift deceleration.
     */
    private static float easeOut(float t) {
        return 1.0f - (1.0f - t) * (1.0f - t);
    }

    /**
     * Ease-in-out: slow start, fast middle, slow end.
     * Good for smooth transitions like release settle.
     */
    private static float easeInOut(float t) {
        return t < 0.5f
            ? 2.0f * t * t
            : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 2) / 2.0f;
    }

    /**
     * Lerp between angles, handling wrap-around at 180/-180.
     */
    private static float lerpAngle(float from, float to, float factor) {
        float diff = MathHelper.wrapDegrees(to - from);
        return from + diff * factor;
    }

    /**
     * End the cinematic - release camera lock and stop animations.
     */
    private static void endCinematic() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Stop death_grab animation on entity
        if (client.world != null && dreadEntityId != -1) {
            Entity entity = client.world.getEntityById(dreadEntityId);
            if (entity instanceof DreadEntity dread) {
                dread.setPlayingDeathGrab(false);
            }
        }

        // Camera was never switched away from player, so no restoration needed.
        // Player regains control of their camera rotation.

        // NOTE: Don't apply downed effects here - server sends DownedStateUpdateS2C packets
        // to sync state. Calling applyDownedEffects() with defaults would overwrite the
        // correct server-synced state (wrong timer, wrong mercy mode).

        // Reset cinematic state
        cinematicActive = false;
        cinematicTimer = 0;
        dreadEntityId = -1;
        currentPhase = CinematicPhase.IMPACT;
        targetYaw = 0;
        targetPitch = 0;
        currentYaw = 0;
        currentPitch = 0;
        initialPitch = 0;
        basePitchOffset = 0;
        impactDipProgress = 0;
    }

    /**
     * Check if cinematic is currently active.
     */
    public static boolean isCinematicActive() {
        return cinematicActive;
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
}
