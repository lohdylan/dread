package com.dread.mixin;

import com.dread.DreadMod;
import com.dread.death.CrawlPoseHandler;
import com.dread.death.DownedPlayersState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side mixin that clears downed state when admin changes player to creative/spectator.
 * Admin commands take precedence over game mechanics.
 */
@Mixin(ServerPlayerEntity.class)
public class GamemodeChangeMixin {

    @Inject(method = "changeGameMode", at = @At("RETURN"))
    private void dread$onGamemodeChange(GameMode newMode, CallbackInfoReturnable<Boolean> cir) {
        // Only proceed if gamemode actually changed
        if (!cir.getReturnValue()) {
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        ServerWorld world = player.getServerWorld();
        DownedPlayersState state = DownedPlayersState.getOrCreate(world);

        // Check if player is downed AND new gamemode is CREATIVE or SPECTATOR
        if (state.isDowned(player) && (newMode == GameMode.CREATIVE || newMode == GameMode.SPECTATOR)) {
            // Clear downed state
            state.removeDowned(player.getUuid());

            // Exit crawl pose
            CrawlPoseHandler.exitCrawlPose(player);

            // Log the state clear
            DreadMod.LOGGER.info("Cleared downed state for {} (gamemode changed to {})",
                player.getName().getString(), newMode.getName());
        }
    }
}
