package com.dread.death;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.TridentItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side handler that blocks all player attacks while in downed state.
 * Prevents both melee attacks (via AttackEntityCallback) and projectile attacks
 * (via UseItemCallback for bows, crossbows, and tridents).
 */
public class AttackPreventionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("dread");

    /**
     * Registers attack prevention callbacks.
     * Should be called during mod initialization after DreadDeathManager.register().
     */
    public static void register() {
        // Block melee attacks for downed players
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world instanceof ServerWorld serverWorld) {
                DownedPlayersState state = DownedPlayersState.getOrCreate(serverWorld);
                if (state.isDowned(player.getUuid())) {
                    LOGGER.debug("Blocked melee attack from downed player: {}", player.getName().getString());
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        // Block projectile weapon use for downed players
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world instanceof ServerWorld serverWorld) {
                DownedPlayersState state = DownedPlayersState.getOrCreate(serverWorld);
                if (state.isDowned(player.getUuid())) {
                    ItemStack stack = player.getStackInHand(hand);
                    // Only block ranged weapons (bow, crossbow) and tridents
                    if (stack.getItem() instanceof RangedWeaponItem ||
                        stack.getItem() instanceof TridentItem) {
                        LOGGER.debug("Blocked projectile attack from downed player: {}", player.getName().getString());
                        return TypedActionResult.fail(stack);
                    }
                }
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });

        LOGGER.info("Registered AttackPreventionHandler");
    }
}
