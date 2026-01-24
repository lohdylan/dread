package com.dread.registry;

import com.dread.DreadMod;
import com.dread.entity.DreadEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    /**
     * The Dread entity type.
     * Dimensions: 0.6f width, 2.2f height (slightly taller than player)
     * Tracking: 128 blocks range, 2 tick interval, forced velocity updates
     */
    public static final EntityType<DreadEntity> DREAD = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of(DreadMod.MOD_ID, "dread_entity"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DreadEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 2.2f))
            .trackRangeBlocks(128)
            .trackedUpdateRate(2)
            .forceTrackedVelocityUpdates(true)
            .build()
    );

    /**
     * Register entity types and default attributes.
     */
    public static void register() {
        DreadMod.LOGGER.info("Registering Dread entities...");

        // Register default attributes
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
            DREAD,
            createDreadAttributes()
        );

        DreadMod.LOGGER.info("Dread entities registered successfully");
    }

    /**
     * Create default attributes for the Dread entity.
     */
    private static DefaultAttributeContainer.Builder createDreadAttributes() {
        return DreadEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }
}
