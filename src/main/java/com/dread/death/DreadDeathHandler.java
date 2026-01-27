package com.dread.death;

import com.dread.DreadMod;
import com.dread.config.DreadConfigLoader;
import com.dread.death.CrawlPoseHandler;
import com.dread.entity.DreadEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Comparator;
import java.util.List;

/**
 * Intercepts player death events when killed by Dread entity.
 * Transitions player to downed state instead of immediate death.
 */
public class DreadDeathHandler {

    /**
     * Register the death event handler during mod initialization.
     */
    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register(DreadDeathHandler::onPlayerDeath);
    }

    /**
     * Death event handler - intercepts Dread kills.
     *
     * @return false to cancel death, true to allow vanilla death
     */
    private static boolean onPlayerDeath(LivingEntity entity, DamageSource source, float damageAmount) {
        // Check if mod is enabled
        if (!DreadConfigLoader.getConfig().modEnabled) {
            return true; // Allow vanilla death when mod disabled
        }

        // Only handle player deaths
        if (!(entity instanceof ServerPlayerEntity player)) {
            return true; // Allow death for non-players
        }

        // Check for void damage or /kill command - bypass downed state
        if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) {
            DownedPlayersState state = DownedPlayersState.getOrCreate(player.getServerWorld());

            if (state.isDowned(player)) {
                // Player was downed, now killed by void/kill - trigger immediate death
                String damageType = source.isOf(DamageTypes.OUT_OF_WORLD) ? "void" : "/kill";
                DreadMod.LOGGER.info("Player {} killed by {} while downed - bypassing timer",
                    player.getName().getString(), damageType);

                // Find nearest Dread entity to trigger cinematic
                DreadEntity nearestDread = findNearestDread(player, 64.0);
                if (nearestDread != null) {
                    DeathCinematicController.triggerDeathCinematic(player, nearestDread);
                }

                // Exit crawl pose
                CrawlPoseHandler.exitCrawlPose(player);

                // Remove from downed state
                state.removeDowned(player.getUuid());
            }

            return true; // Allow vanilla death to proceed
        }

        // Only intercept deaths from Dread entity
        if (!(source.getAttacker() instanceof DreadEntity dread)) {
            return true; // Allow vanilla death for other damage sources
        }

        // Check if player is already downed (prevent re-triggering)
        DownedPlayersState state = DownedPlayersState.getOrCreate(player.getServerWorld());
        if (state.isDowned(player)) {
            return true; // Already downed, allow permanent death
        }

        // Set player health to 1.0 to prevent death next tick
        player.setHealth(1.0f);

        // Transition to downed state
        state.setDowned(player);

        // Apply crawl movement penalty
        RevivalInteractionHandler.applyMovementPenalty(player);

        // Enter crawl/prone pose
        CrawlPoseHandler.enterCrawlPose(player);

        // Trigger death cinematic
        DeathCinematicController.triggerDeathCinematic(player, dread);

        // Cancel vanilla death
        return false;
    }

    /**
     * Find the nearest Dread entity within range of a player.
     *
     * @param player The player to search around
     * @param range The search range in blocks
     * @return The nearest Dread entity, or null if none found
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
}
