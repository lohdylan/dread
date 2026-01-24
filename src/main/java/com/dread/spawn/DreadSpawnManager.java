package com.dread.spawn;

import com.dread.DreadMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Random;

/**
 * Manages Dread spawn probability evaluation and triggers.
 * Registers server tick events and block break tracking.
 */
public class DreadSpawnManager {

    private static final Random RANDOM = new Random();

    /**
     * Register spawn probability events.
     * Call this from DreadMod.onInitialize() AFTER ModSounds and ModEntities.
     */
    public static void register() {
        DreadMod.LOGGER.info("Registering DreadSpawnManager events");

        // Server tick: Evaluate spawn probability every 1 second
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 20 == 0) { // Every 1 second (20 ticks)
                evaluateSpawnProbability(world);
            }
        });

        // Block break: Track mining activity for spawn probability
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!world.isClient() && world instanceof ServerWorld serverWorld) {
                SpawnProbabilityState spawnState = SpawnProbabilityState.getOrCreate(serverWorld);
                spawnState.incrementMinedBlocks(player.getUuid());
            }
        });

        DreadMod.LOGGER.info("DreadSpawnManager registered successfully");
    }

    /**
     * Evaluate spawn probability for all players in the world.
     * Called every second via server tick event.
     */
    private static void evaluateSpawnProbability(ServerWorld world) {
        SpawnProbabilityState state = SpawnProbabilityState.getOrCreate(world);
        List<ServerPlayerEntity> players = world.getPlayers();

        for (ServerPlayerEntity player : players) {
            // Skip if player is on cooldown
            if (state.isOnCooldown(player.getUuid(), world.getTime())) {
                continue;
            }

            // Calculate spawn chance based on day, mining, etc.
            float spawnChance = calculateSpawnChance(state, player, world);

            // Random check
            if (RANDOM.nextFloat() < spawnChance) {
                // Decide: Real spawn (25%) vs Fake-out (75%) for 3:1 ratio
                boolean isRealSpawn = RANDOM.nextFloat() < 0.25f;

                if (isRealSpawn) {
                    DreadMod.LOGGER.info("REAL SPAWN triggered for player {} (chance: {}, day: {})",
                        player.getName().getString(),
                        String.format("%.4f", spawnChance),
                        world.getTimeOfDay() / 24000L);

                    // Reset mining counter and set standard cooldown
                    state.resetAfterSpawn(player.getUuid(), world.getTime());

                    // TODO (Plan 03): Trigger actual Dread spawn
                    // Will emit spawn event or call spawn handler

                } else {
                    DreadMod.LOGGER.info("FAKE-OUT triggered for player {} (chance: {}, day: {})",
                        player.getName().getString(),
                        String.format("%.4f", spawnChance),
                        world.getTimeOfDay() / 24000L);

                    // Track fake-out and set short cooldown
                    state.incrementFakeout(player.getUuid());
                    state.setShortCooldown(player.getUuid(), world.getTime());

                    // TODO (Plan 03): Trigger fake-out effects
                    // Will emit fake-out event (distant footsteps, whispers, etc.)
                }
            }
        }
    }

    /**
     * Calculate spawn chance for a player based on multiple factors.
     *
     * @param state Spawn probability state
     * @param player Target player
     * @param world Server world
     * @return Spawn chance as float (0.0 to 1.0, though typically much lower)
     */
    private static float calculateSpawnChance(SpawnProbabilityState state,
                                              ServerPlayerEntity player,
                                              ServerWorld world) {
        long worldDay = world.getTimeOfDay() / 24000L;
        int blocksMined = state.getMinedBlocks(player.getUuid());

        // Base chance: 0.5% per check (at 1 check/second = 10-20% chance per minute)
        float baseChance = 0.005f;

        // Day escalation: Linear scaling up to day 20 cap
        // Day 1: 1.5x, Day 10: 6x, Day 20: 11x (max)
        float dayMultiplier = 1.0f + Math.min(worldDay, 20) * 0.5f;

        // Mining bonus: +0.1% per block mined since last spawn
        // Encourages active players, resets after spawn
        float miningBonus = blocksMined * 0.001f;

        float totalChance = (baseChance * dayMultiplier) + miningBonus;

        return totalChance;
    }
}
