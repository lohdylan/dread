package com.dread.death;

import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side utility for managing player pose during downed state.
 * Sets SWIMMING pose (prone/crawling) when downed, resets to STANDING when revived.
 * The DataTracker automatically syncs pose changes to all clients.
 */
public class CrawlPoseHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("dread");

    /**
     * Sets player to crawling/prone pose when entering downed state.
     * Uses SWIMMING pose which renders the player prone on the ground.
     *
     * @param player The player entering downed state
     */
    public static void enterCrawlPose(ServerPlayerEntity player) {
        player.setPose(EntityPose.SWIMMING);
        LOGGER.debug("Player {} entered crawl pose", player.getName().getString());
    }

    /**
     * Resets player to standing pose when revived or transitioning to spectator.
     *
     * @param player The player exiting downed state
     */
    public static void exitCrawlPose(ServerPlayerEntity player) {
        player.setPose(EntityPose.STANDING);
        LOGGER.debug("Player {} exited crawl pose", player.getName().getString());
    }
}
