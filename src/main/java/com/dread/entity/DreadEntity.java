package com.dread.entity;

import com.dread.DreadMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

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

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int currentFormVariant = 0;

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
