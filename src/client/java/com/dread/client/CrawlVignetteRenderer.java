package com.dread.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

/**
 * Renders blood vignette overlay when player is downed.
 * Creates urgent "wounded and dying" visual feedback.
 */
public class CrawlVignetteRenderer {

    // Use vanilla vignette texture with red tint
    private static final Identifier VIGNETTE_TEXTURE =
        Identifier.ofVanilla("textures/misc/vignette.png");

    /**
     * Register with HudRenderCallback during client initialization.
     */
    public static void register() {
        HudRenderCallback.EVENT.register(CrawlVignetteRenderer::render);
    }

    /**
     * Render blood vignette overlay.
     * Supports fade-in via DownedStateClientHandler.getShaderFadeIntensity().
     */
    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        // Only render when downed
        if (!DownedStateClientHandler.isDownedEffectActive()) {
            return;
        }

        // Get fade intensity for smooth transition
        float fadeIntensity = DownedStateClientHandler.getShaderFadeIntensity();
        if (fadeIntensity <= 0.0f) {
            return;  // Nothing to render yet
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Enable blending for transparency
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Set red tint with opacity scaled by fade intensity
        // Base opacity is 0.65, scaled by fade
        float alpha = 0.65f * fadeIntensity;
        RenderSystem.setShaderColor(1.0f, 0.15f, 0.15f, alpha);

        // Draw vignette texture covering full screen
        context.drawTexture(
            VIGNETTE_TEXTURE,
            0, 0,           // screen position
            0, 0,           // texture UV start
            width, height,  // render size
            width, height   // texture size (stretch to fit)
        );

        // Reset shader color to white
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
