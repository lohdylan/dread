package com.dread.death;

import com.dread.entity.DreadEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

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
        // Only handle player deaths
        if (!(entity instanceof ServerPlayerEntity player)) {
            return true; // Allow death for non-players
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

        // Trigger death cinematic
        DeathCinematicController.triggerDeathCinematic(player, dread);

        // Cancel vanilla death
        return false;
    }
}
