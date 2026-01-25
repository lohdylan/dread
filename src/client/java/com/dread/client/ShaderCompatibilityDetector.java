package com.dread.client;

import com.dread.config.DreadConfigLoader;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects shader mods at runtime and determines if post-processing
 * effects should be disabled for compatibility.
 */
public class ShaderCompatibilityDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger("dread-client");

    private static boolean irisDetected = false;
    private static boolean optifineDetected = false;

    /**
     * Detect shader mods at startup.
     * Call once during client initialization.
     */
    public static void detect() {
        // Detect Iris (most common Fabric shader mod)
        irisDetected = FabricLoader.getInstance().isModLoaded("iris");

        // Detect Optifine (less common on Fabric, uses different mod ID)
        // Note: Optifine on Fabric typically uses OptiFabric with mod ID "optifabric"
        optifineDetected = FabricLoader.getInstance().isModLoaded("optifabric")
                        || FabricLoader.getInstance().isModLoaded("optifine");

        if (irisDetected) {
            LOGGER.info("Iris shader mod detected - post-processing fallback enabled");
        }
        if (optifineDetected) {
            LOGGER.info("OptiFine detected - post-processing fallback enabled");
        }
        if (!irisDetected && !optifineDetected) {
            LOGGER.info("No shader mods detected - full post-processing enabled");
        }
    }

    /**
     * Determine if post-processing effects should be disabled.
     *
     * @return true if effects should be disabled, false if safe to apply
     */
    public static boolean shouldDisablePostProcessing() {
        var config = DreadConfigLoader.getConfig();

        // Config override: force disable regardless of shader mods
        if (config.disableDownedEffects) {
            return true;
        }

        // Graceful fallback: disable if shader mods detected
        return irisDetected || optifineDetected;
    }

    /**
     * Check if Iris is specifically detected (for logging/debugging).
     */
    public static boolean isIrisDetected() {
        return irisDetected;
    }

    /**
     * Check if OptiFine is specifically detected (for logging/debugging).
     */
    public static boolean isOptifineDetected() {
        return optifineDetected;
    }
}
