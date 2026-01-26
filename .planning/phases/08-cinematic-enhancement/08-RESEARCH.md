# Phase 8: Cinematic Enhancement - Research

**Researched:** 2026-01-25
**Domain:** GeckoLib animation extension, camera shake effects, audio synchronization, motion sickness prevention
**Confidence:** HIGH

## Summary

Phase 8 extends the existing 0.8s death cinematic (attack animation) to a 1.5-2s intense grab sequence with camera shake, synchronized audio, and accessibility-focused motion sickness prevention. The research reveals that:

1. **GeckoLib 4.7.1** (already in use) supports extending animation duration through JSON modification and sound keyframes for audio synchronization
2. **Camera shake** requires custom implementation via client tick handler with pitch/yaw modification - existing libraries exist but add unnecessary dependencies
3. **Motion sickness prevention** follows industry standard: exponential decay curves, intensity sliders (0-100%), FPS-based auto-reduction, and visual compensation (vignette + flash) when shake is reduced
4. **Audio ducking** implemented via existing DreadSoundManager priority system (jumpscare priority already blocks other sounds)

**Primary recommendation:** Extend existing GeckoLib "attack" animation to 1.5-2s, implement lightweight camera shake with exponential decay formula `position += (target - position) * (1 - exp(-speed * dt))`, add config slider for intensity, and use GeckoLib sound keyframes for audio sync.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| GeckoLib | 4.7.1 | Animation engine with sound keyframes | Already in project, supports event-driven audio sync |
| Satin API | 2.0.0 | Post-processing shaders (vignette/flash) | Already in project for downed state effects |
| Fabric API | Latest | Client tick events, config system | Core Fabric modding framework |
| GSON | Bundled | Config serialization for intensity slider | Already used in DreadConfig |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| MinecraftClient | Yarn mappings | FPS detection via getCurrentFps() | Auto-reduce shake intensity at <45 FPS |
| WorldRenderEvents | Fabric API | Render hook for visual compensation overlays | When shake reduced, boost vignette/flash |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom camera shake | fabric-camera-shake library | Library adds dependency, only provides BoomEvent API - custom is lighter and more controlled |
| Sound keyframes | Manual timing calculations | Keyframes guarantee sync, manual requires tick counting and drift handling |
| Exponential decay | Linear decay | Exponential feels more natural, industry standard for camera effects |

**Installation:**
No new dependencies required - all systems already present in v1.0 codebase.

## Architecture Patterns

### Recommended Project Structure
```
src/client/java/com/dread/client/
├── DeathCinematicClientHandler.java    # Existing - add camera shake state
├── CameraShakeHandler.java             # NEW - exponential decay shake logic
├── CinematicCompensationRenderer.java  # NEW - vignette/flash when shake reduced
└── DownedStateClientHandler.java       # Existing - reference for shader patterns

src/main/java/com/dread/
├── config/DreadConfig.java              # Add cameraShakeIntensity (0-100)
├── death/DeathCinematicController.java  # Existing - no changes needed
└── sound/DreadSoundManager.java         # Existing - jumpscare priority already exists

src/main/resources/assets/dread/
└── animations/dread_entity.animation.json  # Extend "attack" to 1.5-2s, add sound keyframes
```

### Pattern 1: Camera Shake with Exponential Decay
**What:** Frame-rate independent camera shake using exponential smoothing formula
**When to use:** Client-side visual effects that need smooth, natural decay regardless of FPS
**Example:**
```java
// Source: https://lisyarus.github.io/blog/posts/exponential-smoothing.html
public class CameraShakeHandler {
    private float shakeMagnitudeYaw = 0.0f;
    private float shakeMagnitudePitch = 0.0f;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private float decaySpeed = 10.0f; // 1/speed = time to move 1/e toward target

    public void triggerShake(float maxMagnitude, float duration) {
        // Sharp violent jolts - random in all directions
        targetYaw = (random.nextFloat() - 0.5f) * 2 * maxMagnitude;
        targetPitch = (random.nextFloat() - 0.5f) * 2 * maxMagnitude;
    }

    public void tick(float deltaTime) {
        // Exponential decay formula - frame-rate independent
        float decayFactor = 1.0f - (float)Math.exp(-decaySpeed * deltaTime);
        shakeMagnitudeYaw += (targetYaw - shakeMagnitudeYaw) * decayFactor;
        shakeMagnitudePitch += (targetPitch - shakeMagnitudePitch) * decayFactor;

        // Decay target toward zero (returns to normal)
        targetYaw *= (float)Math.exp(-decaySpeed * deltaTime);
        targetPitch *= (float)Math.exp(-decaySpeed * deltaTime);
    }

    public float getShakeYaw() { return shakeMagnitudeYaw; }
    public float getShakePitch() { return shakeMagnitudePitch; }
}
```

