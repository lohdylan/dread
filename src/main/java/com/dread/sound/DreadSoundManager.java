package com.dread.sound;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

/**
 * Centralized sound management with priority-based playback.
 * Prevents lower priority sounds from interrupting higher priority ones.
 */
public class DreadSoundManager {
    // Priority levels (lower value = higher priority)
    public static final int PRIORITY_JUMPSCARE = 0;
    public static final int PRIORITY_PROXIMITY = 1;
    public static final int PRIORITY_AMBIENT = 2;

    private static boolean isPlayingJumpscare = false;
    private static long jumpscareEndTick = 0;

    /**
     * Tick handler to reset jumpscare state after duration expires.
     * Call this from spawn manager's tick loop.
     *
     * @param world Server world
     */
    public static void tick(ServerWorld world) {
        // Reset jumpscare flag after 3 seconds (60 ticks)
        if (isPlayingJumpscare && world.getTime() > jumpscareEndTick) {
            isPlayingJumpscare = false;
        }
    }

    /**
     * Play jump scare sound at maximum priority.
     * Blocks all other sounds during playback.
     *
     * @param world Server world
     * @param pos Position to play sound
     */
    public static void playJumpScare(ServerWorld world, BlockPos pos) {
        isPlayingJumpscare = true;
        jumpscareEndTick = world.getTime() + 60; // 3 seconds

        world.playSound(
            null, // all players hear
            pos,
            ModSounds.DREAD_JUMPSCARE,
            SoundCategory.HOSTILE,
            1.0f,  // max volume
            1.0f   // normal pitch
        );
    }

    /**
     * Play a fake-out sound effect to build tension without actual spawn.
     * Randomly selects from multiple fake-out types for variety.
     *
     * @param world Server world
     * @param player Target player
     */
    public static void playFakeoutSound(ServerWorld world, ServerPlayerEntity player) {
        if (isPlayingJumpscare) return; // Don't overlap with jump scare

        // Random fake-out type selection
        float typeRoll = world.getRandom().nextFloat();

        if (typeRoll < 0.4f) {
            // 40%: Distant danger rising sound
            world.playSound(null, player.getBlockPos(), ModSounds.DANGER_RISING,
                SoundCategory.AMBIENT, 0.3f, 0.7f + world.getRandom().nextFloat() * 0.3f);
        } else if (typeRoll < 0.7f) {
            // 30%: Proximity sound suggesting something nearby
            world.playSound(null, player.getBlockPos().add(
                world.getRandom().nextBetween(-10, 10), 0, world.getRandom().nextBetween(-10, 10)),
                ModSounds.DREAD_PROXIMITY, SoundCategory.HOSTILE, 0.4f, 0.8f + world.getRandom().nextFloat() * 0.4f);
        } else {
            // 30%: Quick ambient spike
            world.playSound(null, player.getBlockPos(), ModSounds.DREAD_AMBIENT,
                SoundCategory.AMBIENT, 0.5f, 1.2f);
        }
    }

    /**
     * Play danger rising indicator based on spawn probability intensity.
     * Provides subtle warning as spawn chance increases.
     *
     * @param world Server world
     * @param player Target player
     * @param intensity Intensity value (0.0 to 1.0)
     */
    public static void playDangerRising(ServerWorld world, ServerPlayerEntity player, float intensity) {
        if (isPlayingJumpscare) return;
        float volume = 0.1f + (intensity * 0.4f);
        world.playSound(null, player.getBlockPos(), ModSounds.DANGER_RISING,
            SoundCategory.AMBIENT, volume, 0.9f + (intensity * 0.2f));
    }

    /**
     * Play proximity sound with unnatural silence effect.
     * Volume decreases as entity gets closer (counter-intuitive horror mechanic).
     *
     * @param world Server world
     * @param entityPos Entity position
     * @param distance Distance to entity
     */
    public static void playProximitySound(ServerWorld world, BlockPos entityPos, float distance) {
        if (isPlayingJumpscare) return;

        // Inverse distance for volume (quieter as entity gets closer - unnatural silence)
        float volume;
        if (distance < 2) {
            volume = 0.0f; // Complete silence when very close
        } else if (distance < 8) {
            volume = 0.1f + ((distance - 2) / 6) * 0.4f;
        } else {
            volume = 0.5f; // Normal volume at distance
        }

        if (volume > 0.05f) {
            world.playSound(null, entityPos, ModSounds.DREAD_PROXIMITY,
                SoundCategory.HOSTILE, volume, 0.8f);
        }
    }
}
