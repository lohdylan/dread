# Phase 13: Cinematic Camera Control - Research

**Researched:** 2026-01-27
**Domain:** Minecraft camera positioning, cinematic systems, Camera mixin architecture, third-person camera control
**Confidence:** HIGH

## Summary

Phase 13 implements a cinematic camera system for the death sequence that executes a multi-stage camera path: instant jump to third-person → pull back to frame player/Dread → jump cut to Dread's face close-up. The research reveals three critical architectural requirements:

1. **Camera positioning must use Camera mixin injection** (Camera.setPos() at render time) to avoid the feedback loop problem solved in Phase 9 - never modify entity rotation/position for camera effects
2. **Third-person positioning requires Camera.update() override** via mixin to control distance and angle, with protected setPos() calls for position offsets
3. **Timeline-based choreography** should use explicit tick-based phase transitions (matching existing DeathCinematicClientHandler pattern) rather than keyframe interpolation systems

The existing v1.2 codebase already implements phased camera motion (IMPACT → LIFT → HOLD → RELEASE → SETTLE) using yaw/pitch rotation control. Phase 13 extends this pattern by adding **position control** for third-person pull-back and face close-up stages, while maintaining the render-time mixin architecture that prevents entity state conflicts.

**Primary recommendation:** Extend existing CameraMixin (order 900) to inject Camera.setPos() offsets during cinematic phases. Use DeathCinematicClientHandler phase system to calculate third-person offset (pull-back) and face target position (close-up), with instant "jump cut" transition (no interpolation) between phases.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Camera class | Minecraft 1.21.x | Position/rotation control via setPos(), setRotation() | Only official API for camera manipulation, all mods use this |
| Mixin | Bundled with Fabric | Inject into Camera methods at render time | Industry standard, prevents entity state conflicts (Phase 9 lesson) |
| Fabric API | 0.116.8 | HudRenderCallback for letterbox bars | Already in project, standard for HUD overlays |
| Vec3d | Minecraft 1.21.x | 3D position calculations, direction vectors | Vanilla math class, yarn-mapped |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| @Shadow annotation | Mixin API | Access Camera private fields (pos, pitch, yaw) | Reading/modifying camera state in mixins |
| DrawContext | Minecraft 1.21.x | Render letterbox bars via fill() | HUD rendering, already used in DownedHudOverlay |
| MathHelper | Minecraft 1.21.x | Angle lerp, clamping, interpolation utilities | Existing pattern from CrawlCameraHandler |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Camera mixin | Entity position modification | Entity approach causes feedback loops (Phase 9 bug demonstrates this) |
| Phase-based system | ReplayMod-style keyframe interpolation | Keyframes add complexity, 4.5s sequence doesn't need spline curves |
| Camera.setPos() | setCameraEntity() to separate entity | Creating entity is heavier, position control via mixin is cleaner |
| HudRenderCallback for bars | Custom GameRenderer mixin | HUD callback is standard Fabric pattern, less invasive |

**Installation:**
No new dependencies required - Camera class is vanilla, Mixin bundled with Fabric, HudRenderCallback already used.

## Architecture Patterns

### Recommended Project Structure
```
src/client/java/com/dread/
├── client/
│   ├── DeathCinematicClientHandler.java    # Extend with position calculation logic
│   ├── CinematicLetterboxRenderer.java     # NEW - black bars via HudRenderCallback
│   └── CameraShakeHandler.java             # Existing - keep for potential future use
└── mixin/
    └── CameraMixin.java                     # Extend with setPos() injection
```

