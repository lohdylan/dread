# Phase 14: Animated Entity Textures - Research

**Researched:** 2026-01-27
**Domain:** Entity texture animation, emissive rendering, cinematic synchronization
**Confidence:** MEDIUM (GeckoLib 4 documentation verified, but project uses Fabric 1.21.1 not NeoForge 1.21.4 as stated in context)

## Summary

Animated entity textures for GeckoLib entities in Minecraft Fabric 1.21.1 can be achieved through three primary approaches: (1) vanilla `.mcmeta` animated textures for simple frame-based animations, (2) runtime texture swapping in the GeoModel's `getTextureResource()` method synchronized with cinematic state, or (3) custom `GeoRenderLayer` implementations for advanced per-frame texture manipulation.

The key challenge is that **GeckoLib 4's `.mcmeta` animated textures are incompatible with `AutoGlowingGeoLayer` emissive textures**, which creates a conflict for the requirement to have pulsing emissive runes during the death sequence. Additionally, AMD GPUs have well-documented performance issues with animated textures in Minecraft due to texture atlas upload overhead (20 times per second).

**Primary recommendation:** Use runtime texture swapping (approach #2) with pre-rendered texture variants for each animation state (idle_eyes_closed, idle_eyes_open, kill_runes_dim, kill_runes_bright_1-3, etc.) synchronized to the cinematic timer (0-30 ticks pull-back, 30-90 ticks face close-up). This avoids `.mcmeta` incompatibility with emissive layers and AMD GPU performance issues while maintaining full control over timing.

## Standard Stack

The established libraries/tools for entity texture animation in this project:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| GeckoLib | 4.7.1 | Entity modeling, animation, and rendering | Already integrated, provides `GeoEntityRenderer`, `AutoGlowingGeoLayer` |
| Fabric API | 0.116.8+1.21.1 | Mod loader infrastructure | Project standard |
| Minecraft | 1.21.1 | Base game rendering system | Fixed by project |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `.mcmeta` animation | Native Minecraft | Frame-based texture animation | Simple looping animations without emissive requirements |
| Entity Texture Features | Optional (if needed) | Advanced texture features | Only if vanilla/GeckoLib approaches insufficient |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Runtime texture swapping | `.mcmeta` animation | Automatic but incompatible with emissive textures |
| Pre-rendered variants | UV offset manipulation | More flexible but significantly more complex, GeckoLib doesn't expose UV control |
| Static texture layers | Custom shader via Satin API | Maximum flexibility but scope creep, overkill for requirement |

**Installation:**
```bash
# Already installed - no additional dependencies required
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/resources/assets/dread/textures/entity/
├── dread_base.png                    # Base texture (idle state)
├── dread_base_glowmask.png           # Emissive runes (dim, idle state)
├── dread_kill_runes_pulse_1.png      # Kill sequence texture variant 1
├── dread_kill_runes_pulse_1_glowmask.png
├── dread_kill_runes_pulse_2.png      # Kill sequence texture variant 2
├── dread_kill_runes_pulse_2_glowmask.png
├── dread_kill_runes_pulse_3.png      # Kill sequence texture variant 3 (brightest)
├── dread_kill_runes_pulse_3_glowmask.png
├── dread_eyes_open.png               # Eyes open texture (face close-up)
├── dread_eyes_open_glowmask.png      # Eyes emissive (yellow/amber)
└── ... (variants 2 and 3 follow same pattern)
```

### Pattern 1: Cinematic-Synchronized Texture Swapping

**What:** Override `GeoModel#getTextureResource()` to return different textures based on entity state and cinematic timer.

**When to use:** When animations must synchronize with external game state (cinematic camera, sound events, etc.) and require emissive textures.

**Example:**
```java
// Source: DreadEntityModel.java (existing pattern)
@Override
public Identifier getTextureResource(DreadEntity entity) {
    // Check if entity is in death cinematic
    if (entity.isPlayingDeathGrab()) {
        // Get cinematic progress from client handler
        int cinematicTick = DeathCinematicClientHandler.getCinematicTimer();

        // PULL-BACK PHASE (0-30 ticks): Accelerating rune pulse
        if (cinematicTick < 30) {
            int pulseFrame = calculateRunePulseFrame(cinematicTick, 0, 30);
            return getRunePulseTexture(entity.getFormVariant(), pulseFrame);
        }

        // FACE CLOSE-UP PHASE (30-90 ticks): Eyes open, intense rune glow
        return getEyesOpenTexture(entity.getFormVariant());
    }

    // Idle state: Subtle writhing, dim runes, eyes closed
    return getIdleTexture(entity.getFormVariant());
}

private int calculateRunePulseFrame(int tick, int startTick, int endTick) {
    // Heartbeat accelerates from slow (1.0s) to fast (0.3s)
    // Returns frame index 0-2 (dim -> medium -> bright)
    float progress = (float)(tick - startTick) / (endTick - startTick);
    // Pulse timing logic here
    return pulseFrame;
}
```

### Pattern 2: Emissive Texture Layer Setup

**What:** Use `AutoGlowingGeoLayer` for automatic fullbright rendering of `_glowmask` textures.

**When to use:** When parts of texture need to glow regardless of lighting (eyes, runes, magic effects).

**Example:**
```java
// Source: DreadEntityRenderer.java (already implemented)
public DreadEntityRenderer(EntityRendererFactory.Context context) {
    super(context, new DreadEntityModel());

    // AutoGlowingGeoLayer detects textures ending in _glowmask
    this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
}
```

**Important:** `AutoGlowingGeoLayer` automatically looks for `{texture_name}_glowmask.png` alongside base texture. For `dread_base.png`, it expects `dread_base_glowmask.png`. This must be coordinated with texture swapping.

### Pattern 3: Animation State Synchronization

**What:** Expose cinematic timer from client handler for texture logic to consume.

**When to use:** When renderer/model needs to sync with cinematic phase timing.

**Example:**
```java
// Source: DeathCinematicClientHandler.java (add new method)
/**
 * Get current cinematic timer tick for texture synchronization.
 * Returns -1 if cinematic not active.
 */
public static int getCinematicTimer() {
    return cinematicActive ? cinematicTimer : -1;
}

/**
 * Get current cinematic phase for animation coordination.
 */
public static CinematicPhase getCurrentPhase() {
    return cinematicActive ? currentPhase : null;
}
```

### Anti-Patterns to Avoid

- **Using .mcmeta with emissive textures:** GeckoLib 4 documentation explicitly states these are incompatible. Animated textures will not work with `AutoGlowingGeoLayer`.
- **Texture swapping in render method:** Avoid changing textures in `GeoEntityRenderer#render()`. Use `GeoModel#getTextureResource()` for state-based texture selection (GeckoLib caches texture lookups per-entity).
- **Rapid texture atlas updates:** Each texture swap triggers atlas rebuild on AMD GPUs. Limit texture changes to key cinematic moments (phase transitions) rather than every tick.
- **Storing mutable state in renderer:** Renderers are singletons. Store animation state (timer, phase, etc.) in entity or static client handler, not renderer fields.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Emissive texture rendering | Custom fullbright shader | `AutoGlowingGeoLayer` | Handles lighting, transparency blending, and texture atlas integration automatically |
| Texture animation timing | Manual frame counter with modulo | `.mcmeta` `frametime` property | Vanilla system handles interpolation, frame order, per-frame timing (except when needing emissive) |
| Cinematic state sync | Custom packet system | Existing `DeathCinematicClientHandler` | Already tracks timer, phase, and Dread entity ID |
| Heartbeat pulse curve | Linear interpolation | Easing functions (quadratic, sine) | Biological rhythms need acceleration curves, not linear |

**Key insight:** Texture manipulation in entity rendering is deceptively complex. Vanilla Minecraft's texture atlas system, GeckoLib's render layer architecture, and AMD GPU driver quirks create edge cases that aren't obvious. Use proven patterns (GeoModel texture swapping, AutoGlowingGeoLayer) rather than low-level render manipulation.

## Common Pitfalls

### Pitfall 1: Animated Texture Performance Collapse on AMD GPUs

**What goes wrong:** Minecraft uploads entire texture atlas to GPU every tick when animated textures are present. With hundreds of animated textures from mods, this causes severe FPS drops (1-6 FPS) specifically on AMD GPUs.

**Why it happens:** AMD OpenGL drivers handle texture updates differently than NVIDIA. Minecraft keeps animated textures in CPU memory and modifies the GPU texture atlas 20 times per second. AMD drivers stall the render pipeline during these updates.

**How to avoid:**
- Minimize number of distinct animated textures (use texture swapping instead of .mcmeta where possible)
- Use pre-rendered texture variants rather than runtime animation
- If using .mcmeta, ensure only a few frames (2-4 maximum)
- Consider config option to disable texture animations for AMD users

**Warning signs:**
- FPS drops only on AMD hardware
- Performance degrades with more mods installed
- Profiler shows high time in texture atlas updates

**Sources:**
- [AMD Radeon Drivers bring Minecraft down to <1fps (FoamFix Issue #182)](https://github.com/asiekierka/FoamFix/issues/182)
- [Add VanillaFix to fix texture animation performance (FTB Issue #5550)](https://github.com/FTBTeam/FTB-Modpack-Issues/issues/5550)

### Pitfall 2: GeckoLib Animated Textures + Emissive Textures Incompatibility

**What goes wrong:** When using `.mcmeta` animated textures with GeckoLib entities, the `AutoGlowingGeoLayer` emissive textures do not render correctly or at all.

**Why it happens:** GeckoLib 4 documentation explicitly states: "GeckoLib animated textures are currently not compatible with GeckoLib emissive textures." The animated texture system and emissive layer system conflict in the render pipeline.

**How to avoid:**
- Use runtime texture swapping instead of `.mcmeta` animation
- Create separate texture files for each animation state rather than atlas frames
- Coordinate texture swapping with glowmask textures (both must change together)

**Warning signs:**
- Emissive glow disappears when texture animates
- Only base texture animates, glowmask stays static
- Texture appears stretched or misaligned

**Sources:**
- [GeckoLib Animated Textures (Geckolib4) Wiki](https://github.com/bernie-g/geckolib/wiki/Animated-Textures-(Geckolib4))

### Pitfall 3: Cinematic Timer Out-of-Sync Between Client and Server

**What goes wrong:** Texture animations appear delayed, incorrect, or out of sync with camera movement because the renderer is reading stale cinematic state.

**Why it happens:** The cinematic system runs client-side in `DeathCinematicClientHandler`, but the `DreadEntity` state (`isPlayingDeathGrab`) is synced from server. If the texture logic reads server-side state while cinematic timing is client-side, they can desync.

**How to avoid:**
- Read cinematic timer directly from `DeathCinematicClientHandler` on client
- Don't rely on entity NBT or synced data fields for frame-accurate timing
- Use cinematic phase enum (`THIRD_PERSON_PULLBACK` vs `FACE_CLOSEUP`) for major transitions
- Cache texture calculations per-frame to avoid multiple lookups

**Warning signs:**
- Eyes open 1-2 ticks before face close-up starts
- Rune pulse continues after cinematic ends
- Texture changes are "jumpy" rather than smooth transitions

### Pitfall 4: Texture Resource Caching Prevents Updates

**What goes wrong:** After changing texture selection logic in `getTextureResource()`, the entity continues rendering the old texture even though the state has changed.

**Why it happens:** GeckoLib may cache texture resource lookups per entity. If texture selection depends on mutable state (cinematic timer, animation phase), the cache may not invalidate when state changes.

**How to avoid:**
- Ensure `getTextureResource()` reads from authoritative state sources (client handler, entity fields)
- Return different `Identifier` instances for different states (don't reuse same object)
- If caching is aggressive, consider invalidating entity render data on phase transitions

**Warning signs:**
- Texture doesn't update despite state changes
- First frame of animation works, subsequent frames don't
- Texture updates only after entity re-renders for other reasons

## Code Examples

Verified patterns from official sources and project codebase:

### Cinematic-Aware Texture Selection

```java
// Source: Adapted from DreadEntityModel.java existing pattern
@Override
public Identifier getTextureResource(DreadEntity entity) {
    int formVariant = entity.getFormVariant();

    // Check if entity is in death cinematic (client-side check)
    if (entity.isPlayingDeathGrab()) {
        // Query cinematic state from client handler
        int tick = DeathCinematicClientHandler.getCinematicTimer();

        if (tick >= 0) { // Cinematic active
            if (tick < 30) {
                // PULL-BACK PHASE: Accelerating rune pulse
                return getRunePulseTexture(formVariant, tick);
            } else {
                // FACE CLOSE-UP PHASE: Eyes open, intense glow
                return getEyesOpenTexture(formVariant);
            }
        }
    }

    // Idle state: Subtle writhing, dim runes, eyes closed
    return getIdleTexture(formVariant);
}

private Identifier getRunePulseTexture(int variant, int tick) {
    // Calculate pulse frame based on accelerating heartbeat
    // Tick 0-10: 1 beat per second (20 ticks) -> dim
    // Tick 10-20: 1 beat per 0.6s (12 ticks) -> medium
    // Tick 20-30: 1 beat per 0.3s (6 ticks) -> bright

    int pulseFrame = 0; // 0=dim, 1=medium, 2=bright

    if (tick < 10) {
        pulseFrame = (tick % 20) < 10 ? 0 : 1;
    } else if (tick < 20) {
        pulseFrame = (tick % 12) < 6 ? 0 : 2;
    } else {
        pulseFrame = (tick % 6) < 3 ? 1 : 2;
    }

    return Identifier.of("dread", "textures/entity/" +
        switch (variant) {
            case 1 -> "dread_variant2";
            case 2 -> "dread_variant3";
            default -> "dread_base";
        } + "_pulse_" + pulseFrame + ".png");
}

private Identifier getEyesOpenTexture(int variant) {
    return Identifier.of("dread", "textures/entity/" +
        switch (variant) {
            case 1 -> "dread_variant2_eyes_open.png";
            case 2 -> "dread_variant3_eyes_open.png";
            default -> "dread_base_eyes_open.png";
        });
}

private Identifier getIdleTexture(int variant) {
    // Existing logic from DreadEntityModel
    return switch (variant) {
        case 1 -> Identifier.of("dread", "textures/entity/dread_variant2.png");
        case 2 -> Identifier.of("dread", "textures/entity/dread_variant3.png");
        default -> Identifier.of("dread", "textures/entity/dread_base.png");
    };
}
```

### Heartbeat Pulse Timing Calculator

```java
// Source: Original design based on user requirements
/**
 * Calculate rune pulse brightness for heartbeat rhythm.
 * Pulse accelerates from slow (1.0s/beat) to fast (0.3s/beat) during pull-back phase.
 *
 * @param tick Current cinematic tick (0-30 for pull-back phase)
 * @return Brightness level 0.0 (dim) to 1.0 (bright)
 */
public static float calculateRunePulseBrightness(int tick) {
    // Define three intensity zones with accelerating heartbeat
    // Zone 1 (0-10 ticks): 60 BPM (1.0s period)
    // Zone 2 (10-20 ticks): 100 BPM (0.6s period)
    // Zone 3 (20-30 ticks): 200 BPM (0.3s period)

    float phase;
    float period;

    if (tick < 10) {
        // Slow heartbeat: 20 ticks per beat
        period = 20.0f;
        phase = (tick % 20) / period;
    } else if (tick < 20) {
        // Accelerating: 12 ticks per beat
        period = 12.0f;
        phase = (tick % 12) / period;
    } else {
        // Rapid heartbeat: 6 ticks per beat
        period = 6.0f;
        phase = (tick % 6) / period;
    }

    // Use sine wave for smooth pulse (biological feel)
    // Scale to 0.3-1.0 range (never fully dark, always somewhat glowing)
    float sine = (float) Math.sin(phase * Math.PI * 2.0);
    return 0.3f + (sine * 0.5f + 0.5f) * 0.7f; // Maps [-1,1] to [0.3, 1.0]
}
```

### Client Handler Extension for Texture Sync

```java
// Source: DeathCinematicClientHandler.java (add new methods)
/**
 * Get current cinematic timer for texture animation synchronization.
 * Returns -1 if cinematic not active.
 */
public static int getCinematicTimer() {
    return cinematicActive ? cinematicTimer : -1;
}

/**
 * Get current cinematic phase.
 * Returns null if cinematic not active.
 */
public static CinematicPhase getCurrentPhase() {
    return cinematicActive ? currentPhase : null;
}

/**
 * Check if entity is visible to camera during current phase.
 * Used to optimize rendering or animation updates.
 */
public static boolean isDreadVisible() {
    // During pull-back, both player and Dread visible
    // During close-up, only Dread's face visible
    return cinematicActive;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `.mcmeta` animation for all cases | Runtime texture swapping for state-based changes | GeckoLib 4 (2023) | Emissive incompatibility requires alternative |
| Manual emissive rendering | `AutoGlowingGeoLayer` | GeckoLib 3.1+ | Simplified fullbright rendering |
| Direct renderer manipulation | GeoRenderState extraction pattern | GeckoLib 5 (2024) | Immutable data, better performance |
| Entity field storage | `extractRenderState()` with DataTickets | GeckoLib 5 (2024) | Render thread separation |

**Deprecated/outdated:**
- **GeckoLib 3 dynamic texture patterns:** GeckoLib 4+ uses different renderer architecture
- **Forge-specific texture manipulation:** Project is Fabric, not NeoForge
- **Manual texture atlas registration:** GeckoLib handles this automatically for GeoModel textures

**Note on version mismatch:** Research objective stated "NeoForge 1.21.4 with GeckoLib 5" but project uses Fabric 1.21.1 with GeckoLib 4.7.1. Research focused on GeckoLib 4 (current project version). If upgrading to GeckoLib 5, the `extractRenderState()` pattern would apply.

## Open Questions

Things that couldn't be fully resolved:

1. **Exact pulse BPM values for "organic oppressive" feel**
   - What we know: User wants heartbeat rhythm accelerating from slow to fast (1.0s → 0.3s period during 1.5s pull-back)
   - What's unclear: Exact BPM values for each zone (60 → 100 → 200 BPM is estimate)
   - Recommendation: Implement with config constants, iterate during testing based on feel

2. **Texture frame count vs file count tradeoff**
   - What we know: Need 3 pulse intensity levels (dim, medium, bright) × 3 form variants = 9 textures minimum
   - What's unclear: Whether to use single atlas with .mcmeta (incompatible with emissive) or separate files (more I/O)
   - Recommendation: Use separate files per state to maintain emissive compatibility, texture swapping is cheap compared to atlas updates

3. **"Writhing tentacles" implementation approach**
   - What we know: User wants subtle, constant undulation visible during idle and intensified during kill
   - What's unclear: Whether this is texture-based (UV animation) or animation-based (bone transforms in Blockbench)
   - Recommendation: Use GeckoLib bone animation for tentacles (already supported), texture animation for runes/eyes only

4. **GeckoLib 4 vs 5 feature parity**
   - What we know: Project uses GeckoLib 4.7.1, research found GeckoLib 5 has improved RenderState system
   - What's unclear: Whether GeckoLib 4.7.1 has backported RenderState features or if upgrade required
   - Recommendation: Stay on GeckoLib 4.7.1 (stable, proven), only upgrade if blocking issue found

## Sources

### Primary (HIGH confidence)
- [GeckoLib Animated Textures (Geckolib4) Wiki](https://github.com/bernie-g/geckolib/wiki/Animated-Textures-(Geckolib4)) - Official documentation
- [GeckoLib Emissive Textures (Geckolib4) Wiki](https://github.com/bernie-g/geckolib/wiki/Emissive-Textures-(Geckolib4)) - Official documentation
- [GeckoLib Render Layers (Geckolib4) Wiki](https://github.com/bernie-g/geckolib/wiki/Render-Layers-(Geckolib4)) - Official documentation
- [GeckoLib Triggerable Animations (Geckolib4) Wiki](https://github.com/bernie-g/geckolib/wiki/Triggerable-Animations-(Geckolib4)) - Official documentation
- DreadEntityRenderer.java - Project codebase (existing emissive layer implementation)
- DreadEntityModel.java - Project codebase (existing texture variant selection)
- DeathCinematicClientHandler.java - Project codebase (timing system: 0-30 ticks pull-back, 30-90 ticks close-up)

### Secondary (MEDIUM confidence)
- [Minecraft .mcmeta Animation Format](https://minecraft.wiki/w/Resource_pack#Animation) - Vanilla texture animation
- [AMD GPU Minecraft texture animation performance (FoamFix Issue #182)](https://github.com/asiekierka/FoamFix/issues/182) - Known AMD issue
- [VanillaFix texture animation optimization (FTB Issue #5550)](https://github.com/FTBTeam/FTB-Modpack-Issues/issues/5550) - Performance solution
- [Entity Texture Features mod](https://modrinth.com/mod/entitytexturefeatures) - Alternative approach reference
- [Fabric Entity Rendering Wiki](https://wiki.fabricmc.net/tutorial:entity) - Fabric-specific patterns

### Tertiary (LOW confidence)
- Various forum posts about custom entity renderers - Patterns and pitfalls
- OptiFine emissive texture documentation - Reference only (not used in project)
- GeckoLib 5 RenderState documentation - Future upgrade path

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - GeckoLib 4.7.1 documented and in use, Fabric 1.21.1 verified in gradle.properties
- Architecture: MEDIUM - Runtime texture swapping pattern is established, but specific heartbeat pulse timing requires tuning
- Pitfalls: HIGH - AMD GPU issue and GeckoLib emissive incompatibility verified from official sources

**Research date:** 2026-01-27
**Valid until:** 2026-04-27 (90 days - GeckoLib stable, Minecraft 1.21.1 LTS)

**Note on version discrepancy:** Research objective stated "NeoForge 1.21.4 with GeckoLib 5" but project inspection revealed Fabric 1.21.1 with GeckoLib 4.7.1. Research aligned to actual project stack. If user plans to migrate to NeoForge 1.21.4 + GeckoLib 5, additional research on GeckoLib 5's extractRenderState pattern recommended.
