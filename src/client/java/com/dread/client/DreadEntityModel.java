package com.dread.client;

import com.dread.entity.DreadEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib model for the Dread entity.
 *
 * Provides:
 * - Geometry model (humanoid with tentacles)
 * - Animation definitions (idle, walk, attack, spawn, despawn, head_track)
 * - Texture variant selection based on entity's form variant
 */
public class DreadEntityModel extends GeoModel<DreadEntity> {
    private static final Identifier MODEL = Identifier.of("dread", "geo/dread_entity.geo.json");
    private static final Identifier ANIMATION = Identifier.of("dread", "animations/dread_entity.animation.json");

    private static final Identifier TEXTURE_BASE = Identifier.of("dread", "textures/entity/dread_base.png");
    private static final Identifier TEXTURE_V2 = Identifier.of("dread", "textures/entity/dread_variant2.png");
    private static final Identifier TEXTURE_V3 = Identifier.of("dread", "textures/entity/dread_variant3.png");

    @Override
    public Identifier getModelResource(DreadEntity entity) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(DreadEntity entity) {
        // Select texture based on entity's form variant (0-2)
        return switch (entity.getFormVariant()) {
            case 1 -> TEXTURE_V2;
            case 2 -> TEXTURE_V3;
            default -> TEXTURE_BASE;
        };
    }

    @Override
    public Identifier getAnimationResource(DreadEntity entity) {
        return ANIMATION;
    }
}