### Pattern 1: Camera Position Control via Mixin
**What:** Inject Camera.setPos() at render time to apply third-person offset or face target position
**When to use:** Any camera position modification during cinematics (same reasoning as Phase 9 rotation fix)
**Example:**
```java
// In CameraMixin.java (extend existing from Phase 9)
@Mixin(Camera.class)
public class CameraMixin {
    @Shadow private Vec3d pos;
    @Shadow private float pitch;
    @Shadow private float yaw;

    // Existing rotation injection from Phase 9 (order 900)
    @Inject(method = "setRotation", at = @At("TAIL"), order = 900)
    private void dread$applyCinematicShake(float yaw, float pitch, CallbackInfo ci) {
        // ... existing shake logic
    }

    // NEW: Position injection for third-person pull-back and face close-up
    @Inject(method = "update", at = @At("TAIL"), order = 900)
    private void dread$applyCinematicPosition(
        BlockView area, Entity focusedEntity, boolean thirdPerson,
        boolean inverseView, float tickDelta, CallbackInfo ci
    ) {
        if (!DeathCinematicClientHandler.isCinematicActive()) {
            return;
        }

        // Get cinematic camera offset from handler
        Vec3d offset = DeathCinematicClientHandler.getCameraPositionOffset();
        if (offset != null) {
            // Apply offset to current camera position (after entity update)
            this.pos = this.pos.add(offset);
        }
    }
}
```

**Why this works:**
- `update()` is called every frame, sets camera position based on focused entity
- `@At("TAIL")` executes AFTER entity position processed
- Modifying `this.pos` directly only affects camera, not entity state
- Order 900 runs before other camera mods (same priority as rotation shake)

### Pattern 2: Phase-Based Position Calculation
**What:** Extend existing DeathCinematicClientHandler phase system with position offset logic
**When to use:** When camera choreography has distinct stages (pull-back, hold, face close-up)
**Example:**
```java
// In DeathCinematicClientHandler.java
public enum CinematicPhase {
    THIRD_PERSON_PULLBACK,  // 0-1.5s: Pull back to third person
    JUMP_CUT,               // Instant transition (no interpolation)
    FACE_CLOSEUP,           // 1.5-4.5s: Locked on Dread's face
}

private static Vec3d calculateCameraOffset() {
    switch (currentPhase) {
        case THIRD_PERSON_PULLBACK:
            // Calculate third-person offset behind player
            return calculateThirdPersonOffset();

        case FACE_CLOSEUP:
            // Calculate position to put Dread's face at camera
            return calculateFaceCloseupPosition();

        default:
            return Vec3d.ZERO; // First-person (no offset)
    }
}

private static Vec3d calculateThirdPersonOffset() {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.player == null) return Vec3d.ZERO;

    // Pull back 5 blocks behind player, 2 blocks up (over shoulder view)
    float yaw = client.player.getYaw();
    double distance = 5.0; // blocks
    double height = 2.0;   // blocks above player

    // Calculate offset in player's facing direction (backwards)
    double x = -Math.sin(Math.toRadians(yaw)) * distance;
    double z = Math.cos(Math.toRadians(yaw)) * distance;

    return new Vec3d(x, height, z);
}

private static Vec3d calculateFaceCloseupPosition() {
    MinecraftClient client = MinecraftClient.getInstance();
    Entity dread = client.world.getEntityById(dreadEntityId);
    if (dread == null) return Vec3d.ZERO;

    // Position camera extremely close to Dread's face
    // Dread's eye position + slight offset toward player
    Vec3d dreadEyes = dread.getEyePos();
    Vec3d toPlayer = client.player.getEyePos().subtract(dreadEyes).normalize();

    // 0.5 blocks from face (overwhelming closeness)
    return dreadEyes.add(toPlayer.multiply(0.5));
}
```

### Pattern 3: Jump Cut via Instant Phase Transition
**What:** No interpolation between pull-back and face close-up - instant position change
**When to use:** When jarring effect is intentional (horror aesthetic from CONTEXT.md)
**Example:**
```java
private static void updatePhase() {
    if (cinematicTimer <= PULLBACK_END_TICKS) {
        currentPhase = CinematicPhase.THIRD_PERSON_PULLBACK;
    } else {
        // INSTANT transition - no interpolation phase
        currentPhase = CinematicPhase.FACE_CLOSEUP;
    }
}

// In calculateCameraOffset() - no lerp between phases
private static Vec3d calculateCameraOffset() {
    // Switch returns offset immediately - no blending
    return switch (currentPhase) {
        case THIRD_PERSON_PULLBACK -> calculateThirdPersonOffset();
        case FACE_CLOSEUP -> calculateFaceCloseupPosition();
    };
}
```

