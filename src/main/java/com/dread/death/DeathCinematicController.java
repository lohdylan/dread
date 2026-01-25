package com.dread.death;

import com.dread.entity.DreadEntity;
import com.dread.network.packets.CinematicTriggerS2C;
import com.dread.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Server-side controller for death cinematic sequence.
 * Handles Dread teleportation, death sound, and client synchronization.
 */
public class DeathCinematicController {

    private static final double FACE_TO_FACE_DISTANCE = 1.5; // blocks in front of player

    /**
     * Trigger the death cinematic sequence.
     * Teleports Dread face-to-face with player, plays death sound, sends client packet.
     *
     * @param player The downed player
     * @param dread The Dread entity that killed the player
     */
    public static void triggerDeathCinematic(ServerPlayerEntity player, DreadEntity dread) {
        // Calculate position 1.5 blocks in front of player (facing them)
        Vec3d playerPos = player.getPos();
        Vec3d playerLook = player.getRotationVector().normalize();
        Vec3d dreadPos = playerPos.add(playerLook.multiply(FACE_TO_FACE_DISTANCE));

        // Calculate yaw and pitch to face player
        Vec3d lookVector = playerPos.subtract(dreadPos).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(lookVector.z, lookVector.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.asin(lookVector.y));

        // Teleport Dread to face-to-face position with correct rotation
        dread.refreshPositionAndAngles(dreadPos.x, dreadPos.y, dreadPos.z, yaw, pitch);

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

        // Send cinematic trigger packet to client
        CinematicTriggerS2C packet = new CinematicTriggerS2C(
            dread.getId(),
            deathPos
        );
        ServerPlayNetworking.send(player, packet);
    }
}
