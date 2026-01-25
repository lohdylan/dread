package com.dread.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Renders world-space progress bar above downed players during revival.
 * Billboard effect - bar always faces camera.
 */
public class RevivalProgressRenderer {

    private static final Map<UUID, RevivalProgressData> activeRevivals = new HashMap<>();
    private static final float BAR_WIDTH = 1.0f;
    private static final float BAR_HEIGHT = 0.1f;
    private static final float Y_OFFSET = 2.5f; // Above player's head
    private static final int MAX_RENDER_DISTANCE = 16;
    private static final long STALE_THRESHOLD_MS = 2000; // 2 seconds without update

    /**
     * Register with WorldRenderEvents.AFTER_ENTITIES during client initialization.
     */
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(RevivalProgressRenderer::render);
    }

    /**
     * Update revival progress from network packet.
     */
    public static void updateRevivalProgress(UUID downedPlayerUUID, boolean active, float progress) {
        if (active) {
            activeRevivals.put(downedPlayerUUID, new RevivalProgressData(progress, System.currentTimeMillis()));
        } else {
            activeRevivals.remove(downedPlayerUUID);
        }
    }

    /**
     * Render progress bars for all active revivals.
     */
    private static void render(WorldRenderContext context) {
        MinecraftClient client = context.gameRenderer().getClient();
        if (client.world == null) return;

        // Clean up stale entries
        cleanupStaleEntries();

        // Render each active revival
        for (Map.Entry<UUID, RevivalProgressData> entry : activeRevivals.entrySet()) {
            UUID downedPlayerUUID = entry.getKey();
            RevivalProgressData data = entry.getValue();

            // Find the downed player entity
            PlayerEntity downedPlayer = null;
            for (PlayerEntity player : client.world.getPlayers()) {
                if (player.getUuid().equals(downedPlayerUUID)) {
                    downedPlayer = player;
                    break;
                }
            }

            if (downedPlayer == null) continue;

            // Check render distance
            double distance = client.player.squaredDistanceTo(downedPlayer);
            if (distance > MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE) continue;

            // Render progress bar
            renderProgressBar(context, downedPlayer, data.progress);
        }
    }

    /**
     * Render a single progress bar above a player.
     */
    private static void renderProgressBar(WorldRenderContext context, PlayerEntity player, float progress) {
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        matrices.push();

        // Position above player's head
        double x = player.getX();
        double y = player.getY() + player.getHeight() + Y_OFFSET;
        double z = player.getZ();

        // Translate to world position (camera space)
        matrices.translate(x - context.camera().getPos().x, y - context.camera().getPos().y, z - context.camera().getPos().z);

        // Billboard effect - rotate to face camera
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-context.camera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(context.camera().getPitch()));

        // Set up rendering state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Background (dark gray)
        float bgLeft = -BAR_WIDTH / 2;
        float bgRight = BAR_WIDTH / 2;
        float bgBottom = -BAR_HEIGHT / 2;
        float bgTop = BAR_HEIGHT / 2;

        buffer.vertex(matrix, bgLeft, bgBottom, 0).color(0, 0, 0, 200);
        buffer.vertex(matrix, bgLeft, bgTop, 0).color(0, 0, 0, 200);
        buffer.vertex(matrix, bgRight, bgTop, 0).color(0, 0, 0, 200);
        buffer.vertex(matrix, bgRight, bgBottom, 0).color(0, 0, 0, 200);

        // Progress fill (green)
        float fillWidth = BAR_WIDTH * progress;
        float fillLeft = -BAR_WIDTH / 2;
        float fillRight = fillLeft + fillWidth;
        float fillBottom = bgBottom + 0.01f; // Slight offset to prevent z-fighting
        float fillTop = bgTop - 0.01f;

        buffer.vertex(matrix, fillLeft, fillBottom, 0.01f).color(0, 255, 0, 255);
        buffer.vertex(matrix, fillLeft, fillTop, 0.01f).color(0, 255, 0, 255);
        buffer.vertex(matrix, fillRight, fillTop, 0.01f).color(0, 255, 0, 255);
        buffer.vertex(matrix, fillRight, fillBottom, 0.01f).color(0, 255, 0, 255);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Border (white)
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, bgLeft, bgBottom, 0.02f).color(255, 255, 255, 255);
        buffer.vertex(matrix, bgRight, bgBottom, 0.02f).color(255, 255, 255, 255);
        buffer.vertex(matrix, bgRight, bgTop, 0.02f).color(255, 255, 255, 255);
        buffer.vertex(matrix, bgLeft, bgTop, 0.02f).color(255, 255, 255, 255);
        buffer.vertex(matrix, bgLeft, bgBottom, 0.02f).color(255, 255, 255, 255);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Restore rendering state
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    /**
     * Remove entries older than 2 seconds (no update received).
     */
    private static void cleanupStaleEntries() {
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, RevivalProgressData>> iter = activeRevivals.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<UUID, RevivalProgressData> entry = iter.next();
            if (currentTime - entry.getValue().lastUpdateTime > STALE_THRESHOLD_MS) {
                iter.remove();
            }
        }
    }

    /**
     * Data holder for revival progress.
     */
    private static class RevivalProgressData {
        float progress;
        long lastUpdateTime;

        RevivalProgressData(float progress, long lastUpdateTime) {
            this.progress = progress;
            this.lastUpdateTime = lastUpdateTime;
        }
    }
}