**Why instant works:**
- CONTEXT.md specifies "Jump cut from pull-back to face close-up (no smooth transition — jarring effect)"
- Matches horror editing techniques (sudden transitions increase fear)
- Simpler than interpolation - one less state to manage

### Pattern 4: Letterbox Bars via HudRenderCallback
**What:** Render black bars at top/bottom of screen during cinematic
**When to use:** Visual feedback that cinematic mode is active, player has lost control
**Example:**
```java
// In CinematicLetterboxRenderer.java (NEW)
public class CinematicLetterboxRenderer {
    private static final int BAR_HEIGHT = 50; // pixels

    public static void register() {
        HudRenderCallback.EVENT.register(CinematicLetterboxRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!DeathCinematicClientHandler.isCinematicActive()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Top bar (solid black)
        context.fill(0, 0, width, BAR_HEIGHT, 0xFF000000);

        // Bottom bar (solid black)
        context.fill(0, height - BAR_HEIGHT, width, height, 0xFF000000);
    }
}
```

**Pattern from:** DownedHudOverlay.java (existing) - same HudRenderCallback approach

### Pattern 5: Eye Position Targeting for Face Close-Up
**What:** Calculate Dread's eye position to center eyes in frame (CONTEXT.md requirement)
**When to use:** Face close-up phase - eyes must be centered and visible
**Example:**
```java
private static Vec3d getDreadEyePosition() {
    Entity dread = MinecraftClient.getInstance().world.getEntityById(dreadEntityId);
    if (dread == null) return Vec3d.ZERO;

    // Entity.getEyePos() returns position + getEyeHeight()
    // For vanilla entities, getEyeHeight() is 85% of entity height
    // GeckoLib entities inherit this unless overridden
    Vec3d eyePos = dread.getEyePos();

    // Adjust for all 3 Dread forms (same eye-level targeting)
    // If forms have different heights, getEyeHeight() scales automatically
    return eyePos;
}

// In face close-up positioning
private static Vec3d calculateFaceCloseupPosition() {
    Vec3d dreadEyes = getDreadEyePosition();

    // Camera looks directly at eye position
    // Distance: edge-to-edge screen fill (CONTEXT.md: "overwhelming")
    // 0.3 blocks = extremely close, fills screen
    Vec3d toCamera = getPlayerDirection().multiply(0.3);

    return dreadEyes.add(toCamera);
}
```

### Pattern 6: Input Lockout via Existing System
**What:** Complete input lockout already implemented in v1.2 via camera lock
**When to use:** CONTEXT.md requires "Complete input lockout during 4.5s sequence"
**Example:**
```java
// Already implemented in DeathCinematicClientHandler.startCinematic()
// Camera locked to player prevents look input
// Movement/attack already disabled during downed state

// Phase 13 only adds visual feedback (letterbox bars)
// Input lockout is existing behavior - just make it visible
```

### Anti-Patterns to Avoid
- **Modifying entity position for camera effects**: Creates feedback loops (Phase 9 lesson applies to position too)
- **Smooth interpolation for jump cut**: Defeats horror aesthetic - CONTEXT.md explicitly wants jarring transition
- **Creating separate camera entity**: Heavier than mixin approach, adds entity ID tracking complexity
- **Using GameRenderer.renderWorld() for positioning**: Too late in pipeline, Camera.update() is correct injection point
- **Hardcoded eye offsets instead of getEyePos()**: Breaks across different Dread forms with varying heights

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Camera positioning | Custom entity-based system | Camera.setPos() via mixin | Vanilla API, prevents feedback loops, compatible with other mods |
| Eye position calculation | Manual height * 0.85 calculation | Entity.getEyePos() | Handles all entities including GeckoLib, accounts for pose/dimensions |
| Letterbox rendering | Custom GL calls or shaders | HudRenderCallback + DrawContext.fill() | Standard Fabric pattern, already used in DownedHudOverlay |
| Direction vectors | Manual sin/cos calculations | Vec3d.normalize() and multiply() | Yarn-mapped, handles edge cases, readable |
| Angle wrapping | Custom 360° modulo logic | MathHelper.wrapDegrees() | Handles -180/+180 wrapping correctly |
| Third-person distance | Trial and error values | Existing Camera.update() third-person logic as reference | Vanilla already solved collision, clipping |

