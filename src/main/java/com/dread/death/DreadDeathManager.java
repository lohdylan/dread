package com.dread.death;

import com.dread.network.packets.DownedStateUpdateS2C;
import com.dread.network.packets.RemoveDownedEffectsS2C;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import com.dread.death.CrawlPoseHandler;
import com.dread.death.DeathCinematicController;
import com.dread.death.GameModeDetector.DreadGameMode;
import com.dread.entity.DreadEntity;
import com.dread.config.DreadConfigLoader;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.*;

/**
 * Central coordinator for death/revival tick processing.
 * Handles downed timer countdown, revival progress, and spectator transitions.
 */
public class DreadDeathManager {

    private static int tickCounter = 0;
    private static final int SYNC_INTERVAL = 20; // Sync every 1 second
    private static final int CINEMATIC_DURATION_TICKS = 120; // 6 seconds (matches death_grab animation v3.0)

    // Track players waiting for cinematic to complete before death
    private static final Map<UUID, Integer> pendingDeathAfterCinematic = new HashMap<>();
    // Track players currently in death cinematic (to pause their downed timer)
    private static final Set<UUID> playersInCinematic = new HashSet<>();

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

        // Process pending deaths (after cinematic completes)
        processPendingDeaths(world, state);

        // Spawn blood particles for downed players
        spawnBloodParticles(world, state);

