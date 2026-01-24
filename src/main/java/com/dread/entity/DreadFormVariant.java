package com.dread.entity;

/**
 * Form variants for the Dread entity.
 * Form changes based on world progression to escalate horror over time.
 */
public enum DreadFormVariant {
    BASE(0),      // Days 1-3: Base form, 4 face tentacles
    EVOLVED(1),   // Days 4-7: More tentacles, larger eyes
    ELDRITCH(2);  // Days 8+: Most distorted, multiple eye spots

    private final int textureIndex;

    DreadFormVariant(int textureIndex) {
        this.textureIndex = textureIndex;
    }

    public int getTextureIndex() {
        return textureIndex;
    }

    /**
     * Select form variant based on world day count.
     * @param worldDay The current world day (getTimeOfDay() / 24000)
     * @return The appropriate variant for this progression level
     */
    public static DreadFormVariant fromWorldDay(long worldDay) {
        if (worldDay >= 8) {
            return ELDRITCH;
        } else if (worldDay >= 4) {
            return EVOLVED;
        } else {
            return BASE;
        }
    }

    public static DreadFormVariant fromIndex(int index) {
        return switch (index) {
            case 1 -> EVOLVED;
            case 2 -> ELDRITCH;
            default -> BASE;
        };
    }
}
