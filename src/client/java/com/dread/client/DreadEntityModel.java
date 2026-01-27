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
 * - Cinematic-synchronized texture selection with accelerating rune pulse
 */
public class DreadEntityModel extends GeoModel<DreadEntity> {
    private static final Identifier MODEL = Identifier.of("dread", "geo/dread_entity.geo.json");
    private static final Identifier ANIMATION = Identifier.of("dread", "animations/dread_entity.animation.json");

    // Idle state textures (dim runes)
    private static final Identifier TEXTURE_BASE = Identifier.of("dread", "textures/entity/dread_base.png");
    private static final Identifier TEXTURE_V2 = Identifier.of("dread", "textures/entity/dread_variant2.png");
    private static final Identifier TEXTURE_V3 = Identifier.of("dread", "textures/entity/dread_variant3.png");

    @Override
    public Identifier getModelResource(DreadEntity entity) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(DreadEntity entity) {
        // Check if entity is in death cinematic
        if (entity.isPlayingDeathGrab()) {
            int tick = DeathCinematicClientHandler.getCinematicTimer();

            if (tick >= 0) { // Cinematic active
                // Face close-up phase (30-90 ticks): Eyes open texture
                if (DeathCinematicClientHandler.isInFaceCloseup()) {
                    return getEyesOpenTexture(entity.getFormVariant());
                }

                // Pull-back phase (0-30 ticks): Accelerating rune pulse
                int pulseFrame = calculatePulseFrame(tick);
                return getPulseTexture(entity.getFormVariant(), pulseFrame);
            }
        }

        // Idle state: Base texture (dim runes)
        return getIdleTexture(entity.getFormVariant());
    }

    @Override
    public Identifier getAnimationResource(DreadEntity entity) {
        return ANIMATION;
    }

    /**
     * Calculate rune pulse frame based on accelerating heartbeat rhythm.
     * Pulse accelerates from slow (1.0s/beat) to fast (0.3s/beat) over 1.5s pull-back.
     *
     * @param tick Cinematic tick (0-30 for pull-back phase)
     * @return Pulse frame 0-2 (dim, medium, bright)
     */
    private int calculatePulseFrame(int tick) {
        // Three zones with accelerating heartbeat
        if (tick < 10) {
            // Zone 1: Slow (20-tick period = 1.0s at 20 ticks/sec)
            // Toggle between dim(0) and medium(1)
            return ((tick / 10) % 2 == 0) ? 0 : 1;
        } else if (tick < 20) {
            // Zone 2: Medium (12-tick period = 0.6s)
            // Toggle between dim(0), medium(1), bright(2)
            int phase = ((tick - 10) / 4) % 3;
            return phase;
        } else {
            // Zone 3: Fast (6-tick period = 0.3s)
            // Rapid pulse to bright
            int phase = ((tick - 20) / 2) % 3;
            return phase;
        }
    }

    /**
     * Get pulse texture for entity variant.
     * @param variant Form variant (0-2)
     * @param pulseFrame Pulse brightness (0=dim, 1=medium, 2=bright)
     */
    private Identifier getPulseTexture(int variant, int pulseFrame) {
        String baseName = getVariantBaseName(variant);
        return Identifier.of("dread", "textures/entity/" + baseName + "_pulse_" + pulseFrame + ".png");
    }

    /**
     * Get eyes-open texture for entity variant (used during face close-up phase).
     */
    private Identifier getEyesOpenTexture(int variant) {
        String baseName = getVariantBaseName(variant);
        return Identifier.of("dread", "textures/entity/" + baseName + "_eyes_open.png");
    }

    /**
     * Get idle texture for entity variant (dim runes).
     */
    private Identifier getIdleTexture(int variant) {
        return switch (variant) {
            case 1 -> TEXTURE_V2;
            case 2 -> TEXTURE_V3;
            default -> TEXTURE_BASE;
        };
    }

    /**
     * Get base texture name for entity variant.
     */
    private String getVariantBaseName(int variant) {
        return switch (variant) {
            case 1 -> "dread_variant2";
            case 2 -> "dread_variant3";
            default -> "dread_base";
        };
    }
}
