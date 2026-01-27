package com.dread.death;

import com.dread.DreadMod;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
    }
}
