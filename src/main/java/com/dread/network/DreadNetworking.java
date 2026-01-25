package com.dread.network;

import com.dread.DreadMod;
import com.dread.network.packets.CinematicTriggerS2C;
import com.dread.network.packets.DownedStateUpdateS2C;
import com.dread.network.packets.RemoveDownedEffectsS2C;
import com.dread.network.packets.RevivalProgressS2C;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Central registration point for all Dread mod network packets.
 * Registers Server-to-Client (S2C) packets for death cinematics, downed state sync,
 * and revival progress updates.
 */
public class DreadNetworking {

    /**
     * Registers all network packet types with Fabric API.
     * Called during mod initialization.
     */
    public static void registerPackets() {
        // Register Server-to-Client payloads
        PayloadTypeRegistry.playS2C().register(CinematicTriggerS2C.ID, CinematicTriggerS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(DownedStateUpdateS2C.ID, DownedStateUpdateS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(RevivalProgressS2C.ID, RevivalProgressS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoveDownedEffectsS2C.ID, RemoveDownedEffectsS2C.CODEC);

        DreadMod.LOGGER.info("Registered Dread network packets");
    }
}
