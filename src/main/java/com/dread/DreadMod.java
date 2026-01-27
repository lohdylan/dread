package com.dread;

import com.dread.config.DreadConfigLoader;
import com.dread.death.AttackPreventionHandler;
import com.dread.death.DreadDeathHandler;
import com.dread.death.DreadDeathManager;
import com.dread.death.PlayerConnectionHandler;
import com.dread.network.DreadNetworking;
import com.dread.registry.ModEntities;
import com.dread.sound.ModSounds;
import com.dread.spawn.DreadSpawnManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DreadMod implements ModInitializer {
    public static final String MOD_ID = "dread";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Dread mod...");

        // Load config FIRST before any other initialization
        DreadConfigLoader.load();

        ModSounds.register();
        ModEntities.register();
        DreadSpawnManager.register();
        DreadNetworking.registerPackets();
        DreadDeathHandler.register();
        DreadDeathManager.register();
        AttackPreventionHandler.register();
        PlayerConnectionHandler.register();
        LOGGER.info("Dread mod initialized successfully");
    }
}
