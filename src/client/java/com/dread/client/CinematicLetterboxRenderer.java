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
     * Bars are solid black, instant appearance, no animation.
     */
    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!DeathCinematicClientHandler.isCinematicActive()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Top letterbox bar (solid black, instant appearance)
        context.fill(0, 0, width, BAR_HEIGHT, BAR_COLOR);

        // Bottom letterbox bar (solid black, instant appearance)
        context.fill(0, height - BAR_HEIGHT, width, height, BAR_COLOR);
    }
}
