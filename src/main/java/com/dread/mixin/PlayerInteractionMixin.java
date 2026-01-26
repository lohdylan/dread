package com.dread.mixin;

import com.dread.death.DownedPlayersState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side mixin that blocks all block interactions when player is downed.
 * Prevents opening chests, using buttons, levers, doors, crafting tables, etc.
 */
@Mixin(ServerPlayerInteractionManager.class)
public class PlayerInteractionMixin {

    @Shadow
    protected ServerPlayerEntity player;

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void dread$blockInteractionsWhenDowned(
        ServerPlayerEntity player,
        World world,
        ItemStack stack,
        Hand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        DownedPlayersState state = DownedPlayersState.getOrCreate((net.minecraft.server.world.ServerWorld) world);
        if (state.isDowned(player)) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}
