package com.dread.death;

import com.dread.network.packets.DownedStateUpdateS2C;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import com.dread.death.CrawlPoseHandler;
import com.dread.death.GameModeDetector.DreadGameMode;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Central coordinator for death/revival tick processing.
 * Handles downed timer countdown, revival progress, and spectator transitions.
 */
public class DreadDeathManager {

    private static int tickCounter = 0;
    private static final int SYNC_INTERVAL = 20; // Sync every 1 second

    /**
     * Register with ServerTickEvents.END_WORLD_TICK during mod initialization.
     */
    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DreadDeathManager::tick);
    }

    /**
     * Main tick handler - processes all downed players and revivals.
     */
    private static void tick(ServerWorld world) {
        tickCounter++;

        DownedPlayersState state = DownedPlayersState.getOrCreate(world);

        // Process downed timers every tick
        processDownedTimers(world, state);

        // Process active revivals every tick
        processActiveRevivals(world, state);

        // Spawn blood particles for downed players
        spawnBloodParticles(world, state);

        // Sync downed states to clients every second
        if (tickCounter >= SYNC_INTERVAL) {
            syncDownedStates(world, state);
            tickCounter = 0;
        }
    }

    /**
     * Decrement timers for all downed players and transition to spectator on expiration.
     */
    private static void processDownedTimers(ServerWorld world, DownedPlayersState state) {
        List<UUID> expiredPlayers = new ArrayList<>();

        for (DownedPlayerData data : state.getAllDowned()) {
            // Skip players currently being revived (timer pauses during revival)
            if (state.isBeingRevived(data.playerId)) {
                continue;
            }

            // Decrement timer
            int remainingSeconds = state.decrementTimer(data.playerId);

            // Check for expiration
            if (remainingSeconds <= 0) {
                expiredPlayers.add(data.playerId);
            }
        }

        // Transition expired players to spectator
        for (UUID playerId : expiredPlayers) {
            transitionToSpectator(world, playerId, state);
        }
    }

    /**
     * Transition player to spectator mode and broadcast death message.
     */
    private static void transitionToSpectator(ServerWorld world, UUID playerId, DownedPlayersState state) {
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
        if (player == null) {
            // Player disconnected, clean up state
            state.removeDowned(playerId);
            return;
        }

        // Exit crawl pose BEFORE changing to spectator (pose changes don't work in spectator)
        CrawlPoseHandler.exitCrawlPose(player);

        // Change to spectator mode
        player.changeGameMode(GameMode.SPECTATOR);

        // Remove from downed state
        state.removeDowned(playerId);

        // Broadcast death message
        Text deathMessage = Text.literal(player.getName().getString() + " succumbed to the Dread");
        world.getServer().getPlayerManager().broadcast(deathMessage, false);
    }

    /**
     * Process active revivals - tick progress, handle completions, send progress updates.
     */
    private static void processActiveRevivals(ServerWorld world, DownedPlayersState state) {
        // Tick all revivals and get completed ones
        List<UUID> completedRevivals = state.tickRevivals();

        // Handle completed revivals
        for (UUID downedPlayerId : completedRevivals) {
            ServerPlayerEntity downedPlayer = world.getServer().getPlayerManager().getPlayer(downedPlayerId);
            if (downedPlayer != null) {
                RevivalInteractionHandler.completeRevival(world, downedPlayer);
            } else {
                // Player disconnected, clean up
                state.removeDowned(downedPlayerId);
            }
        }

        // Broadcast progress for active revivals
        for (DownedPlayerData data : state.getAllDowned()) {
            RevivalProgress revival = state.getRevivalProgress(data.playerId);
            if (revival != null) {
                RevivalInteractionHandler.broadcastRevivalProgress(
                    world,
                    data.playerId,
                    revival.getProgress(),
                    true
                );
            }
        }

        // Check for new revivers among downed players
        for (DownedPlayerData data : state.getAllDowned()) {
            ServerPlayerEntity downedPlayer = world.getServer().getPlayerManager().getPlayer(data.playerId);
            if (downedPlayer != null) {
                RevivalInteractionHandler.checkForRevivers(world, downedPlayer, state);
            }
        }
    }

    /**
     * Synchronize downed state to clients every second.
     */
    private static void syncDownedStates(ServerWorld world, DownedPlayersState state) {
        for (DownedPlayerData data : state.getAllDowned()) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(data.playerId);
            if (player != null) {
                DownedStateUpdateS2C packet = new DownedStateUpdateS2C(
                    true,
                    data.getRemainingSeconds(),
                    data.mode == DreadGameMode.SINGLEPLAYER  // isMercyMode
                );
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    /**
     * Spawn blood drip particles around downed players.
     * Visible to all nearby players, reinforcing injury state.
     */
    private static void spawnBloodParticles(ServerWorld world, DownedPlayersState state) {
        // Only spawn particles every 10 ticks (0.5 seconds)
        if (world.getTime() % 10 != 0) return;

        for (DownedPlayerData data : state.getAllDowned()) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(data.playerId);
            if (player == null) continue;

            Vec3d pos = player.getPos();

            // Spawn 1-2 particles around player at random offsets
            int particleCount = world.random.nextInt(2) + 1;
            for (int i = 0; i < particleCount; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 0.6;
                double offsetZ = (world.random.nextDouble() - 0.5) * 0.6;

                world.spawnParticles(
                    ParticleTypes.DRIPPING_LAVA,  // Red dripping particle
                    pos.x + offsetX,
                    pos.y + 0.3,  // Slightly above ground level
                    pos.z + offsetZ,
                    1,            // particle count
                    0, 0, 0,      // no velocity spread
                    0.0           // no extra speed
                );
            }
        }
    }
}