### Pattern 2: GeckoLib Sound Keyframes for Audio Sync
**What:** Trigger sounds at precise animation timestamps using GeckoLib 4 keyframe events
**When to use:** Any time audio must sync exactly with animation frames (grab impact, kill moment)
**Example:**
```java
// In DreadEntity.registerControllers()
controllers.add(new AnimationController<>(this, "grab", 0, state -> {
    // ... existing logic
}).setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>()));

// In dread_entity.animation.json "attack_extended" animation:
// Add sound_effects keyframes at timestamps:
// 0.0s: "dread:grab_impact|1.0|1.0"  (grab starts - loud sting)
// 0.2s: "dread:entity_growl|0.8|0.9"  (sustained horror)
// 1.8s: "dread:kill_impact|1.0|1.0"   (final death blow)
```

### Pattern 3: Motion Sickness Prevention with Compensation
**What:** Reduce shake intensity via config slider, boost visual effects (vignette/flash) to maintain horror impact
**When to use:** Accessibility features that can't compromise core experience
**Example:**
```java
// Config-based intensity scaling
float configIntensity = config.cameraShakeIntensity / 100.0f; // 0.0 to 1.0
float reducedShake = baseShake * configIntensity;

// Compensation: when shake reduced, boost vignette
if (configIntensity < 1.0f) {
    float compensation = 1.0f - configIntensity;
    renderRedVignette(baseVignetteOpacity + (compensation * 0.3f));
    renderWhiteFlash(compensation * 0.4f); // Brief flash
}
```

### Pattern 4: FPS-Based Auto-Reduction
**What:** Detect low FPS (<45) and automatically reduce shake to prevent judder
**When to use:** Runtime performance adaptation for smooth experience across hardware
**Example:**
```java
public void tick(MinecraftClient client, float deltaTime) {
    int currentFps = client.getCurrentFps();

    // Auto-reduce at low FPS to prevent judder
    float fpsMultiplier = 1.0f;
    if (currentFps < 45) {
        fpsMultiplier = Math.max(0.3f, currentFps / 45.0f);
        // Enable compensation rendering
        shouldCompensate = true;
    }

    float finalIntensity = configIntensity * fpsMultiplier;
    applyCameraShake(baseShake * finalIntensity, deltaTime);
}
```

### Anti-Patterns to Avoid
- **Linear decay instead of exponential**: Feels unnatural, stutters at variable frame rates
- **Tick-based timing instead of deltaTime**: Breaks at different FPS, causes drift
- **Manual sound timing instead of keyframes**: Animation/audio desync inevitable with manual calculations
- **Mixin to GameRenderer.renderWorld()**: Too invasive, breaks with other camera mods - use pitch/yaw modification in tick handler instead
- **Hardcoded shake values**: Makes accessibility tuning impossible - always use config multiplier

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Frame-rate independent decay | Custom delta-time smoothing | Exponential decay formula: `1 - exp(-speed * dt)` | Handles extreme delta spikes (lag), naturally clamps to [0,1], mathematically proven |
| Audio-animation sync | Manual tick counters + timers | GeckoLib SoundKeyframeHandler + AutoPlayingSoundKeyframeHandler | Drift-free, animation scrubbing support, BlockBench integration |
| FPS detection | Custom frame counter | MinecraftClient.getCurrentFps() | Already averaged, performance optimized, yarn-mapped |
| Camera shake library | Import fabric-camera-shake | Lightweight custom handler (50 lines) | Library only provides BoomEvent - overkill for single cinematic, adds dependency |
| Visual compensation overlays | Custom GL calls | Existing CrawlVignetteRenderer pattern + DrawContext | Shader compatibility handled, blending correct, HUD integration |

