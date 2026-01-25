package com.dread.spawn;

import com.dread.DreadMod;
import com.dread.config.DreadConfigLoader;
import com.dread.entity.DreadEntity;
import com.dread.registry.ModEntities;
import com.dread.sound.DreadSoundManager;
import com.dread.sound.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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
        // Check if mod is enabled
        if (!DreadConfigLoader.getConfig().modEnabled) {
            return; // Skip all spawn logic when mod disabled
        }

        // Tick sound manager
        DreadSoundManager.tick(world);

        SpawnProbabilityState state = SpawnProbabilityState.getOrCreate(world);
        List<ServerPlayerEntity> players = world.getPlayers();

        for (ServerPlayerEntity player : players) {
            // Skip if player is on cooldown
            if (state.isOnCooldown(player.getUuid(), world.getTime())) {
                continue;
            }

            // Calculate spawn chance based on day, mining, etc.
            float spawnChance = calculateSpawnChance(state, player, world);

            // Danger indicator sound based on current probability
            if (spawnChance > 0.01f) {
                float intensity = Math.min(spawnChance * 10, 1.0f);
                // Only play occasionally (not every tick, every 5 seconds)
                if (world.getTime() % 100 == 0 && world.getRandom().nextFloat() < 0.3f) {
                    DreadSoundManager.playDangerRising(world, player, intensity);
                }
            }

            // Random check
            if (RANDOM.nextFloat() < spawnChance) {
                // Decide: Real spawn (25%) vs Fake-out (75%) for 3:1 ratio
                boolean isRealSpawn = RANDOM.nextFloat() < 0.25f;

                if (isRealSpawn) {
                    DreadMod.LOGGER.info("REAL SPAWN triggered for player {} (chance: {}, day: {})",
                        player.getName().getString(),
                        String.format("%.4f", spawnChance),
                        world.getTimeOfDay() / 24000L);

                    // Spawn Dread behind player
                    spawnDread(world, player);

                    // Reset mining counter and set standard cooldown
                    state.resetAfterSpawn(player.getUuid(), world.getTime());

                } else {
                    DreadMod.LOGGER.info("FAKE-OUT triggered for player {} (chance: {}, day: {})",
                        player.getName().getString(),
                        String.format("%.4f", spawnChance),
                        world.getTimeOfDay() / 24000L);

                    // Trigger fake-out sound
                    triggerFakeout(world, player, state);
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
        var config = DreadConfigLoader.getConfig();

        long worldDay = world.getTimeOfDay() / 24000L;
        int blocksMined = state.getMinedBlocks(player.getUuid());

        // Use config values instead of hardcoded
        float baseChance = config.baseSpawnChancePerSecond;

        // Day escalation: Linear scaling up to config cap
        float dayMultiplier = 1.0f + Math.min(worldDay, config.dayEscalationCap) * 0.5f;

        // Mining bonus from config
        float miningBonus = blocksMined * config.miningBonusPerBlock;

        float totalChance = (baseChance * dayMultiplier) + miningBonus;

        return totalChance;
    }

    /**
     * Trigger a fake-out event that plays tension sounds without spawning.
     */
    private static void triggerFakeout(ServerWorld world, ServerPlayerEntity player, SpawnProbabilityState state) {
        DreadSoundManager.playFakeoutSound(world, player);
        state.incrementFakeout(player.getUuid());

        // Fake-outs have shorter cooldown (10-20 seconds)
        state.setShortCooldown(player.getUuid(), world.getTime());

        DreadMod.LOGGER.debug("Fake-out triggered for player {}", player.getName().getString());
    }

    /**
     * Spawn a Dread entity behind the player at random distance (3-8 blocks).
     * Plays jump scare sound and adjusts Y to ground level.
     */
    private static void spawnDread(ServerWorld world, ServerPlayerEntity player) {
        // Calculate spawn position behind player
        Vec3d lookDir = player.getRotationVector();
        Vec3d behindDir = lookDir.multiply(-1.0);

        // Random distance 3-8 blocks
        double distance = 3.0 + world.getRandom().nextDouble() * 5.0;
        Vec3d spawnPos = player.getPos().add(behindDir.multiply(distance));

        // Adjust Y to ground level
        BlockPos groundPos = new BlockPos((int)spawnPos.x, (int)spawnPos.y, (int)spawnPos.z);
        while (world.getBlockState(groundPos).isAir() && groundPos.getY() > world.getBottomY()) {
            groundPos = groundPos.down();
        }
        groundPos = groundPos.up(); // Stand on top of ground

        // Spawn entity
        DreadEntity dread = ModEntities.DREAD.create(world);
        if (dread != null) {
            dread.setPosition(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);
            dread.setYaw(player.getYaw() + 180); // Face player
            world.spawnEntity(dread);

            // Play jump scare sound via DreadSoundManager
            DreadSoundManager.playJumpScare(world, groundPos);

            DreadMod.LOGGER.info("Dread spawned behind player {} at {}", player.getName().getString(), groundPos);
        }
    }
}
