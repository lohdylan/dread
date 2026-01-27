package com.dread.death;

import com.dread.DreadMod;
import com.dread.death.GameModeDetector.DreadGameMode;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

/**
 * Handles player disconnect/reconnect lifecycle for downed state management.
 * - On DISCONNECT: Clears downed state, marks player as escaped if they were downed
 * - On JOIN: Applies reconnect penalty (2 HP, 3s immunity, broadcast) if player escaped
 */
public class PlayerConnectionHandler {

    private static final int RECONNECT_IMMUNITY_TICKS = 60;  // 3 seconds at 20 ticks/second

    /**
     * Registers both disconnect and join event handlers.
     * Call from DreadMod.onInitialize().
     */
    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register(PlayerConnectionHandler::onPlayerDisconnect);
        ServerPlayConnectionEvents.JOIN.register(PlayerConnectionHandler::onPlayerJoin);
        ServerPlayerEvents.AFTER_RESPAWN.register(PlayerConnectionHandler::onPlayerRespawn);
        DreadMod.LOGGER.info("PlayerConnectionHandler registered");
    }

    /**
     * Handles player disconnect.
     * If player is downed: mark as escaped, clear downed state, exit crawl pose.
     */
    private static void onPlayerDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        ServerWorld world = player.getServerWorld();
        DownedPlayersState state = DownedPlayersState.getOrCreate(world);

        if (state.isDowned(player)) {
            DreadMod.LOGGER.info("Player {} disconnected while downed - marking as escaped",
                player.getName().getString());

            state.markEscapedPlayer(player.getUuid());
            state.removeDowned(player.getUuid());
            CrawlPoseHandler.exitCrawlPose(player);
        }

        // Check if we need to transition downed players to SINGLEPLAYER mode
        // This happens when the last other player leaves
        int remainingPlayers = server.getPlayerManager().getCurrentPlayerCount() - 1; // -1 for disconnecting player
        if (remainingPlayers == 1 && server.isSingleplayer()) {
            // Only one player left in integrated server - check for downed players to transition
            for (DownedPlayerData data : state.getAllDowned()) {
                if (data.mode == DreadGameMode.MULTIPLAYER) {
                    DreadMod.LOGGER.info("Last other player left, transitioning downed player {} to SINGLEPLAYER mode",
                        data.playerId);
                    state.transitionToSingleplayer(data.playerId);
                }
            }
        }
    }

    /**
     * Handles player join/reconnect.
     * If player was marked as escaped: apply reconnect penalty.
     */
    private static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        ServerWorld world = player.getServerWorld();
        DownedPlayersState state = DownedPlayersState.getOrCreate(world);

        if (state.wasEscapedPlayer(player.getUuid())) {
            DreadMod.LOGGER.info("Player {} rejoined after escaping - applying reconnect penalty",
                player.getName().getString());

            // Set health to 2 hearts (4.0f half-hearts)
            player.setHealth(4.0f);

            // Grant 3 seconds of damage immunity (vanilla field)
            player.timeUntilRegen = RECONNECT_IMMUNITY_TICKS;

            // Broadcast escape message to all players
            Text message = Text.literal(player.getName().getString() + " narrowly escaped the Dread");
            server.getPlayerManager().broadcast(message, false);

            // Clear escape flag
            state.clearEscapedPlayer(player.getUuid());
        }

        // Check if any downed players need mode transition (SP -> MP)
        // This happens when a second player joins (LAN or dedicated)
        for (DownedPlayerData data : state.getAllDowned()) {
            if (data.mode == DreadGameMode.SINGLEPLAYER) {
                DreadMod.LOGGER.info("Player joined, transitioning downed player {} to MULTIPLAYER mode",
                    data.playerId);
                state.transitionToMultiplayer(data.playerId);
            }
        }
    }

    /**
     * Handle player respawn - apply debuff if player died from Dread in singleplayer.
     */
    private static void onPlayerRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        // Only handle death respawns (alive = false means respawning from death, true means returning from End)
        if (alive) return;

        ServerWorld world = newPlayer.getServerWorld();
        DownedPlayersState state = DownedPlayersState.getOrCreate(world);

        // Check if player just respawned from Dread death
        if (state.hadRecentDreadDeath(newPlayer.getUuid())) {
            DreadMod.LOGGER.info("Player {} respawned from Dread death - applying debuffs",
                newPlayer.getName().getString());

            // Apply Weakness II for 60 seconds
            newPlayer.addStatusEffect(new StatusEffectInstance(
                StatusEffects.WEAKNESS,
                20 * 60,  // 60 seconds
                1,        // Amplifier 1 = Weakness II
                false,    // isAmbient
                true      // showParticles
            ));

            // Apply Slowness I for 30 seconds
            newPlayer.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SLOWNESS,
                20 * 30,  // 30 seconds
                0,        // Amplifier 0 = Slowness I
                false,    // isAmbient
                true      // showParticles
            ));

            // Clear the flag so debuff isn't applied again
            state.clearDreadDeathFlag(newPlayer.getUuid());
        }
    }
}
