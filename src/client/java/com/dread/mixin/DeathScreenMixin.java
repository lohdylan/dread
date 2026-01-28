package com.dread.mixin;

import com.dread.client.DownedStateClientHandler;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents the death screen from showing when player is in downed state.
 * Player should see the world (blurred) with the countdown timer, not the death screen.
 */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

    protected DeathScreenMixin(Text title) {
        super(title);
    }

    // Guard to prevent recursive calls when closing the screen
    private static boolean isClosingScreen = false;

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private void dread$preventDeathScreenWhenDowned(CallbackInfo ci) {
        if (isClosingScreen) {
            ci.cancel();
            return;
        }

        if (DownedStateClientHandler.isDownedEffectActive()) {
            // Close the death screen - player should see the world with blur effect
            if (this.client != null) {
                isClosingScreen = true;
                try {
                    this.client.setScreen(null);
                } finally {
                    isClosingScreen = false;
                }
            }
            ci.cancel();
        }
    }
}
