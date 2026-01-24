package com.dread;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side initialization for the Dread mod.
 *
 * Entity renderer will be registered in Plan 02 (model/renderer implementation).
 * For now, entities will be invisible when spawned.
 */
public class DreadClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("dread-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Dread client...");

        // Entity renderer registration will be added in Plan 01-02
        // EntityRendererRegistry.register(ModEntities.DREAD, DreadRenderer::new);

        LOGGER.info("Dread client initialized successfully");
    }
}