**Key insight:** GeckoLib already solves animation/audio sync, Minecraft already provides FPS detection, and existing Satin/vignette patterns handle compensation - only camera shake needs custom implementation.

## Common Pitfalls

### Pitfall 1: Animation Extension Breaking Existing Attack Behavior
**What goes wrong:** Extending "attack" animation from 0.8s to 1.5-2s makes normal attacks feel sluggish
**Why it happens:** Attack animation used for both normal melee attacks AND death cinematic
**How to avoid:** Create new "death_grab" animation (1.5-2s) separate from "attack" (keep 0.8s), trigger death_grab only during cinematic
**Warning signs:** Players report Dread attacks feel slow, entity frozen after hitting player

### Pitfall 2: Camera Shake Continues After Cinematic Ends
**What goes wrong:** Shake persists into downed state or spectator mode, nauseating
**Why it happens:** Shake state not reset when cinematic ends, exponential decay never fully reaches zero
**How to avoid:** Hard reset shake state to zero when DeathCinematicClientHandler.endCinematic() called
**Warning signs:** Players report dizziness continuing after death, shake visible in spectator mode

### Pitfall 3: Audio Desync on Server Lag
**What goes wrong:** Server lag delays cinematic packet, animation starts before entity teleports, audio plays in wrong location
**Why it happens:** Sound keyframes trigger on animation start, but server might delay entity teleport/packet
**How to avoid:** Play initial grab sound from SERVER in DeathCinematicController (like DREAD_DEATH), use keyframes only for sustained sounds
**Warning signs:** Players hear grab sound from entity's old position, audio spatial location wrong

### Pitfall 4: Motion Sickness from Sudden Shake Start
**What goes wrong:** Instant shake application at cinematic start causes nausea even with low intensity
**Why it happens:** No easing into shake - goes 0 to max in one frame (jump scare effect too abrupt)
**How to avoid:** Apply brief ease-in (0.1-0.2s) using same exponential formula ramping up from zero
**Warning signs:** Players with shake at 50% still report nausea, complaints about "jarring" start

### Pitfall 5: FPS Detection Triggering on Brief Drops
**What goes wrong:** Single frame drop to 40 FPS triggers compensation effects, flashing vignette/intensity changes
**Why it happens:** Checking currentFps() without averaging - world gen, particle bursts cause momentary drops
**How to avoid:** Use rolling average of last 10 frames OR require 20+ consecutive frames below threshold before reducing
**Warning signs:** Vignette flickers during cinematic, players report "flashing effects", shake intensity unstable

### Pitfall 6: Compensation Effects Stacking with Existing Downed State Vignette
**What goes wrong:** Red vignette from compensation overlaps with blood vignette from downed state, screen goes solid red
**Why it happens:** Both use same rendering technique, render at different times without blend awareness
**How to avoid:** Only render compensation during cinematic (before downed state), OR blend compensation into existing vignette renderer
**Warning signs:** Screen almost completely red after death, vignette too dark, players can't see

### Pitfall 7: Config Slider at 0% Disables Cinematic Entirely
**What goes wrong:** Setting shake to 0% also disables camera lock, entity teleport, full cinematic experience
**Why it happens:** Misunderstanding scope - shake intensity should only affect camera rotation, not cinematic activation
**How to avoid:** Shake config only affects camera shake magnitude, cinematic still plays (camera locks, entity teleports)
**Warning signs:** Players confused why setting to 0% skips entire death sequence, think it's same as skipDeathCinematic

## Code Examples

Verified patterns from official sources and existing codebase:

