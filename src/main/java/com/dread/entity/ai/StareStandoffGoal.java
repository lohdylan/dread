package com.dread.entity.ai;

import com.dread.entity.DreadEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.EnumSet;
import java.util.List;

/**
 * Weeping Angel / SCP-173 mechanic: Dread freezes when player is looking at it.
 * If player stares too long (30 seconds), Dread vanishes.
 */
public class StareStandoffGoal extends Goal {
    private static final double FOV_THRESHOLD = 0.85; // ~31 degree cone
    private static final int VANISH_AFTER_TICKS = 600; // 30 seconds at 20 tps
    private static final double DETECTION_RANGE = 16.0;

    private final DreadEntity dread;
    private PlayerEntity watchingPlayer;
    private int stareTimer = 0;

    public StareStandoffGoal(DreadEntity dread) {
        this.dread = dread;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        this.watchingPlayer = findWatchingPlayer();
        return this.watchingPlayer != null;
    }

    @Override
    public boolean shouldContinue() {
        if (this.watchingPlayer == null || !this.watchingPlayer.isAlive()) {
            return false;
        }

        // Check if player still watching
        if (!isPlayerLookingAtEntity(this.watchingPlayer)) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.stareTimer = 0;
        // Stop all movement
        this.dread.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.watchingPlayer == null) {
            return;
        }

        // Freeze in place - stop navigation
        this.dread.getNavigation().stop();

        // Increment stare timer
        this.stareTimer++;

        // If stared at too long, vanish
        if (this.stareTimer >= VANISH_AFTER_TICKS) {
            this.dread.setVanishing(true);
        }
    }

    @Override
    public void stop() {
        this.watchingPlayer = null;
        this.stareTimer = 0;
    }

    /**
     * Find the closest player within range who is looking at this entity.
     */
    private PlayerEntity findWatchingPlayer() {
        if (!(this.dread.getWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }
        List<? extends PlayerEntity> players = serverWorld.getPlayers();
        PlayerEntity closestWatcher = null;
        double closestDistance = Double.MAX_VALUE;

        for (PlayerEntity player : players) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }

            double distance = this.dread.squaredDistanceTo(player);
            if (distance > DETECTION_RANGE * DETECTION_RANGE) {
                continue;
            }

            if (isPlayerLookingAtEntity(player)) {
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestWatcher = player;
                }
            }
        }

        return closestWatcher;
    }

    /**
     * Check if player is looking at this entity using FOV cone + raycast obstruction check.
     */
    private boolean isPlayerLookingAtEntity(PlayerEntity player) {
        // Get player's look direction
        Vec3d playerLook = player.getRotationVec(1.0f);

        // Vector from player to entity
        Vec3d toEntity = this.dread.getPos().subtract(player.getEyePos()).normalize();

        // Dot product - measures alignment (-1 to 1)
        double dot = playerLook.dotProduct(toEntity);

        // Check if within FOV cone
        if (dot < FOV_THRESHOLD) {
            return false;
        }

        // Raycast to check for obstruction
        Vec3d start = player.getEyePos();
        Vec3d end = this.dread.getPos().add(0, this.dread.getHeight() / 2, 0);

        HitResult hitResult = this.dread.getWorld().raycast(new RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            player
        ));

        // If raycast hits a block before reaching entity, player can't see it
        return hitResult.getType() == HitResult.Type.MISS;
    }
}