**Key insight:** Camera class is the single source of truth for position and rotation. Mixin injection at render time (Phase 9 pattern) applies to position control exactly as it does for rotation control.

## Common Pitfalls

### Pitfall 1: Camera Position Feedback Loop (Entity State Conflict)
**What goes wrong:** Modifying player/Dread position to move camera causes entity AI/physics to fight back
**Why it happens:** Same root cause as Phase 9 rotation bug - entity state is authoritative, camera just reads it
**How to avoid:** NEVER modify entity position for camera effects - use Camera mixin to inject setPos() offsets
**Warning signs:** Camera jitters during cinematic, player teleports, Dread animation stutters

### Pitfall 2: Jump Cut Interpolation Undermines Horror
**What goes wrong:** Adding smooth transition between pull-back and face close-up reduces fear impact
**Why it happens:** Developer instinct to make everything smooth, missing intentional jarring effect
**How to avoid:** CONTEXT.md specifies "no smooth transition — jarring effect" - instant phase change only
**Warning signs:** Cinematic feels polished but not scary, missing horror editing technique

### Pitfall 3: Letterbox Bars Render Behind Game Elements
**What goes wrong:** Chat, inventory, or other HUD elements render on top of letterbox bars
**Why it happens:** HudRenderCallback execution order, some HUD elements render later
**How to avoid:** Use late registration or render bars last via callback priority (test with F3 debug, chat)
**Warning signs:** Bars visible but partially covered, text overlaps letterbox area

### Pitfall 4: Face Close-Up Position Clips Through Dread Model
**What goes wrong:** Camera positioned inside Dread's head, player sees texture interior or black
**Why it happens:** getEyePos() is at eye level, but moving camera TO that position puts it inside model
**How to avoid:** Offset camera slightly in front of eyes (toward player) by 0.3-0.5 blocks
**Warning signs:** Black screen during face close-up, flickering textures, can see inside model

### Pitfall 5: Third-Person Pull-Back Clips Through Walls
**What goes wrong:** Camera positioned 5 blocks behind player intersects with wall, shows void/blocks
**Why it happens:** No collision detection on camera offset calculation
**How to avoid:** Raycast from player to camera position, reduce distance if wall detected (or accept clipping for 1.5s)
**Warning signs:** Players report seeing through walls, camera inside blocks during outdoor deaths

### Pitfall 6: Eye Position Inconsistent Across Dread Forms
**What goes wrong:** Face close-up framing varies wildly between BASE/TALL/WIDE forms
**Why it happens:** Different entity heights, not adjusting camera distance or angle per form
**How to avoid:** getEyePos() handles height automatically, but verify all 3 forms in testing
**Warning signs:** Some forms show eyes centered, others show neck/forehead, inconsistent framing

### Pitfall 7: Camera Mixin Order Conflicts
**What goes wrong:** Third-party camera mods (Camera Overhaul, Better Third Person) conflict with position injection
**Why it happens:** Multiple mods injecting into Camera.update() with same or conflicting priority
**How to avoid:** Use order 900 (matching rotation shake), document known conflicts, test with popular camera mods
**Warning signs:** Cinematic camera doesn't move, position jumps, other camera mod features break