### Exponential Decay Implementation
```java
// Source: https://lisyarus.github.io/blog/posts/exponential-smoothing.html (exponential smoothing)
// Frame-rate independent camera shake decay
public class CameraShakeHandler {
    private static final float SHAKE_MAGNITUDE = 2.5f; // degrees (subtle but noticeable)
    private static final float DECAY_SPEED = 10.0f;    // medium decay (0.7-1s feel)

    private float currentYaw = 0.0f;
    private float currentPitch = 0.0f;
    private float targetYaw = 0.0f;
    private float targetPitch = 0.0f;
    private boolean isActive = false;
    private float elapsed = 0.0f;

    public void startShake(Random random, float intensity) {
        // Pure chaos direction - random in all directions
        targetYaw = (random.nextFloat() - 0.5f) * 2.0f * SHAKE_MAGNITUDE * intensity;
        targetPitch = (random.nextFloat() - 0.5f) * 2.0f * SHAKE_MAGNITUDE * intensity;
        currentYaw = 0.0f;
        currentPitch = 0.0f;
        isActive = true;
        elapsed = 0.0f;
    }

    public void tick(float deltaTime) {
        if (!isActive) return;

        elapsed += deltaTime;

        // Exponential decay - frame-rate independent
        float decay = 1.0f - (float)Math.exp(-DECAY_SPEED * deltaTime);
        currentYaw += (targetYaw - currentYaw) * decay;
        currentPitch += (targetPitch - currentPitch) * decay;

        // Decay target toward zero
        targetYaw *= (float)Math.exp(-DECAY_SPEED * deltaTime);
        targetPitch *= (float)Math.exp(-DECAY_SPEED * deltaTime);

        // Stop when negligible (prevent floating point drift)
        if (Math.abs(currentYaw) < 0.01f && Math.abs(currentPitch) < 0.01f) {
            reset();
        }
    }

    public void reset() {
        currentYaw = 0.0f;
        currentPitch = 0.0f;
        targetYaw = 0.0f;
        targetPitch = 0.0f;
        isActive = false;
    }

    public float getYawOffset() { return currentYaw; }
    public float getPitchOffset() { return currentPitch; }
}
```

### Camera Rotation Application
```java
// In DeathCinematicClientHandler tick() - apply shake to camera entity
private static void tick() {
    cinematicTimer++;

    MinecraftClient client = MinecraftClient.getInstance();
    if (client.getCameraEntity() == null) return;

    // Get delta time for frame-rate independence
    float deltaTime = client.getRenderTickCounter().getTickDelta(true) / 20.0f;

    // Update shake state
    cameraShake.tick(deltaTime);

    // Apply shake offset to camera rotation
    Entity camera = client.getCameraEntity();
    float baseYaw = camera.getYaw();
    float basePitch = camera.getPitch();

    camera.setYaw(baseYaw + cameraShake.getYawOffset());
    camera.setPitch(basePitch + cameraShake.getPitchOffset());

    if (cinematicTimer >= CINEMATIC_DURATION_TICKS) {
        cameraShake.reset(); // CRITICAL: reset before ending
        endCinematic();
    }
}
```

### GeckoLib Sound Keyframes (Animation JSON)
```json
// In dread_entity.animation.json - new "death_grab" animation
{
  "death_grab": {
    "loop": false,
    "animation_length": 1.8,
    "bones": {
      "body": {
        "position": {
          "0.0": [0, 0, 0],
          "0.15": [0, 0, 12],
          "1.0": [0, 0, 12],
          "1.8": [0, 0, 0]
        }
      },
      "left_arm": {
        "rotation": {
          "0.0": [0, 0, 0],
          "0.15": [-100, 0, -60],
          "1.0": [-100, 0, -60],
          "1.8": [0, 0, 0]
        }
      },
      "right_arm": {
        "rotation": {
          "0.0": [0, 0, 0],
          "0.15": [-100, 0, 60],
          "1.0": [-100, 0, 60],
          "1.8": [0, 0, 0]
        }
      }
    },
    "sound_effects": {
      "0.0": {
        "effect": "dread:grab_impact"
      },
      "0.2": {
        "effect": "dread:entity_growl"
      },
      "1.6": {
        "effect": "dread:kill_impact"
      }
    }
  }
}
```

