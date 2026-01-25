package com.dread.entity;

import com.dread.DreadMod;
import com.dread.config.DreadConfigLoader;
import com.dread.entity.ai.StareStandoffGoal;
import com.dread.entity.ai.VanishGoal;
import com.dread.sound.DreadSoundManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
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
    private static final String NBT_SPAWN_ANIM_PLAYED = "SpawnAnimPlayed";
    private static final int EXTINGUISH_RANGE = 8;
    private static final int EXTINGUISH_COOLDOWN_TICKS = 20; // One torch per second
    private static final int PROXIMITY_SOUND_COOLDOWN = 40; // 2 seconds

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private DreadFormVariant formVariant = DreadFormVariant.BASE;
    private boolean hasPlayedSpawnAnimation = false;
    private boolean isVanishing = false;
    private int extinguishCooldown = 0;
    private int proximitySoundCooldown = 0;
    private List<BlockPos> pendingExtinguish = new ArrayList<>();

    public DreadEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * Check if entity is in vanishing state.
     */
    public boolean isVanishing() {
        return this.isVanishing;
    }

    /**
     * Set vanishing state (triggers VanishGoal).
     */
    public void setVanishing(boolean vanishing) {
        this.isVanishing = vanishing;
    }

    @Override
    protected void initGoals() {
        // AI goals with priorities (lower number = higher priority)
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new VanishGoal(this)); // Highest priority when vanishing
        this.goalSelector.add(2, new StareStandoffGoal(this)); // Freeze when watched
        this.goalSelector.add(3, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));

        // Target players
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public boolean tryAttack(Entity target) {
        var config = DreadConfigLoader.getConfig();

        // Skip attack if mod disabled
        if (!config.modEnabled) {
            return false;
        }

        // Apply configured damage
        float damage = config.dreadAttackDamage;

        if (target instanceof LivingEntity living) {
            living.damage(this.getDamageSources().mobAttack(this), damage);
            return true;
        }

        return super.tryAttack(target);
    }

    @Override
    public EntityData initialize(
        ServerWorldAccess world,
        LocalDifficulty difficulty,
        SpawnReason spawnReason,
        @Nullable EntityData entityData
    ) {
        // Select form variant based on world day
        long worldDay = world.toServerWorld().getTimeOfDay() / 24000L;
        this.formVariant = DreadFormVariant.fromWorldDay(worldDay);

        DreadMod.LOGGER.debug("Dread spawned on day {} with variant {}", worldDay, formVariant);

        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Override
    public void tick() {
        super.tick();

        // Server-side only - torch extinguishing and proximity audio
        if (!this.getWorld().isClient) {
            handleTorchExtinguishing();
            handleProximitySound();
        }
    }

    private void handleProximitySound() {
        if (proximitySoundCooldown > 0) {
            proximitySoundCooldown--;
            return;
        }

        // Find nearest player
        PlayerEntity nearestPlayer = this.getWorld().getClosestPlayer(this, 24.0);
        if (nearestPlayer == null) return;

        float distance = (float) this.distanceTo(nearestPlayer);

        // Only trigger if within audio range
        if (distance < 16.0f && this.getWorld() instanceof ServerWorld serverWorld) {
            DreadSoundManager.playProximitySound(serverWorld, this.getBlockPos(), distance);
            proximitySoundCooldown = PROXIMITY_SOUND_COOLDOWN;
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
        // Main movement controller
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            // Vanishing takes priority over death
            if (this.isVanishing) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("despawn"));
            }

            if (this.isDead()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("despawn"));
            }

            if (!hasPlayedSpawnAnimation) {
                hasPlayedSpawnAnimation = true;
                return state.setAndContinue(RawAnimation.begin()
                    .thenPlay("spawn")
                    .thenLoop("idle"));
            }

            if (this.isAttacking()) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("attack"));
            }

            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }

            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        // Separate head tracking controller (concurrent)
        controllers.add(new AnimationController<>(this, "head", 0, state -> {
            return state.setAndContinue(RawAnimation.begin().thenLoop("head_track"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ========================
    // Form Variant System
    // ========================

    /**
     * Get the current form variant texture index (0-2).
     */
    public int getFormVariant() {
        return this.formVariant.getTextureIndex();
    }

    /**
     * Get the form variant enum.
     */
    public DreadFormVariant getFormVariantEnum() {
        return this.formVariant;
    }

    /**
     * Set the form variant (0-2).
     */
    public void setFormVariant(int variant) {
        this.formVariant = DreadFormVariant.fromIndex(variant);
    }

    // ========================
    // NBT Serialization
    // ========================

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt(NBT_FORM_VARIANT, this.formVariant.getTextureIndex());
        nbt.putBoolean(NBT_SPAWN_ANIM_PLAYED, this.hasPlayedSpawnAnimation);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.formVariant = DreadFormVariant.fromIndex(nbt.getInt(NBT_FORM_VARIANT));
        this.hasPlayedSpawnAnimation = nbt.getBoolean(NBT_SPAWN_ANIM_PLAYED);
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