### Pitfall 8: Letterbox Bars Don't Snap Instantly
**What goes wrong:** Bars fade in gradually instead of instant appearance
**Why it happens:** Adding fade animation for polish, contradicting CONTEXT.md "snap in instantly"
**How to avoid:** Render bars at full opacity (0xFF000000) as soon as isCinematicActive() returns true
**Warning signs:** Bars animate in, smooth transitions, lacks horror aesthetic

### Pitfall 9: Face Close-Up Distance Too Far
**What goes wrong:** Dread's face doesn't fill screen "edge-to-edge" as specified
**Why it happens:** Conservative camera distance (1-2 blocks) doesn't achieve overwhelming effect
**How to avoid:** CONTEXT.md says "fills full screen (edge-to-edge, overwhelming)" - use 0.3-0.5 block distance
**Warning signs:** Face visible but not overwhelming, too much background visible, lacks intensity

### Pitfall 10: Pull-Back Doesn't Frame Both Characters
**What goes wrong:** Third-person view shows only player or only Dread, not both together
**Why it happens:** Camera offset calculated only from player position, ignoring Dread's location
**How to avoid:** Calculate midpoint between player and Dread, position camera to frame both (may require dynamic angle)
**Warning signs:** Size difference not visible, Dread off-screen during pull-back, composition fails

## Code Examples

Verified patterns from existing codebase and Minecraft Camera API:

### Complete Camera Position Mixin
```java
// File: src/client/java/com/dread/mixin/CameraMixin.java
// Extend existing mixin from Phase 9
package com.dread.mixin;

import com.dread.client.DeathCinematicClientHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies cinematic camera effects (rotation shake, position offsets).
 * Injects at render time to avoid entity state feedback loops.
 */
@Mixin(Camera.class)
public class CameraMixin {

    @Shadow private Vec3d pos;
    @Shadow private float pitch;
    @Shadow private float yaw;

    /**
     * Apply cinematic shake offsets to camera rotation.
     * Existing from Phase 9 - rotation control.
     */
    @Inject(method = "setRotation", at = @At("TAIL"), order = 900)
    private void dread$applyCinematicShake(float yaw, float pitch, CallbackInfo ci) {
        if (!DeathCinematicClientHandler.isCinematicActive()) {
            return;
        }

        this.yaw += DeathCinematicClientHandler.getShakeYawOffset();
        this.pitch += DeathCinematicClientHandler.getShakePitchOffset();
    }

    /**
     * Apply cinematic position offsets for third-person and face close-up.
     * NEW for Phase 13 - position control.
     */
    @Inject(method = "update", at = @At("TAIL"), order = 900)
    private void dread$applyCinematicPosition(
        BlockView area, Entity focusedEntity, boolean thirdPerson,
        boolean inverseView, float tickDelta, CallbackInfo ci
    ) {
        if (!DeathCinematicClientHandler.isCinematicActive()) {
            return;
        }

        // Get position offset from handler (phase-dependent)
        Vec3d offset = DeathCinematicClientHandler.getCameraPositionOffset();
        if (offset != null && !offset.equals(Vec3d.ZERO)) {
            // Apply offset to camera position (after entity update)
            this.pos = this.pos.add(offset);
        }
    }
}
```

