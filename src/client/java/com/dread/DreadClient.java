package com.dread;

import com.dread.client.DreadEntityRenderer;
import com.dread.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
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

        // Register Dread entity renderer with GeckoLib
        EntityRendererRegistry.register(ModEntities.DREAD, DreadEntityRenderer::new);
        LOGGER.info("Registered DreadEntityRenderer with AutoGlowingGeoLayer");

        LOGGER.info("Dread client initialized successfully");
    }
}
