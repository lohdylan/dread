package com.dread.client;

import com.dread.entity.DreadEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib renderer for the Dread entity.
 *
 * Features:
 * - Renders entity using GeoModel geometry
 * - AutoGlowingGeoLayer for emissive eyes and tentacle tips
 * - Supports texture variant switching via model
 */
public class DreadEntityRenderer extends GeoEntityRenderer<DreadEntity> {
    public DreadEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new DreadEntityModel());

        // Add emissive layer for glowing eyes and tentacle tips
        // AutoGlowingGeoLayer automatically detects and renders _glowmask textures
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public Identifier getTextureLocation(DreadEntity entity) {
        // Delegate to model for texture variant selection
        return this.model.getTextureResource(entity);
    }
}