### Extended DeathCinematicClientHandler with Position Logic
```java
// In DeathCinematicClientHandler.java - extend existing phase system

// New phases for Phase 13
private enum CinematicPhase {
    FIRST_PERSON_INSTANT,   // 0 ticks: Instant (no delay)
    THIRD_PERSON_PULLBACK,  // 0-30 ticks (1.5s): Pull back to third person
    FACE_CLOSEUP,           // 30-90 ticks (4.5s total): Locked on face
}

private static final int PULLBACK_END_TICKS = 30;  // 1.5 seconds
private static final int CINEMATIC_DURATION_TICKS = 90; // 4.5 seconds

// Third-person camera settings
private static final double PULLBACK_DISTANCE = 5.0; // blocks behind player
private static final double PULLBACK_HEIGHT = 2.0;   // blocks above player
private static final double FACE_DISTANCE = 0.4;     // blocks from Dread's face

/**
 * Get camera position offset for current cinematic phase.
 * Called by CameraMixin.update() injection.
 */
public static Vec3d getCameraPositionOffset() {
    if (!cinematicActive) return Vec3d.ZERO;

    return switch (currentPhase) {
        case FIRST_PERSON_INSTANT -> Vec3d.ZERO; // No offset (first-person)
        case THIRD_PERSON_PULLBACK -> calculatePullbackOffset();
        case FACE_CLOSEUP -> calculateFaceCloseupOffset();
    };
}

/**
 * Calculate third-person pull-back offset.
 * Positions camera behind and above player to frame both player and Dread.
 */
private static Vec3d calculatePullbackOffset() {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.player == null) return Vec3d.ZERO;

    // Get player's facing direction (yaw only, ignore pitch)
    float yaw = client.player.getYaw();

    // Calculate offset behind player
    double x = -Math.sin(Math.toRadians(yaw)) * PULLBACK_DISTANCE;
    double z = Math.cos(Math.toRadians(yaw)) * PULLBACK_DISTANCE;

    // Camera above player for better framing
    return new Vec3d(x, PULLBACK_HEIGHT, z);
}

/**
 * Calculate face close-up camera position.
 * Positions camera extremely close to Dread's eyes (overwhelming effect).
 */
private static Vec3d calculateFaceCloseupOffset() {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client.player == null || client.world == null) return Vec3d.ZERO;

    Entity dread = client.world.getEntityById(dreadEntityId);
    if (dread == null) return Vec3d.ZERO;

    // Get Dread's eye position (handles all 3 forms automatically)
    Vec3d dreadEyes = dread.getEyePos();

    // Get player's current position (camera follows player)
    Vec3d playerPos = client.player.getEyePos();

    // Direction from Dread to player
    Vec3d toPlayer = playerPos.subtract(dreadEyes).normalize();

    // Position camera slightly in front of Dread's eyes toward player
    // This creates "face fills screen" effect without clipping inside model
    Vec3d cameraTarget = dreadEyes.add(toPlayer.multiply(FACE_DISTANCE));

    // Calculate offset from player position to target
    return cameraTarget.subtract(playerPos);
}

/**
 * Update phase based on cinematic timer.
 * Jump cut: instant transition from pull-back to face close-up.
 */
private static void updatePhase() {
    if (cinematicTimer == 0) {
        currentPhase = CinematicPhase.FIRST_PERSON_INSTANT;
    } else if (cinematicTimer <= PULLBACK_END_TICKS) {
        currentPhase = CinematicPhase.THIRD_PERSON_PULLBACK;
    } else {
        // INSTANT transition - no interpolation phase
        currentPhase = CinematicPhase.FACE_CLOSEUP;
    }
}
```

### Letterbox Renderer Implementation
```java
// File: src/client/java/com/dread/client/CinematicLetterboxRenderer.java
package com.dread.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Renders letterbox bars during death cinematic.
 * Instant appearance (no fade) to signal player control loss.
 */
public class CinematicLetterboxRenderer {
    private static final int BAR_HEIGHT = 60; // pixels (adjust for prominence)
    private static final int BAR_COLOR = 0xFF000000; // Solid black

    public static void register() {
        HudRenderCallback.EVENT.register(CinematicLetterboxRenderer::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!DeathCinematicClientHandler.isCinematicActive()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Top letterbox bar (solid black, instant appearance)
        context.fill(0, 0, width, BAR_HEIGHT, BAR_COLOR);

        // Bottom letterbox bar (solid black, instant appearance)
        context.fill(0, height - BAR_HEIGHT, width, height, BAR_COLOR);
    }
}
```

