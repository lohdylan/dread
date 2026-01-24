package com.dread.entity;

import com.dread.DreadMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Dread entity - a cosmic horror creature that stalks and terrifies players.
 *
 * Features:
 * - Multiple form variants (3 distinct appearances)
 * - GeckoLib animations (idle, walk, attack)
 * - Persistent form variant across saves
 * - Hostile behavior towards players
 */
public class DreadEntity extends PathAwareEntity implements GeoEntity {
    private static final String NBT_FORM_VARIANT = "FormVariant";
    private static final int EXTINGUISH_RANGE = 8;
    private static final int EXTINGUISH_COOLDOWN_TICKS = 20; // One torch per second

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int currentFormVariant = 0;
    private int extinguishCooldown = 0;
    private List<BlockPos> pendingExtinguish = new ArrayList<>();

    public DreadEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        // Basic AI goals
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(4, new LookAroundGoal(this));

        // Target players
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        // Server-side only - torch extinguishing
        if (!this.getWorld().isClient) {
            handleTorchExtinguishing();
        }
    }

    private void handleTorchExtinguishing() {
        if (extinguishCooldown > 0) {
            extinguishCooldown--;
            return;
        }

        // If no pending torches, scan for new ones
        if (pendingExtinguish.isEmpty()) {
            scanForTorches();
        }

        // Extinguish one torch
        if (!pendingExtinguish.isEmpty()) {
            BlockPos torchPos = pendingExtinguish.remove(0);
            BlockState state = this.getWorld().getBlockState(torchPos);

            // Verify still a torch (might have been broken by player)
            if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)) {
                // Spawn smoke particles (server sends to clients)
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.spawnParticles(
                        ParticleTypes.LARGE_SMOKE,
                        torchPos.getX() + 0.5,
                        torchPos.getY() + 0.5,
                        torchPos.getZ() + 0.5,
                        5, // count
                        0.1, 0.1, 0.1, // spread
                        0.02 // speed
                    );
                }

                // Remove torch
                this.getWorld().setBlockState(torchPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }

            extinguishCooldown = EXTINGUISH_COOLDOWN_TICKS;
        }
    }

    private void scanForTorches() {
        BlockPos entityPos = this.getBlockPos();
        pendingExtinguish.clear();

        for (BlockPos pos : BlockPos.iterateOutwards(entityPos, EXTINGUISH_RANGE, EXTINGUISH_RANGE, EXTINGUISH_RANGE)) {
            BlockState state = this.getWorld().getBlockState(pos);
            if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)) {
                pendingExtinguish.add(pos.toImmutable());
            }
        }

        // Shuffle for random order (horror effect) - Fisher-Yates shuffle
        for (int i = pendingExtinguish.size() - 1; i > 0; i--) {
            int j = this.getWorld().getRandom().nextInt(i + 1);
            BlockPos temp = pendingExtinguish.get(i);
            pendingExtinguish.set(i, pendingExtinguish.get(j));
            pendingExtinguish.set(j, temp);
        }
    }

    // ========================
    // GeckoLib Animation System
    // ========================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    /**
     * Animation predicate - determines which animation to play based on entity state.
     */
    private PlayState predicate(AnimationState<DreadEntity> state) {
        if (state.isMoving()) {
            state.getController().setAnimation(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
        } else {
            state.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ========================
    // Form Variant System
    // ========================

    /**
     * Get the current form variant (0-2).
     */
    public int getFormVariant() {
        return this.currentFormVariant;
    }

    /**
     * Set the form variant (0-2).
     */
    public void setFormVariant(int variant) {
        this.currentFormVariant = Math.max(0, Math.min(2, variant));
    }

    // ========================
    // NBT Serialization
    // ========================

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt(NBT_FORM_VARIANT, this.currentFormVariant);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.currentFormVariant = nbt.getInt(NBT_FORM_VARIANT);
    }

    // ========================
    // Memory Leak Prevention
    // ========================

    @Override
    public void remove(RemovalReason reason) {
        // Clear brain to prevent memory leak (MC-260605)
        if (this.getBrain() != null) {
            this.getBrain().clear();
        }
        super.remove(reason);
    }
}