        // Sync downed states to clients every second
        if (tickCounter >= SYNC_INTERVAL) {
            syncDownedStates(world, state);
            tickCounter = 0;
        }
    }

    /**
     * Process players waiting for death cinematic to complete before dying/spectator.
     */
    private static void processPendingDeaths(ServerWorld world, DownedPlayersState state) {
        List<UUID> readyToTransition = new ArrayList<>();

        // Decrement timers and find players ready to transition
        for (Map.Entry<UUID, Integer> entry : pendingDeathAfterCinematic.entrySet()) {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                readyToTransition.add(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }

        // Complete transitions for players whose cinematic is done
        for (UUID playerId : readyToTransition) {
            pendingDeathAfterCinematic.remove(playerId);

            if (pendingSpectatorAfterCinematic.contains(playerId)) {
                // Multiplayer - transition to spectator
                completeSpectatorTransition(world, playerId, state);
            } else {
                // Singleplayer - normal death
                completeSingleplayerDeath(world, playerId, state);
            }
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

            // Skip players in death cinematic (timer pauses during cinematic)
            if (playersInCinematic.contains(data.playerId)) {
                continue;
            }

            // Decrement timer
            int remainingSeconds = state.decrementTimer(data.playerId);

            // Check for expiration
            if (remainingSeconds <= 0) {
                expiredPlayers.add(data.playerId);
            }
        }

        // Transition expired players based on game mode
        for (UUID playerId : expiredPlayers) {
            DownedPlayerData data = state.getDownedData(playerId);
            if (data != null && data.mode == DreadGameMode.SINGLEPLAYER) {
                triggerSingleplayerDeath(world, playerId, state);
            } else {
                transitionToSpectator(world, playerId, state);
            }
        }
    }

    // Track players waiting for cinematic before spectator (multiplayer)
    private static final Set<UUID> pendingSpectatorAfterCinematic = new HashSet<>();

    /**
     * Transition player to spectator mode - immediate transition, no second cinematic.
     * The cinematic already played when Dread first attacked. Player bled out, now spectator.
     */
    private static void transitionToSpectator(ServerWorld world, UUID playerId, DownedPlayersState state) {
        // No second cinematic - player already saw Dread attack when first downed.
        // They've been bleeding out, now they become spectator.
        completeSpectatorTransition(world, playerId, state);
    }

    /**
     * Complete the spectator transition after cinematic finishes.
     */
    private static void completeSpectatorTransition(ServerWorld world, UUID playerId, DownedPlayersState state) {
        // Clear cinematic flag
        playersInCinematic.remove(playerId);

        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
        if (player == null) {
            state.removeDowned(playerId);
            pendingSpectatorAfterCinematic.remove(playerId);
            return;
        }

        // Exit crawl pose BEFORE changing to spectator (pose changes don't work in spectator)
        CrawlPoseHandler.exitCrawlPose(player);

        // Remove movement penalty
        RevivalInteractionHandler.removeMovementPenalty(player);

        // Change to spectator mode
        player.changeGameMode(GameMode.SPECTATOR);

        // Remove from downed state
        state.removeDowned(playerId);

        // Clear pending flag
        pendingSpectatorAfterCinematic.remove(playerId);

        // Broadcast death message
        Text deathMessage = Text.literal(player.getName().getString() + " succumbed to the Dread");
        world.getServer().getPlayerManager().broadcast(deathMessage, false);
    }

    /**
     * Trigger singleplayer death - immediate death, no second cinematic.
     * The cinematic already played when Dread first attacked. Player bled out, now they die.
     */
    private static void triggerSingleplayerDeath(ServerWorld world, UUID playerId, DownedPlayersState state) {
        // No second cinematic - player already saw Dread attack when first downed.
        // They've been bleeding out, now they just die.
        completeSingleplayerDeath(world, playerId, state);
    }

    /**
     * Complete the singleplayer death after cinematic finishes.
     * Called either directly (no Dread) or after cinematic timer expires.
     */
    private static void completeSingleplayerDeath(ServerWorld world, UUID playerId, DownedPlayersState state) {
        // Clear cinematic flag
        playersInCinematic.remove(playerId);

        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
        if (player == null) {
            state.removeDowned(playerId);
            pendingDeathAfterCinematic.remove(playerId);
            return;
        }

        // Exit crawl pose BEFORE death
        CrawlPoseHandler.exitCrawlPose(player);

        // Remove movement penalty
        RevivalInteractionHandler.removeMovementPenalty(player);

        // Mark for respawn debuff
        state.markDreadDeath(playerId);

        // Remove from downed state
        state.removeDowned(playerId);

        // Broadcast death message
        Text deathMessage = Text.literal(player.getName().getString() + " succumbed to the Dread");
        world.getServer().getPlayerManager().broadcast(deathMessage, false);

        // CRITICAL: Send packet to clear client-side downed effects BEFORE kill()
        // Without this, client still thinks it's downed and DeathScreenMixin cancels init(),
        // causing null scoreText and crash in DeathScreen.render()
        ServerPlayNetworking.send(player, new RemoveDownedEffectsS2C());

        // Trigger normal Minecraft death - respects keepInventory, shows death screen
        player.kill();
    }

    /**
     * Find the nearest Dread entity within range of player.
     */
    private static DreadEntity findNearestDread(ServerPlayerEntity player, double range) {
        List<DreadEntity> dreads = player.getServerWorld().getEntitiesByClass(
            DreadEntity.class,
            player.getBoundingBox().expand(range),
            dread -> true
        );

        if (dreads.isEmpty()) {
            return null;
        }

        return dreads.stream()
            .min(Comparator.comparingDouble(dread -> dread.squaredDistanceTo(player)))
            .orElse(null);
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

    // Blood red color for particles (dark crimson red)
    private static final DustParticleEffect BLOOD_PARTICLE = new DustParticleEffect(
        new Vector3f(0.6f, 0.05f, 0.05f),  // Dark blood red RGB
        1.2f  // Slightly larger size
    );

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

            // Spawn 2-4 blood particles around player at random offsets
            int particleCount = world.random.nextInt(3) + 2;
            for (int i = 0; i < particleCount; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
                double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
                double offsetY = world.random.nextDouble() * 0.3;

                world.spawnParticles(
                    BLOOD_PARTICLE,
                    pos.x + offsetX,
                    pos.y + 0.2 + offsetY,  // Near ground level
                    pos.z + offsetZ,
                    1,              // particle count
                    0.02, -0.05, 0.02,  // slight spread, downward drift
                    0.01            // slow speed
                );
            }
        }
    }
}
