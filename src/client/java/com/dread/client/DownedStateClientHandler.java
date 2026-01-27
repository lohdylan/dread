package com.dread.client;

import org.ladysnake.satin.api.managed.ManagedShaderEffect;
import org.ladysnake.satin.api.managed.ShaderEffectManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side handler for managing downed state visual effects.
 * Applies heavy blur and vignette post-processing shader when player is downed.
 * Uses Satin API for shader management.
 */
public class DownedStateClientHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("dread-client");
    private static final Identifier DOWNED_SHADER = Identifier.of("dread", "shaders/post/downed_blur.json");

    private static ManagedShaderEffect downedShader;
    private static boolean isDownedEffectActive = false;
    private static int remainingSeconds = 0;
    private static boolean isMercyMode = false;

    /**
     * Initializes the downed state shader effect.
     * Called during client initialization.
     */
    public static void register() {
        // Initialize shader effect through Satin API
        downedShader = ShaderEffectManager.getInstance().manage(DOWNED_SHADER);

        // Register shader rendering at END to capture everything including clouds
        WorldRenderEvents.END.register(context -> {
            // Skip shader if compatibility mode or disabled by config
            if (ShaderCompatibilityDetector.shouldDisablePostProcessing()) {
                return;
            }

            if (isDownedEffectActive && downedShader != null) {
                downedShader.render(context.tickCounter().getTickDelta(true));
            }
        });

        LOGGER.info("Registered DownedStateClientHandler with Satin shader");
    }

    /**
     * Applies the downed state shader effect.
     * Called when DownedStateUpdateS2C packet is received with isDowned=true.
     *
     * @param remainingTime Remaining seconds until permanent death
     * @param mercyMode Whether player is in MERCY mode (singleplayer forgiveness)
     */
    public static void applyDownedEffects(int remainingTime, boolean mercyMode) {
        isDownedEffectActive = true;
        remainingSeconds = remainingTime;
        isMercyMode = mercyMode;

        if (ShaderCompatibilityDetector.shouldDisablePostProcessing()) {
            LOGGER.info("Applied downed state ({}s remaining, {}) - shader effects disabled for compatibility",
                remainingTime, mercyMode ? "MERCY" : "NO MERCY");
        } else {
            LOGGER.info("Applied downed state effects ({}s remaining, {})",
                remainingTime, mercyMode ? "MERCY" : "NO MERCY");
        }
    }

    /**
     * Applies the downed state shader effect with default mercy mode.
     * Called when DownedStateUpdateS2C packet is received with isDowned=true.
     *
     * @param remainingTime Remaining seconds until permanent death
     */
    public static void applyDownedEffects(int remainingTime) {
        applyDownedEffects(remainingTime, false); // Default to NO MERCY
    }

    /**
     * Applies the downed state shader effect with default timer.
     * Called from death cinematic when transitioning to downed state.
     */
    public static void applyDownedEffects() {
        applyDownedEffects(300); // Default 300 seconds from DOWNED_DURATION_TICKS
    }

    /**
     * Removes all downed state effects.
     * Called when RemoveDownedEffectsS2C packet is received.
     */
    public static void removeDownedEffects() {
        isDownedEffectActive = false;
        remainingSeconds = 0;
        isMercyMode = false;
        LOGGER.info("Removed downed state effects");
    }

    /**
     * Updates the countdown timer.
     * Called on tick or when DownedStateUpdateS2C packet received.
     *
     * @param remainingTime Updated remaining seconds
     */
    public static void updateCountdown(int remainingTime) {
        remainingSeconds = remainingTime;
    }

    /**
     * Returns the current remaining seconds.
     * Used by HUD overlay for countdown timer.
     */
    public static int getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * Returns whether downed effects are currently active.
     * Used by HUD overlay to determine if timer should be shown.
     */
    public static boolean isDownedEffectActive() {
        return isDownedEffectActive;
    }

    /**
     * Returns whether player is in MERCY mode (singleplayer forgiveness).
     * Used by HUD overlay to determine mode indicator and timer color.
     */
    public static boolean isMercyMode() {
        return isMercyMode;
    }

    /**
     * Sets mercy mode state (for live mode transitions).
     * Called when server sends updated mercy mode during gameplay.
     */
    public static void setMercyMode(boolean mercyMode) {
        isMercyMode = mercyMode;
    }
}