### Config Integration
```java
// In DreadConfig.java - add shake intensity slider
public class DreadConfig {
    // ... existing fields

    // Camera shake intensity (0 = disabled, 100 = full horror)
    public int cameraShakeIntensity = 100;

    @SerializedName("_comment_shake")
    public final String commentShake = "cameraShakeIntensity: Camera shake strength during death (0-100). Lower values boost red vignette/flash compensation.";
}
```

### Compensation Rendering
```java
// Pattern from existing CrawlVignetteRenderer.java - adapt for compensation
public class CinematicCompensationRenderer {
    private static final Identifier VIGNETTE_TEXTURE =
        Identifier.ofVanilla("textures/misc/vignette.png");

    public static void renderCompensation(DrawContext context, float compensationAmount) {
        if (compensationAmount <= 0.0f) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Red vignette boost (like existing CrawlVignetteRenderer)
        float vignetteOpacity = 0.4f + (compensationAmount * 0.3f);
        RenderSystem.setShaderColor(1.0f, 0.15f, 0.15f, vignetteOpacity);
        context.drawTexture(VIGNETTE_TEXTURE, 0, 0, 0, 0, width, height, width, height);

        // White flash overlay (brief, intense)
        float flashOpacity = compensationAmount * 0.25f;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, flashOpacity);
        context.fill(0, 0, width, height, 0xFFFFFF);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
```

