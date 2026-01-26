package com.dread.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * Renders visual compensation effects during death cinematic when camera shake is reduced.
 * Boosts red vignette and adds white flash to maintain horror impact for motion-sensitive players.
 */
public class CinematicCompensationRenderer {

    private static final Identifier VIGNETTE_TEXTURE =
        Identifier.ofVanilla("textures/misc/vignette.png");

    // Track compensation amount (0.0 = full shake, 1.0 = no shake)
    private static float compensationAmount = 0.0f;
    private static boolean isActive = false;
    private static int flashTimer = 0;
    private static final int FLASH_DURATION = 2; // 0.1s pulse (2 ticks)

    /**
     * Register with HudRenderCallback during client initialization.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(CinematicCompensationRenderer::render);
    }

    /**
     * Set compensation amount based on shake reduction.
     * Call from DeathCinematicClientHandler when shake starts.
     *
     * @param amount 0.0 (full shake, no compensation) to 1.0 (no shake, full compensation)
     */
    public static void setCompensation(float amount) {
        compensationAmount = Math.clamp(amount, 0.0f, 1.0f);
        isActive = amount > 0.0f;
        if (isActive) {
            flashTimer = FLASH_DURATION; // Start flash pulse
        }
    }

    /**
     * Stop rendering compensation (call when cinematic ends).
     */
    public static void stop() {
        compensationAmount = 0.0f;
        isActive = false;
        flashTimer = 0;
    }

    /**
     * Tick the flash timer.
     */
    public static void tick() {
        if (flashTimer > 0) {
            flashTimer--;
        }
    }

    /**
     * Render compensation effects.
     */
    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        // Only render during active cinematic with compensation needed
        if (!isActive || compensationAmount <= 0.0f) {
            return;
        }

        // Don't render if downed state vignette is active (avoid stacking)
        if (DownedStateClientHandler.isDownedEffectActive()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Boosted red vignette (more intense than normal)
        // Base opacity 0.4, boosted by up to 0.3 based on compensation
        float vignetteOpacity = 0.4f + (compensationAmount * 0.3f);
        RenderSystem.setShaderColor(1.0f, 0.15f, 0.15f, vignetteOpacity);
        context.drawTexture(
            VIGNETTE_TEXTURE,
            0, 0,
            0, 0,
            width, height,
            width, height
        );

        // White flash overlay (brief pulse at start)
        if (flashTimer > 0) {
            float flashOpacity = compensationAmount * 0.25f * (flashTimer / (float)FLASH_DURATION);
            // Draw white rectangle overlay
            int alpha = (int)(flashOpacity * 255);
            int color = (alpha << 24) | 0xFFFFFF; // ARGB white with alpha
            context.fill(0, 0, width, height, color);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
