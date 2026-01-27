package com.dread.death;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Utility for detecting whether the game is in single-player or multiplayer mode.
 * Used to apply different downed state timeouts based on game context.
 */
public class GameModeDetector {

    /**
     * Game mode classification for Dread mod behavior.
     */
    public enum DreadGameMode {
        SINGLEPLAYER,  // True single-player: integrated server with 1 player
        MULTIPLAYER    // Dedicated server or LAN with 2+ players
    }

    /**
     * Detect current game mode based on server state.
     *
     * @param world The server world to check
     * @return SINGLEPLAYER if integrated server with 1 player, MULTIPLAYER otherwise
     */
    public static DreadGameMode detectMode(ServerWorld world) {
        MinecraftServer server = world.getServer();

        // Dedicated servers are always multiplayer
        if (!server.isSingleplayer()) {
            return DreadGameMode.MULTIPLAYER;
        }

        // Integrated server with 1 player = true singleplayer
        if (server.getPlayerManager().getCurrentPlayerCount() == 1) {
            return DreadGameMode.SINGLEPLAYER;
        }

        // Integrated server with 2+ players = LAN multiplayer
        return DreadGameMode.MULTIPLAYER;
    }

    /**
     * Convenience method to check if currently in single-player mode.
     *
     * @param world The server world to check
     * @return true if single-player mode
     */
    public static boolean isSingleplayerMode(ServerWorld world) {
        return detectMode(world) == DreadGameMode.SINGLEPLAYER;
    }
}
