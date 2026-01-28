package com.dread.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders letterbox bars during death cinematic.
 * Bars appear instantly (no fade) to signal player control loss.
 * Creates cinematic film aesthetic that reinforces horror.
 */
public class CinematicLetterboxRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("dread-client");

    // Bar configuration
    private static final int BAR_HEIGHT = 60; // pixels (works for 720p-2160p)
    private static final int BAR_COLOR = 0xFF000000; // Solid black, full opacity

    /**
     * Register the letterbox renderer with Fabric HUD callback.
     * Called during client initialization.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(CinematicLetterboxRenderer::render);
        LOGGER.info("Registered CinematicLetterboxRenderer");
    }

    /**
     * Render letterbox bars if cinematic is active.
     * Currently disabled - user feedback indicated bars were too jarring.
     */
    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        // Letterbox bars disabled per user feedback
        // Keep method registered for potential future use
    }
}
