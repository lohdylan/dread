package com.dread;

import com.dread.client.CinematicCompensationRenderer;
import com.dread.client.CrawlVignetteRenderer;
import com.dread.client.DeathCinematicClientHandler;
import com.dread.client.DownedHudOverlay;
import com.dread.client.DownedStateClientHandler;
import com.dread.client.DreadEntityRenderer;
import com.dread.client.RevivalProgressRenderer;
import com.dread.client.ShaderCompatibilityDetector;
import com.dread.network.packets.CinematicTriggerS2C;
import com.dread.network.packets.DownedStateUpdateS2C;
import com.dread.network.packets.RemoveDownedEffectsS2C;
import com.dread.network.packets.RevivalProgressS2C;
import com.dread.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side initialization for the Dread mod.
 *
 * Registers entity renderers for client-side rendering.
 */
public class DreadClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("dread-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Dread client...");

        // Detect shader mods for compatibility (before downed state handler)
        ShaderCompatibilityDetector.detect();

        // Register Dread entity renderer with GeckoLib
        EntityRendererRegistry.register(ModEntities.DREAD, DreadEntityRenderer::new);
        LOGGER.info("Registered DreadEntityRenderer with AutoGlowingGeoLayer");

        // Register death cinematic handler
        DeathCinematicClientHandler.register();

        // Register downed state handlers
        DownedStateClientHandler.register();
        DownedHudOverlay.register();
        CrawlVignetteRenderer.register();

        // Register cinematic compensation renderer
        CinematicCompensationRenderer.register();

        // Register revival progress renderer
        RevivalProgressRenderer.register();

        // Register packet receivers
        registerPacketReceivers();

        LOGGER.info("Dread client initialized successfully");
    }

    private void registerPacketReceivers() {
        // Cinematic trigger packet - starts death camera lock
        ClientPlayNetworking.registerGlobalReceiver(
            CinematicTriggerS2C.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    DeathCinematicClientHandler.startCinematic(payload);
                });
            }
        );

        // Downed state update packet - applies shader effects and updates timer
        ClientPlayNetworking.registerGlobalReceiver(
            DownedStateUpdateS2C.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    boolean isDowned = payload.isDowned();
                    int remainingSeconds = payload.remainingSeconds();
                    boolean isMercyMode = payload.isMercyMode();

                    if (isDowned) {
                        DownedStateClientHandler.applyDownedEffects(remainingSeconds, isMercyMode);
                    } else {
                        DownedStateClientHandler.updateCountdown(remainingSeconds);
                        DownedStateClientHandler.setMercyMode(isMercyMode);
                    }
                });
            }
        );

        // Remove downed effects packet - cleans up shader and HUD
        ClientPlayNetworking.registerGlobalReceiver(
            RemoveDownedEffectsS2C.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    DownedStateClientHandler.removeDownedEffects();
                });
            }
        );

        // Revival progress packet - updates world-space progress bar
        ClientPlayNetworking.registerGlobalReceiver(
            RevivalProgressS2C.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    RevivalProgressRenderer.updateRevivalProgress(
                        payload.downedPlayerUUID(),
                        payload.active(),
                        payload.progress()
                    );
                });
            }
        );

        LOGGER.info("Registered packet receivers for death cinematics and downed state");
    }
}
