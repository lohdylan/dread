package com.dread.sound;

import com.dread.DreadMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    // Low droning/humming for tension building
    public static final SoundEvent DREAD_AMBIENT = registerSound("dread_ambient");

    // Sharp loud noise for spawn reveal
    public static final SoundEvent DREAD_JUMPSCARE = registerSound("dread_jumpscare");

    // Subtle sound when Dread is nearby but unseen
    public static final SoundEvent DREAD_PROXIMITY = registerSound("dread_proximity");

    // Off-putting sound as spawn probability increases
    public static final SoundEvent DANGER_RISING = registerSound("danger_rising");

    // Death sequence audio when killed by Dread
    public static final SoundEvent DREAD_DEATH = registerSound("dread_death");

    // Grab impact sound for death cinematic start
    public static final SoundEvent GRAB_IMPACT = registerSound("grab_impact");

    private static SoundEvent registerSound(String id) {
        Identifier identifier = Identifier.of(DreadMod.MOD_ID, id);
        return Registry.register(
            Registries.SOUND_EVENT,
            identifier,
            SoundEvent.of(identifier)
        );
    }

    public static void register() {
        DreadMod.LOGGER.info("Registering Dread sounds");
    }
}
