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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Manages Dread spawn probability evaluation and triggers.
 * Registers server tick events and block break tracking.
 */
public class DreadSpawnManager {

    private static final Random RANDOM = new Random();

    // Track glimpse Dreads scheduled to vanish: entityId -> ticksRemaining
    private static final Map<Integer, Integer> pendingGlimpseVanish = new HashMap<>();

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

        // Process pending glimpse vanishes
        processGlimpseVanishes(world);

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

        // Check if player is in a safe daytime location
        if (isPlayerInDaylightSafety(player, world)) {
            return 0.0f; // No spawns during day unless underground
        }

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
     * Check if player is in a "safe" daytime location where Dread won't spawn.
     * Returns true if it's daytime AND player has sky access AND is above ground.
     *
     * @param player Target player
     * @param world Server world
     * @return true if player is safe from daytime spawns
     */
    private static boolean isPlayerInDaylightSafety(ServerPlayerEntity player, ServerWorld world) {
        // Check if it's daytime (1000-13000 ticks = 7am to 7pm)
        long timeOfDay = world.getTimeOfDay() % 24000;
        boolean isDaytime = timeOfDay >= 1000 && timeOfDay < 13000;

        if (!isDaytime) {
            return false; // Night time - not safe
        }

        BlockPos playerPos = player.getBlockPos();

        // Underground check: Below Y=50 is always "underground" regardless of sky
        if (playerPos.getY() < 50) {
            DreadMod.LOGGER.debug("Player {} below Y=50, daytime safety disabled", player.getName().getString());
            return false; // Deep underground - Dread can spawn
        }

        // Check if player can see the sky directly above them
        boolean canSeeSky = world.isSkyVisible(playerPos);
        if (!canSeeSky) {
            DreadMod.LOGGER.debug("Player {} cannot see sky, daytime safety disabled", player.getName().getString());
            return false; // In cave or under solid blocks - Dread can spawn
        }

        // Sky light check: Additional safety check for covered areas
        int skyLight = world.getLightLevel(net.minecraft.world.LightType.SKY, playerPos);
        if (skyLight < 10) {
            DreadMod.LOGGER.debug("Player {} low sky light ({}), daytime safety disabled", player.getName().getString(), skyLight);
            return false; // Very dark even with sky access - Dread can spawn
        }

        // Player is in daytime with sky access above Y=50 - SAFE
        DreadMod.LOGGER.debug("Player {} is in daylight safety (time={}, Y={}, skyLight={})",
            player.getName().getString(), timeOfDay, playerPos.getY(), skyLight);
        return true;
    }

    /**
     * Trigger a fake-out event - either audio-only or visual glimpse.
     * Visual glimpses are more terrifying: Dread appears briefly at edge of vision then vanishes.
     */
    private static void triggerFakeout(ServerWorld world, ServerPlayerEntity player, SpawnProbabilityState state) {
        // 40% chance for visual glimpse, 60% for audio-only fake-out
        boolean isVisualGlimpse = RANDOM.nextFloat() < 0.40f;

        if (isVisualGlimpse) {
            spawnGlimpse(world, player);
            DreadMod.LOGGER.info("GLIMPSE fake-out for player {}", player.getName().getString());
        } else {
            DreadSoundManager.playFakeoutSound(world, player);
            DreadMod.LOGGER.debug("Audio fake-out for player {}", player.getName().getString());
        }

        state.incrementFakeout(player.getUuid());

        // Fake-outs have shorter cooldown (10-20 seconds)
        state.setShortCooldown(player.getUuid(), world.getTime());
    }

    /**
     * Spawn a "glimpse" Dread at the edge of player's peripheral vision.
     * The Dread appears briefly then immediately starts vanishing.
     * Creates the unsettling "did I just see something?" effect.
     */
    private static void spawnGlimpse(ServerWorld world, ServerPlayerEntity player) {
        // Calculate position at edge of player's vision (60-90 degrees to the side)
        float playerYaw = player.getYaw();

        // Randomly choose left or right peripheral
        float sideAngle = RANDOM.nextBoolean() ? 75.0f : -75.0f;
        // Add some randomness (-15 to +15 degrees)
        sideAngle += (RANDOM.nextFloat() - 0.5f) * 30.0f;

        float spawnYaw = playerYaw + sideAngle;
        double yawRadians = Math.toRadians(-spawnYaw - 90);

        // Spawn 8-15 blocks away (far enough to be at edge of vision)
        double distance = 8.0 + RANDOM.nextDouble() * 7.0;
        double spawnX = player.getX() + Math.cos(yawRadians) * distance;
        double spawnZ = player.getZ() + Math.sin(yawRadians) * distance;

        // Find ground level
        BlockPos groundPos = new BlockPos((int) spawnX, (int) player.getY(), (int) spawnZ);

        // Search up and down for valid ground
        for (int i = 0; i < 10; i++) {
            if (!world.getBlockState(groundPos).isAir()) {
                groundPos = groundPos.up();
                break;
            }
            groundPos = groundPos.down();
        }

        // Make sure we're standing on solid ground
        while (world.getBlockState(groundPos).isAir() && groundPos.getY() > world.getBottomY()) {
            groundPos = groundPos.down();
        }
        groundPos = groundPos.up();

        // Spawn the glimpse Dread
        DreadEntity dread = ModEntities.DREAD.create(world);
        if (dread != null) {
            dread.setPosition(groundPos.getX() + 0.5, groundPos.getY(), groundPos.getZ() + 0.5);

            // Face toward the player
            double dx = player.getX() - groundPos.getX();
            double dz = player.getZ() - groundPos.getZ();
            float faceYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            dread.setYaw(faceYaw);

            world.spawnEntity(dread);

            // Immediately trigger vanishing after a brief moment (0.5-1.5 seconds)
            int vanishDelay = 10 + RANDOM.nextInt(20); // 10-30 ticks
            scheduleGlimpseVanish(world, dread, vanishDelay);

            // Play subtle ambient sound (not full jump scare)
            world.playSound(
                null,
                groundPos,
                ModSounds.DREAD_AMBIENT,
                SoundCategory.HOSTILE,
                0.4f, // Quieter than normal
                0.8f + RANDOM.nextFloat() * 0.4f // Slightly varied pitch
            );

            DreadMod.LOGGER.debug("Glimpse spawned at {} for player {}", groundPos, player.getName().getString());
        }
    }

    /**
     * Schedule a glimpse Dread to vanish after a short delay.
     */
    private static void scheduleGlimpseVanish(ServerWorld world, DreadEntity dread, int delayTicks) {
        pendingGlimpseVanish.put(dread.getId(), delayTicks);
    }

    /**
     * Process pending glimpse vanishes - decrement timers and trigger vanish.
     */
    private static void processGlimpseVanishes(ServerWorld world) {
        if (pendingGlimpseVanish.isEmpty()) return;

        List<Integer> toRemove = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : pendingGlimpseVanish.entrySet()) {
            int entityId = entry.getKey();
            int ticksRemaining = entry.getValue() - 20; // Subtract 20 since we tick every second

            if (ticksRemaining <= 0) {
                // Time to vanish
                var entity = world.getEntityById(entityId);
                if (entity instanceof DreadEntity dread && !dread.isVanishing()) {
                    dread.setVanishing(true);
                    DreadMod.LOGGER.debug("Glimpse Dread {} starting vanish", entityId);
                }
                toRemove.add(entityId);
            } else {
                entry.setValue(ticksRemaining);
            }
        }

        // Clean up completed entries
        for (Integer id : toRemove) {
            pendingGlimpseVanish.remove(id);
        }
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
