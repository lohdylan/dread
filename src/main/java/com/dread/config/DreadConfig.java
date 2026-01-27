package com.dread.config;

import com.google.gson.annotations.SerializedName;

public class DreadConfig {
    // Spawn configuration (from Phase 2 tuning)
    public float baseSpawnChancePerSecond = 0.005f; // 0.5% per second
    public float miningBonusPerBlock = 0.001f;      // +0.1% per block mined
    public int dayEscalationCap = 20;              // Max day for multiplier

    // Damage configuration
    public float dreadAttackDamage = 20.0f;        // One-shot kill (10 hearts)

    // Feature toggles
    public boolean modEnabled = true;
    public boolean skipDeathCinematic = false;     // Cinematic plays by default
    public boolean disableDownedEffects = false;   // Force disable blur/vignette

    // Camera shake intensity (0 = disabled, 100 = full horror)
    public int cameraShakeIntensity = 100;

    // Downed state timeouts (seconds)
    public int singleplayerDownedTimeout = 30;   // 30 seconds for singleplayer
    public int multiplayerDownedTimeout = 300;   // 300 seconds for multiplayer

    // Documentation fields (appear as comments in JSON)
    @SerializedName("_comment_spawn")
    public final String comment1 = "baseSpawnChancePerSecond: Base probability per tick (0.005 = 0.5%). miningBonusPerBlock: Added per block mined. dayEscalationCap: Max world day for multiplier.";

    @SerializedName("_comment_damage")
    public final String comment2 = "dreadAttackDamage: Damage dealt by Dread (20.0 = instant kill for 20 HP players).";

    @SerializedName("_comment_features")
    public final String comment3 = "modEnabled: Master toggle. skipDeathCinematic: Skip 4.5s death camera lock. disableDownedEffects: Force disable blur/vignette shaders.";

    @SerializedName("_comment_shake")
    public final String comment4 = "cameraShakeIntensity: Camera shake strength during death cinematic (0-100). Set to 0 to disable shake while keeping cinematic. Lower values for motion-sensitive players.";

    @SerializedName("_comment_timeouts")
    public final String comment5 = "singleplayerDownedTimeout: Seconds in downed state before death in singleplayer (normal respawn). multiplayerDownedTimeout: Seconds in multiplayer before permanent spectator.";
}