### FPS-Based Auto-Reduction
```java
// In CameraShakeHandler - adapt intensity based on FPS
public float getAdaptiveIntensity(MinecraftClient client, float configIntensity) {
    int fps = client.getCurrentFps();

    // Auto-reduce below 45 FPS to prevent judder
    float fpsMultiplier = 1.0f;
    if (fps < 45) {
        // Smooth scaling: 30fps = 67%, 20fps = 44%
        fpsMultiplier = Math.max(0.3f, fps / 45.0f);
        LOGGER.debug("Auto-reducing shake: {}fps -> {}% intensity", fps, (int)(fpsMultiplier * 100));
    }

    return configIntensity * fpsMultiplier;
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Linear camera shake decay | Exponential smoothing with `1 - exp(-speed * dt)` | Game dev standard 2023+ | Frame-rate independent, natural feel, no oscillation |
| Manual sound timing via tick counters | GeckoLib 4 SoundKeyframeHandler | GeckoLib 4.0 release (2023) | Drift-free audio sync, BlockBench integration |
| Single shake intensity for all players | Accessibility sliders (0-100%) + FPS adaptation | Industry standard 2024+ | Halo Infinite, Cyberpunk 2077 accessibility features |
| Camera shake via GameRenderer mixin | Client tick yaw/pitch modification | 2025+ compatibility | Works with shader mods, less invasive |
| Disabled features for motion sickness | Compensation effects (vignette/flash boost) | Accessibility research 2024-2025 | Maintains horror impact while reducing nausea |

**Deprecated/outdated:**
- **fabric-camera-shake library (LoganDark)**: Last updated 2021, designed for MC 1.16-1.18, uses older BoomEvent API - modern approach is lightweight custom handler
- **Manual audio ducking**: DreadSoundManager already implements priority system (jumpscare blocks all others) - no additional ducking needed
- **Linear interpolation for decay**: Causes stuttering at variable frame rates - exponential is now standard

## Open Questions

Things that couldn't be fully resolved:

1. **Exact grab animation front-loading timing**
   - What we know: User wants 1.5-2s total, "front-loaded violence" like jump scare pounce
   - What's unclear: Exact timing split between grab rush (0-0.3s?) and hold/kill (0.3-1.8s?)
   - Recommendation: Implement 0.15s explosive grab (like current 0.2s but faster), 0.8s hold at max extension, 0.85s return/kill. Test in BlockBench, adjust feel.

2. **Player control during grab**
   - What we know: User deferred this to Claude's discretion
   - What's unclear: Complete lock (existing camera lock only) vs futile struggling (input allowed but ineffective)?
   - Recommendation: Start with complete lock (matches existing camera lock, simpler). If too passive, add Phase 8.1 for input ghosting (accept input, zero movement response).

3. **Audio ducking implementation location**
   - What we know: User wants "full duck" - all other sounds nearly muted
   - What's unclear: Whether to extend DreadSoundManager or implement in cinematic handler
   - Recommendation: Extend DreadSoundManager.playJumpScare() to also duck ambient/proximity sounds (lower volume to 10%) during jumpscare flag period. Existing priority system already blocks new sounds.

4. **Compensation effect timing**
   - What we know: Boost vignette + flash when shake reduced
   - What's unclear: Flash duration (single frame? 0.2s pulse?), vignette persistence (entire cinematic? fade?)
   - Recommendation: White flash = 0.1s pulse at grab start, red vignette = sustained throughout cinematic (like downed state). Test with players at 0% shake.

5. **MinecraftClient.getCurrentFps() averaging window**
   - What we know: Method exists and returns FPS counter
   - What's unclear: Is it already averaged? What window size?
   - Recommendation: Assume it's averaged (Minecraft debug screen shows stable values). If jittery in testing, implement 10-frame rolling average.

## Sources

### Primary (HIGH confidence)
- [GeckoLib GitHub Wiki - Keyframe Triggers (GeckoLib 4)](https://github.com/bernie-g/geckolib/wiki/Keyframe-Triggers-(Geckolib4)) - Sound keyframes, custom instruction handlers
- [Exponential Smoothing for Animation](https://lisyarus.github.io/blog/posts/exponential-smoothing.html) - Mathematical formula, frame-rate independence
- [fabric-camera-shake library](https://github.com/LoganDark/fabric-camera-shake) - Architecture reference (not using, but validates approach)
- [Minecraft Wiki - Screen Effects](https://minecraft.wiki/w/Screen_effects) - Vignette rendering, vanilla overlay techniques
- Existing codebase: DreadEntity.java (GeckoLib patterns), DownedStateClientHandler.java (Satin shader usage), CrawlVignetteRenderer.java (vignette rendering), DreadSoundManager.java (priority system)

### Secondary (MEDIUM confidence)
- [Game Audio Theory: Ducking](https://www.gamedeveloper.com/audio/game-audio-theory-ducking) - Audio priority and volume attenuation
- [Halo Infinite Accessibility Features](https://learn.microsoft.com/en-us/gaming/accessibility/xbox-accessibility-guidelines/117) - Intensity sliders (0-100%), screen shake, vignette
- [Camera Overhaul Mod](https://modrinth.com/mod/cameraoverhaul) - Camera rotation implementation reference, exponential decay mention
- [Motion Sickness Accessibility in Video Games](https://madelinemiller.dev/blog/motion-sickness-accessibility/) - FPS stability, config options, compensation techniques
- [MinecraftClient Yarn Mappings](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/client/MinecraftClient.html) - getCurrentFps() API documentation

### Tertiary (LOW confidence)
- WebSearch results on Android Motion Cues 2026 - intensity sliders, adjustable levels
- WebSearch results on Cyberpunk 2077 additive camera motions - motion sickness settings
- Community forum posts on camera shake implementation (Forge forums, MCreator) - general techniques

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - GeckoLib 4.7.1 already in project, all systems verified in existing code
- Architecture: HIGH - Exponential decay formula proven, GeckoLib sound keyframes documented, patterns extracted from working v1.0 code
- Pitfalls: HIGH - Derived from motion sickness research, accessibility standards, and existing cinematic/audio system constraints
- Audio synchronization: HIGH - GeckoLib 4 AutoPlayingSoundKeyframeHandler official API
- Motion sickness prevention: MEDIUM - Industry standards verified (Halo, Cyberpunk), exponential decay proven, but FPS threshold (45) not scientifically validated
- Camera shake mechanics: MEDIUM - Exponential formula verified, but Minecraft-specific integration (yaw/pitch modification timing) requires testing

**Research date:** 2026-01-25
**Valid until:** 2026-02-25 (30 days - stable domain, GeckoLib 4.x mature, Minecraft 1.21.x stable)
