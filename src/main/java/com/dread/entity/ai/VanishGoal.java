package com.dread.entity.ai;

import com.dread.entity.DreadEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Despawn Dread with animation when flagged to vanish.
 * Triggered after stare timeout or other vanish conditions.
 */
public class VanishGoal extends Goal {
    private static final int DESPAWN_ANIM_TICKS = 40; // 2 seconds

    private final DreadEntity dread;
    private int animationTimer = 0;

    public VanishGoal(DreadEntity dread) {
        this.dread = dread;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        return this.dread.isVanishing();
    }

    @Override
    public boolean shouldContinue() {
        return this.animationTimer < DESPAWN_ANIM_TICKS;
    }

    @Override
    public void start() {
        this.animationTimer = 0;
        // Stop all movement
        this.dread.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Freeze in place during vanish animation
        this.dread.getNavigation().stop();
        this.dread.setVelocity(0, 0, 0);

        this.animationTimer++;

        // After animation completes, remove entity
        if (this.animationTimer >= DESPAWN_ANIM_TICKS) {
            this.dread.discard();
        }
    }

    @Override
    public void stop() {
        this.animationTimer = 0;
    }
}
