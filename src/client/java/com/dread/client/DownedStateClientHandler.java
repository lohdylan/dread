package com.dread.client;

import ladysnake.satin.api.managed.ManagedShaderEffect;
import ladysnake.satin.api.managed.ShaderEffectManager;
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

    /**
     * Initializes the downed state shader effect.
     * Called during client initialization.
     */
    public static void register() {
        // Initialize shader effect through Satin API
        downedShader = ShaderEffectManager.getInstance().manage(DOWNED_SHADER);

        // Register shader rendering
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
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
     */
    public static void applyDownedEffects(int remainingTime) {
        isDownedEffectActive = true;
        remainingSeconds = remainingTime;
        LOGGER.info("Applied downed state effects ({}s remaining)", remainingTime);
    }

    /**
     * Removes all downed state effects.
     * Called when RemoveDownedEffectsS2C packet is received.
     */
    public static void removeDownedEffects() {
        isDownedEffectActive = false;
        remainingSeconds = 0;
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
}