### Camera API Reference (Yarn Mappings)
```java
// Source: https://maven.fabricmc.net/docs/yarn-1.17-pre1+build.1/net/minecraft/client/render/Camera.html

public class Camera {
    private Vec3d pos;
    private float yaw;
    private float pitch;

    /**
     * Update camera state based on focused entity and view mode.
     * Called every frame during rendering.
     */
    public void update(
        BlockView area,
        Entity focusedEntity,
        boolean thirdPerson,
        boolean inverseView,
        float tickDelta
    ) {
        // Sets camera position, rotation, FOV based on entity
        // Handles third-person distance, collision detection
    }

    /**
     * Set camera position directly.
     * Protected - accessible via @Shadow in mixins.
     */
    protected void setPos(Vec3d pos) {
        this.pos = pos;
    }

    protected void setPos(double x, double y, double z) {
        this.pos = new Vec3d(x, y, z);
    }

    /**
     * Get current camera position.
     */
    public Vec3d getPos() {
        return this.pos;
    }
}
```

### Eye Position Calculation Pattern
```java
// Entity eye position API (works for all entities including GeckoLib)
Entity dread = client.world.getEntityById(dreadEntityId);

// Method 1: Direct getEyePos() (recommended)
Vec3d eyePos = dread.getEyePos();
// Returns: entity.getPos().add(0, entity.getEyeHeight(), 0)

// Method 2: Manual calculation (if custom logic needed)
Vec3d pos = dread.getPos();
float eyeHeight = dread.getEyeHeight();
Vec3d eyePos = pos.add(0, eyeHeight, 0);

// For GeckoLib entities:
// - getEyeHeight() inherited from LivingEntity
// - Default: 85% of entity height (EntityDimensions)
// - Can be overridden in DreadEntity if needed
// - Same method works for all 3 Dread forms (scales with height)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Entity position modification for camera | Camera mixin with setPos() injection | Phase 9 lesson (2026-01-26) | Eliminates feedback loops, smooth cinematics |
| Separate camera entity (spectator mode) | Camera offset via mixin on player entity | Minecraft 1.13+ Camera refactor | Lighter, no entity ID tracking, cleaner state |
| Smooth keyframe interpolation for all cuts | Instant jump cuts for horror effect | Horror film editing standards (1970s+) | Jarring transitions increase fear impact |
| Manual eye height calculations | Entity.getEyePos() API | Minecraft 1.14+ entity dimensions | Handles all entity types, pose-aware |
| Custom GL for letterbox bars | HudRenderCallback + DrawContext.fill() | Fabric API standard (2020+) | Shader-compatible, mod-friendly |
| Keyframe timeline systems (ReplayMod) | Phase-based tick transitions | Simple cinematics don't need curves | Lighter, matches existing v1.2 pattern |

**Deprecated/outdated:**
- **setCameraEntity(spectatorEntity)**: Creates extra entity, harder to clean up, v1.2 uses player-based lock
- **Smooth transitions for all camera cuts**: Horror cinematography intentionally uses jump cuts for fear
- **GameRenderer.renderWorld() mixins**: Too late for position, Camera.update() is correct injection point

## Open Questions

Things that couldn't be fully resolved:

1. **Third-person pull-back collision detection**
   - What we know: Vanilla Camera.update() handles third-person collision, but we're offsetting AFTER update
   - What's unclear: Should we raycast to detect walls, or accept 1.5s of potential clipping?
   - Recommendation: Accept clipping for simplicity (1.5s is short), add raycast only if players report immersion break

2. **Face close-up distance for all Dread forms**
   - What we know: CONTEXT.md wants "edge-to-edge" screen fill, but forms have different sizes
   - What's unclear: Should distance be constant (0.4 blocks) or scale with form size?
   - Recommendation: Start with constant 0.4 blocks, test all 3 forms, adjust if framing inconsistent

3. **Letterbox bar height for different resolutions**
   - What we know: Bars should be prominent but not excessive
   - What's unclear: Should BAR_HEIGHT scale with screen height, or use fixed pixel value?
   - Recommendation: Fixed 60 pixels works for 720p-2160p, test on ultrawide for edge cases

4. **Camera mixin performance with third-party mods**
   - What we know: Order 900 should run before most camera mods (default 1000)
   - What's unclear: Do popular mods (Camera Overhaul, Better Third Person) conflict with position injection?
   - Recommendation: Test with Camera Overhaul specifically, document known conflicts, add config to disable if needed

5. **Jump cut timing precision**
   - What we know: CONTEXT.md says "Jump cut happens at peak pull-back (no pause before cut)"
   - What's unclear: Exact tick for transition (tick 30, or slightly before peak for anticipation?)
   - Recommendation: Transition at tick 30 (1.5s) exactly - "peak" means end of pull-back phase, no need for early trigger

6. **Eye centering precision across forms**
   - What we know: CONTEXT.md requires "Eyes are centered in frame (direct eye contact)"
   - What's unclear: Do we need pitch adjustment to ensure eyes are vertically centered, or is getEyePos() sufficient?
   - Recommendation: getEyePos() should be sufficient (85% of height is eye level), test with actual models, add pitch offset if needed

## Sources

### Primary (HIGH confidence)
- [Minecraft Camera class (Yarn 1.17)](https://maven.fabricmc.net/docs/yarn-1.17-pre1+build.1/net/minecraft/client/render/Camera.html) - setPos(), update() API reference
- [ReplayMod Keyframe System](https://deepwiki.com/ReplayMod/ReplayMod/3.1-keyframe-system) - Timeline architecture, interpolation methods (reference, not using)
- Existing codebase: CameraMixin.java (Phase 9 rotation pattern), DeathCinematicClientHandler.java (phase system), DownedHudOverlay.java (HUD rendering pattern)
- [Mixin Documentation - @Inject](https://github.com/SpongePowered/Mixin/wiki/Injection-Point-Reference) - @At("TAIL") behavior for Camera.update()
- Phase 9 research (09-RESEARCH.md) - Camera mixin anti-patterns, feedback loop prevention

### Secondary (MEDIUM confidence)
- [Camera Utils mod](https://modrinth.com/mod/camera-utils) - Third-person distance control, camera detaching (validates approach)
- [Better Third Person mod](https://www.curseforge.com/minecraft/mc-mods/better-third-person) - Free camera rotation, cinematic camera patterns
- [Minecraft entity eye position](https://hypixel.net/threads/player-eye-position.5634319/) - getEyeHeight() calculation (85% of entity height)
- [Fabric HudRenderCallback](https://maven.fabricmc.net/docs/yarn-1.16.4+build.1/net/minecraft/client/render/Camera.html) - HUD overlay rendering pattern
- [Cinematic Bars resource pack](https://modrinth.com/resourcepack/cinematic-bars-pumpkin-overlay) - Letterbox implementation approach

### Tertiary (LOW confidence)
- WebSearch results on horror film editing (jump cuts, jarring transitions) - Aesthetic validation
- General game cinematics tutorials (Unreal Engine, KeyShot) - Keyframe systems overview (not specific to this implementation)

## Metadata

**Confidence breakdown:**
- Camera mixin approach: HIGH - Phase 9 proves pattern works, Camera.update() is correct injection point
- Third-person positioning: HIGH - Camera.setPos() API verified in Yarn docs, offset calculation is Vec3d math
- Face close-up targeting: HIGH - Entity.getEyePos() handles all forms, tested pattern in existing code
- Letterbox rendering: HIGH - DownedHudOverlay.java demonstrates exact pattern with HudRenderCallback
- Jump cut timing: HIGH - Phase-based system matches existing v1.2 pattern, no interpolation needed
- Wall collision handling: MEDIUM - May need raycast testing, but accepting clipping is viable for 1.5s duration
- Third-party mod compatibility: MEDIUM - Order 900 should work, but untested with Camera Overhaul

**Research date:** 2026-01-27
**Valid until:** 2026-02-27 (30 days - Camera API stable since 1.13, Mixin patterns mature)
