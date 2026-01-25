package com.dread.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HUD overlay for downed state countdown timer.
 * Displays "DOWNED" label and MM:SS countdown timer centered on screen.
 * Timer color transitions from yellow to red as time decreases.
 */
public class DownedHudOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger("dread-client");

    // Colors
    private static final int COLOR_YELLOW = 0xFFFFFF00;
    private static final int COLOR_RED = 0xFFFF0000;

    /**
     * Registers the HUD overlay with Fabric API.
     * Called during client initialization.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(DownedHudOverlay::renderDownedHud);
        LOGGER.info("Registered DownedHudOverlay");
    }

    /**
     * Renders the downed state HUD overlay.
     * Only renders if player is currently downed.
     */
    private static void renderDownedHud(DrawContext drawContext, RenderTickCounter tickCounter) {
        if (!DownedStateClientHandler.isDownedEffectActive()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        int remainingSeconds = DownedStateClientHandler.getRemainingSeconds();

        // Calculate screen center
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Render "DOWNED" label above timer
        String downedLabel = "DOWNED";
        int labelWidth = client.textRenderer.getWidth(downedLabel);
        drawContext.drawText(
            client.textRenderer,
            downedLabel,
            centerX - labelWidth / 2,
            centerY - 30,
            COLOR_RED,
            true // shadow
        );

        // Format time as MM:SS
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        String timerText = String.format("%02d:%02d", minutes, seconds);

        // Calculate timer color (yellow to red based on time remaining)
        // Assuming max time is 300 seconds (5 minutes)
        float timeRatio = remainingSeconds / 300.0f;
        int timerColor = interpolateColor(COLOR_YELLOW, COLOR_RED, 1.0f - timeRatio);

        // Render countdown timer centered
        int timerWidth = client.textRenderer.getWidth(timerText);
        drawContext.drawText(
            client.textRenderer,
            timerText,
            centerX - timerWidth / 2,
            centerY - 10,
            timerColor,
            true // shadow
        );
    }

    /**
     * Interpolates between two ARGB colors.
     *
     * @param color1 Start color (ARGB)
     * @param color2 End color (ARGB)
     * @param ratio Interpolation ratio (0.0 = color1, 1.0 = color2)
     * @return Interpolated ARGB color
     */
    private static int interpolateColor(int color1, int color2, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio)); // Clamp to [0, 1]

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
