package com.dread.death;

import com.dread.network.packets.RemoveDownedEffectsS2C;
import com.dread.network.packets.RevivalProgressS2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

/**
 * Handles proximity-based revival detection and processing.
 * Detects nearby crouching players and manages the 3-second revival process.
 */
public class RevivalInteractionHandler {

    private static final double REVIVAL_RANGE = 4.0;
    private static final Identifier CRAWL_MODIFIER_ID = Identifier.of("dread", "crawl_penalty");
    private static final double CRAWL_SPEED_MULTIPLIER = -0.9; // -90% movement speed

    /**
     * Check for nearby crouching players and start revivals.
     * Called every tick for each downed player.
     */
    public static void checkForRevivers(ServerWorld world, ServerPlayerEntity downedPlayer, DownedPlayersState state) {
        UUID downedId = downedPlayer.getUuid();

        // Skip if already being revived
        if (state.isBeingRevived(downedId)) {
            return;
        }

        // Find nearby players within revival range
        List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(player ->
            player != downedPlayer &&
            !state.isDowned(player) &&
            player.squaredDistanceTo(downedPlayer) <= REVIVAL_RANGE * REVIVAL_RANGE
        );

        // Check if any nearby player is crouching
        for (ServerPlayerEntity potentialReviver : nearbyPlayers) {
            if (potentialReviver.isSneaking()) {
                startRevival(state, downedId, potentialReviver.getUuid());
                return; // One reviver at a time
            }
        }
    }

    /**
     * Start a 3-second uninterruptible revival process.
     */
    private static void startRevival(DownedPlayersState state, UUID downedPlayerId, UUID reviverPlayerId) {
        state.startRevival(downedPlayerId, reviverPlayerId);
    }

    /**
     * Complete a revival - restore health, remove downed state, send packets.
     */
    public static void completeRevival(ServerWorld world, ServerPlayerEntity downedPlayer) {
        UUID playerId = downedPlayer.getUuid();
        DownedPlayersState state = DownedPlayersState.getOrCreate(world);

        // Restore player health to full
        downedPlayer.setHealth(downedPlayer.getMaxHealth());

        // Remove movement penalty
        removeMovementPenalty(downedPlayer);

        // Exit crawl/prone pose
        CrawlPoseHandler.exitCrawlPose(downedPlayer);

        // Remove from downed state
        state.removeDowned(playerId);

        // Send packet to remove visual effects
        ServerPlayNetworking.send(downedPlayer, new RemoveDownedEffectsS2C());
    }

    /**
     * Apply -90% movement speed penalty for crawling.
     */
    public static void applyMovementPenalty(ServerPlayerEntity player) {
        EntityAttributeInstance movementSpeed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed == null) return;

        // Remove existing modifier if present
        movementSpeed.removeModifier(CRAWL_MODIFIER_ID);

        // Add crawl penalty
        EntityAttributeModifier modifier = new EntityAttributeModifier(
            CRAWL_MODIFIER_ID,
            CRAWL_SPEED_MULTIPLIER,
            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        movementSpeed.addPersistentModifier(modifier);
    }

    /**
     * Remove movement penalty when revived.
     */
    public static void removeMovementPenalty(ServerPlayerEntity player) {
        EntityAttributeInstance movementSpeed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (movementSpeed == null) return;

        movementSpeed.removeModifier(CRAWL_MODIFIER_ID);
    }

    /**
     * Broadcast revival progress to nearby players (within 16 blocks).
     */
    public static void broadcastRevivalProgress(ServerWorld world, UUID downedPlayerId, float progress, boolean active) {
        ServerPlayerEntity downedPlayer = world.getServer().getPlayerManager().getPlayer(downedPlayerId);
        if (downedPlayer == null) return;

        RevivalProgressS2C packet = new RevivalProgressS2C(downedPlayerId, active, progress);

        // Send to all nearby players (including the downed player)
        List<ServerPlayerEntity> nearbyPlayers = world.getPlayers(player ->
            player.squaredDistanceTo(downedPlayer) <= 16 * 16
        );

        for (ServerPlayerEntity player : nearbyPlayers) {
            ServerPlayNetworking.send(player, packet);
        }
    }
}
