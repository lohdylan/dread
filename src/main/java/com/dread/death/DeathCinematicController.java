package com.dread.death;

import com.dread.config.DreadConfigLoader;
import com.dread.entity.DreadEntity;
import com.dread.network.packets.CinematicTriggerS2C;
import com.dread.sound.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side controller for death cinematic sequence.
 * Handles Dread teleportation, death sound, client synchronization, and Dread departure.
 */
public class DeathCinematicController {

    private static final double FACE_TO_FACE_DISTANCE = 1.5; // blocks in front of player
    private static final int DREAD_VANISH_DELAY_TICKS = 100; // 5 seconds - Dread leaves after attack animation

    // Track Dreads that need to vanish after attacking
    private static final Map<Integer, Integer> pendingVanish = new HashMap<>();

    /**
     * Register tick handler for Dread departure timing.
     */
    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DeathCinematicController::tick);
    }

    /**
     * Tick handler - process Dread departures.
     */
    private static void tick(ServerWorld world) {
        if (pendingVanish.isEmpty()) return;

        List<Integer> readyToVanish = new ArrayList<>();

        // Decrement timers
        for (Map.Entry<Integer, Integer> entry : pendingVanish.entrySet()) {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                readyToVanish.add(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }

        // Trigger vanishing for ready Dreads
        for (Integer entityId : readyToVanish) {
            pendingVanish.remove(entityId);

            var entity = world.getEntityById(entityId);
            if (entity instanceof DreadEntity dread) {
                dread.setVanishing(true);
            }
        }
    }

    /**
     * Trigger the death cinematic sequence.
     * Teleports Dread face-to-face with player, plays death sound, sends client packet.
     * Dread will vanish after 2 seconds.
     *
     * @param player The downed player
     * @param dread The Dread entity that killed the player
     */
    public static void triggerDeathCinematic(ServerPlayerEntity player, DreadEntity dread) {
        var config = DreadConfigLoader.getConfig();

        // Play death sound at player location
        BlockPos deathPos = player.getBlockPos();
        player.getServerWorld().playSound(
            null, // Play to all nearby players
            deathPos,
            ModSounds.DREAD_DEATH,
            SoundCategory.HOSTILE,
            1.0f, // volume
            1.0f  // pitch
        );

        // Schedule Dread to vanish after attack animation (2 seconds)
        pendingVanish.put(dread.getId(), DREAD_VANISH_DELAY_TICKS);

        // Skip camera lock if configured
        if (config.skipDeathCinematic) {
            return;
        }

        // Calculate position in front of player using HORIZONTAL facing only (ignore pitch)
        Vec3d playerPos = player.getPos();
        float playerYaw = player.getYaw();

        // Convert yaw to direction vector (horizontal only, no pitch component)
        double yawRadians = Math.toRadians(-playerYaw - 90);
        Vec3d horizontalLook = new Vec3d(Math.cos(yawRadians), 0, Math.sin(yawRadians));

        // Position Dread in front of player at same Y level
        Vec3d dreadPos = playerPos.add(horizontalLook.multiply(FACE_TO_FACE_DISTANCE));

        // Calculate yaw for Dread to face player (opposite direction)
        float dreadYaw = playerYaw + 180.0f;

        // Teleport Dread to face-to-face position at ground level
        dread.refreshPositionAndAngles(dreadPos.x, playerPos.y, dreadPos.z, dreadYaw, 0.0f);

        // Send cinematic trigger packet to client
        CinematicTriggerS2C packet = new CinematicTriggerS2C(
            dread.getId(),
            deathPos
        );
        ServerPlayNetworking.send(player, packet);
    }
}
