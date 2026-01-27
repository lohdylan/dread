# Architecture Patterns: Dread v2.0 Cinematic Integration

**Domain:** Minecraft Fabric Mod - Horror Cinematics
**Milestone:** v2.0 Atmosphere & Dread
**Researched:** 2026-01-27
**Confidence:** MEDIUM (verified with official Fabric docs and GeckoLib wiki; camera control based on mod analysis)

## Executive Summary

v2.0 adds cinematic camera control, animated textures, and environmental effects to the existing Dread mod architecture. The key architectural challenge is **render-time orchestration**: synchronizing camera override, texture animation timing, and environmental triggers within the existing 4.5s death sequence.

**Critical insight:** Minecraft 1.21+ separates game thread from render thread, requiring all cinematic state to be pre-computed and passed via render context. Direct entity manipulation during rendering is no longer possible.

**Integration strategy:** All new features hook into existing `DeathCinematicClientHandler` as the single source of truth for cinematic progress (0.0 → 1.0 over 4.5s). Each component queries this progress value rather than maintaining separate timers.

## Existing Architecture Overview

**Current death sequence flow:**
```
1. DreadEntity.attack() → server triggers kill
2. Server sends death cinematic packet → DeathCinematicClientHandler starts
3. CameraMixin (order 900) applies shake during 4.5s sequence
4. Player enters downed state → DownedStateClientHandler activates
```

**Existing components:**
- **CameraMixin**: Render-time shake at mixin order 900
- **DeathCinematicClientHandler**: 4.5s sequence timing coordinator
- **DreadEntity**: GeckoLib entity with 3 form variants
- **DownedStateClientHandler**: Blood vignette, crawl pose
- **BlockEntityMixin**: Torch extinguishing
- **Custom networking**: Client-server synchronization

---

## v2.0 Architecture: New Components

### 1. Cinematic Camera System

**New component: `CinematicCameraController`**

**Purpose:** Override camera position and rotation (not just shake) during death sequence.

**Architecture pattern:** Modifier-based camera override with priority system
- Follows Free Camera API pattern: capture → modify → apply pipeline
- Integrates with existing CameraMixin at render time
- Uses Fabric event system for lifecycle management

**Key methods:**
```java
// ICameraModifier-style interface
void enableCinematic()           // Activates camera override
void setCameraPath(Timeline)     // Defines position/rotation over time
void update(float partialTick)   // Called each frame during cinematic
void disableCinematic()          // Restores player camera

// Integration with existing CameraMixin
int getPriority()                // Higher than shake (order 950)
boolean isActive()               // During DeathCinematicClientHandler window
```

**Data flow:**
```
DeathCinematicClientHandler.start()
  → CinematicCameraController.enableCinematic()
  → Sets camera path timeline (4.5s sequence)

Each render frame:
  GameRenderer.render()
    → CameraMixin.apply() [existing shake, order 900]
    → CinematicCameraController.apply() [new override, order 950]
    → Final camera state written to Camera.setPosition/setRotation()
```

**Integration points:**
- **Mixin target:** `net.minecraft.client.render.Camera` (same as CameraMixin)
- **Injection point:** `@Inject(method = "update", at = @At("TAIL"))`
- **Priority handling:** Apply after existing shake via higher order value
- **State storage:** Store active cinematic in `ClientTickEvents.END_CLIENT_TICK` handler

**Timeline coordination:**
```java
// In DeathCinematicClientHandler
private static final Timeline DEATH_CAMERA_PATH = Timeline.builder()
    .keyframe(0.0f, CameraKeyframe.at(player).lookingAt(dread))
    .keyframe(1.5f, CameraKeyframe.circling(dread, radius=3, height=1.5))
    .keyframe(3.0f, CameraKeyframe.closeup(dread.face))
    .keyframe(4.5f, CameraKeyframe.fadeToBlack())
    .build();
```

**Sources:**
- [Free Camera API architecture](https://deepwiki.com/AnECanSaiTin/Free-camera-API) (MEDIUM confidence - mod analysis)
- [Fabric Mixin injection patterns](https://wiki.fabricmc.net/tutorial:mixin_injects) (HIGH confidence - official docs)

---

### 2. Animated Texture System

**New component: `DreadTextureAnimator`**

**Purpose:** Animate Dread's texture during kill sequence (glitch effect, form transition).

**Architecture pattern:** GeckoLib 5 render-state-driven texture selection
- Cannot swap textures at render time (1.21+ thread separation)
- Must pre-compute texture during `extractRenderState()` phase
- Use `GeoRenderLayer` for overlay effects

**Critical constraint:** GeckoLib 5 architectural change
> "At the time of rendering an object, the object did not exist. This means texture selection can no longer depend on live entity state during rendering."

**Implementation approaches:**

**Option A: Texture atlas with UV animation** (Recommended for glitch effects)
```java
// DreadEntity.java - add render data during state extraction
@Override
public void extractRenderData(DreadRenderState renderState) {
    super.extractRenderData(renderState);

    // Pre-compute UV offset based on cinematic progress
    float progress = DeathCinematicClientHandler.getProgress(); // 0.0 to 1.0
    renderState.putData(UV_OFFSET_TICKET, calculateGlitchOffset(progress));
}

// In custom shader or GeoRenderLayer
// Apply UV offset to create scrolling/glitch effect
```

**Option B: Form variant switching** (For discrete texture changes)
```java
// Existing DreadEntity already has 3 form variants
// Use GeckoLib's getTextureResource with render context

@Override
public ResourceLocation getTextureResource(DreadEntity entity,
                                           @Nullable DreadRenderer renderer) {
    if (DeathCinematicClientHandler.isActive()) {
        float progress = DeathCinematicClientHandler.getProgress();
        if (progress < 0.3f) return DREAD_FORM_1;
        if (progress < 0.7f) return DREAD_FORM_2;
        return DREAD_FORM_3;
    }
    return entity.getCurrentFormTexture();
}
```

**Option C: Custom GeoRenderLayer** (For overlay effects like blood drip)
```java
public class DreadBloodLayer extends GeoRenderLayer<DreadEntity> {
    @Override
    public void render(PoseStack poseStack, DreadEntity entity,
                       BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        float progress = DeathCinematicClientHandler.getProgress();
        if (progress > 0.5f) {
            // Render blood drip texture overlay
            float alpha = (progress - 0.5f) * 2.0f; // Fade in
            // Render semi-transparent blood texture
        }
    }
}
```

**Timing synchronization:**
- **Single source of truth:** `DeathCinematicClientHandler.getProgress()`
- **Access pattern:** All render-time code queries static progress value
- **Update cadence:** Updated in `ClientTickEvents.END_CLIENT_TICK`

**Integration with GeckoLib animations:**
```java
// Coordinate texture changes with bone animations
@Override
public void setCustomAnimations(DreadEntity entity, long instanceId,
                                AnimationState<DreadEntity> state) {
    super.setCustomAnimations(entity, instanceId, state);

    float progress = DeathCinematicClientHandler.getProgress();
    if (progress > 0.0f) {
        // Trigger kill animation in Blockbench
        triggerAnimation("kill_sequence");

        // Sync texture transitions with animation keyframes
        // E.g., switch texture at frame 30 (1.5s into 4.5s sequence)
    }
}
```

**Sources:**
- [GeckoLib 5 render thread separation](https://github.com/bernie-g/geckolib/wiki/Geckolib-5-Changes) (HIGH confidence - official wiki)
- [GeckoLib 4 animated textures](https://github.com/bernie-g/geckolib/wiki/Animated-Textures-(Geckolib4)) (HIGH confidence - official wiki)

---

### 3. Environmental Effects System

**New components: `ProximityEffectManager` + `EnvironmentalEffectTriggers`**

**Purpose:** Trigger door/light effects when Dread approaches, blood trail when crawling.

**Architecture pattern:** Event-driven effect triggers with client-side evaluation

#### 3A. Proximity-Based Effects (Dread approaches)

**Trigger mechanism:** Client-side distance calculation in tick event
```java
public class ProximityEffectManager {
    private static final double DOOR_SLAM_RADIUS = 8.0;
    private static final double LIGHT_FLICKER_RADIUS = 12.0;

    public static void tick(ClientLevel level) {
        // Find nearest Dread entity
        DreadEntity dread = findNearestDread(level);
        if (dread == null) return;

        // Evaluate proximity triggers
        for (BlockPos doorPos : nearbyDoors) {
            double distance = dread.distanceToSqr(doorPos);
            if (distance < DOOR_SLAM_RADIUS * DOOR_SLAM_RADIUS) {
                triggerDoorSlam(doorPos);
            }
        }

        for (BlockPos lightPos : nearbyLights) {
            double distance = dread.distanceToSqr(lightPos);
            if (distance < LIGHT_FLICKER_RADIUS * LIGHT_FLICKER_RADIUS) {
                triggerLightFlicker(lightPos);
            }
        }
    }
}
```

**Registration:**
```java
// In mod initializer
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (client.level != null) {
        ProximityEffectManager.tick(client.level);
    }
});
```

**Effect implementations:**

**Door slam:**
```java
private void triggerDoorSlam(BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    if (state.getBlock() instanceof DoorBlock && state.getValue(DoorBlock.OPEN)) {
        // Play slam sound
        level.playLocalSound(pos, SoundEvents.DOOR_SLAM, ...);

        // Send packet to server to close door
        NetworkHandler.sendDoorClosePacket(pos);

        // Client-side prediction: close immediately
        level.setBlock(pos, state.setValue(DoorBlock.OPEN, false), 3);
    }
}
```

**Light flicker:**
```java
private void triggerLightFlicker(BlockPos pos) {
    // Existing BlockEntityMixin can extinguish torches
    // For other lights, use client-side particle effects

    BlockEntity be = level.getBlockEntity(pos);
    if (be instanceof LightBlockEntity) {
        // Trigger flicker animation
        // Could use random light level reduction + particle sparks
        spawnFlickerParticles(pos);
    }
}
```

#### 3B. Event-Based Effects (Player crawling)

**Trigger mechanism:** Respond to player state change events
```java
public class CrawlBloodTrailEffect {
    private static final int PARTICLE_SPAWN_INTERVAL = 5; // Every 5 ticks
    private static int tickCounter = 0;

    public static void tick(LocalPlayer player) {
        if (!DownedStateClientHandler.isPlayerDowned()) return;

        tickCounter++;
        if (tickCounter >= PARTICLE_SPAWN_INTERVAL) {
            tickCounter = 0;
            spawnBloodTrailParticle(player.position());
        }
    }

    private static void spawnBloodTrailParticle(Vec3 pos) {
        // Use Minecraft's particle system
        Minecraft.getInstance().level.addParticle(
            ParticleTypes.DRIPPING_DRIPSTONE_LAVA, // Or custom particle
            pos.x, pos.y, pos.z,
            0, -0.01, 0 // Velocity (downward drift)
        );
    }
}
```

**Integration with DownedStateClientHandler:**
```java
// Add to existing DownedStateClientHandler
public static void onDownedStateStart() {
    // Existing blood vignette code
    ...

    // NEW: Register blood trail ticker
    ClientTickEvents.END_CLIENT_TICK.register(CrawlBloodTrailEffect::tick);
}

public static void onDownedStateEnd() {
    // Unregister to prevent memory leak
    ClientTickEvents.END_CLIENT_TICK.unregister(CrawlBloodTrailEffect::tick);
}
```

**Particle spawning architecture:**
- **Client-only:** Blood trail particles are purely visual
- **No networking:** Server doesn't need to know about particle positions
- **Performance:** Limit spawn rate (every 5 ticks = 4 particles/sec)

**Sources:**
- [Fabric ClientTickEvents](https://docs.fabricmc.net/develop/events) (HIGH confidence - official docs)
- [Minecraft particle system](https://minecraft.wiki/w/Particles_(Java_Edition)) (HIGH confidence - official wiki)
- [Fabric block entities](https://docs.fabricmc.net/develop/blocks/block-entities) (HIGH confidence - official docs)

---

## Integration Pattern: Complete Death Sequence

**Orchestrator: `DeathCinematicClientHandler` (existing, modified)**

```java
public class DeathCinematicClientHandler {
    private static final float DURATION = 4.5f; // seconds
    private static long startTime = 0;
    private static boolean active = false;

    public static void start() {
        active = true;
        startTime = System.currentTimeMillis();

        // NEW v2.0 integrations
        CinematicCameraController.enableCinematic();
        DreadTextureAnimator.startKillSequence();

        // Existing functionality
        // ... trigger GeckoLib kill animation
        // ... start camera shake (via CameraMixin)
    }

    public static float getProgress() {
        if (!active) return 0.0f;
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1.0f, elapsed / (DURATION * 1000.0f));
    }

    public static void tick() {
        if (!active) return;

        float progress = getProgress();

        // Update all cinematic components
        CinematicCameraController.update(progress);
        DreadTextureAnimator.update(progress);

        // End sequence
        if (progress >= 1.0f) {
            end();
        }
    }

    private static void end() {
        active = false;

        CinematicCameraController.disableCinematic();
        DreadTextureAnimator.endKillSequence();

        // Transition to downed state (existing)
        DownedStateClientHandler.activate();
    }
}
```

**Render pipeline integration:**

```
Frame N:
  ┌─────────────────────────────────────────────────────┐
  │ ClientTickEvents.END_CLIENT_TICK                    │
  │   → DeathCinematicClientHandler.tick()              │
  │   → ProximityEffectManager.tick()                   │
  │   → CrawlBloodTrailEffect.tick()                    │
  │   → Update progress: 0.0 → 1.0 over 4.5s            │
  └─────────────────────────────────────────────────────┘
                         ↓
  ┌─────────────────────────────────────────────────────┐
  │ GameRenderer.render()                               │
  │   → Camera.update()                                 │
  │      → CameraMixin.apply() [shake, order 900]       │
  │      → CinematicCameraController.apply() [override] │
  │                                                      │
  │   → WorldRenderer.render()                          │
  │      → DreadEntity.extractRenderState()             │
  │         → Query DeathCinematicClientHandler.progress│
  │         → Pre-compute texture/UV offset             │
  │      → DreadRenderer.render()                       │
  │         → GeoRenderLayers (blood overlay, etc.)     │
  │                                                      │
  │   → Particle rendering (blood trail from crawl)     │
  └─────────────────────────────────────────────────────┘
```

**Key synchronization mechanism:**
- **Single source of truth:** `DeathCinematicClientHandler.getProgress()`
- **Static access:** All components query progress, don't store local copies
- **Tick-update-render pattern:** State updates in tick events, read in render

---

## Component Boundaries

| Component | Responsibility | Communicates With | Type |
|-----------|---------------|-------------------|------|
| **DeathCinematicClientHandler** | Orchestrates 4.5s sequence, owns progress timer | All cinematic components | Coordinator (existing, modified) |
| **CinematicCameraController** | Overrides camera pos/rot based on timeline | Camera (via mixin), DeathCinematicClientHandler | NEW |
| **DreadTextureAnimator** | Manages texture state during kill sequence | DreadEntity.extractRenderState(), GeoModel | NEW (optional - can use GeoModel directly) |
| **ProximityEffectManager** | Detects Dread proximity, triggers effects | ClientTickEvents, door/light blocks | NEW |
| **CrawlBloodTrailEffect** | Spawns particles during downed crawl | DownedStateClientHandler, particle system | NEW |
| **CameraMixin** | Applies shake effect | Camera (existing, unmodified) | EXISTING |
| **DreadEntity** | GeckoLib entity with animations | GeckoLib renderer, extractRenderState | EXISTING (modified for render state) |
| **DownedStateClientHandler** | Blood vignette, crawl pose | CrawlBloodTrailEffect | EXISTING (modified to register blood trail) |

---

## Data Flow Patterns

### Pattern 1: Cinematic State Broadcasting

**Problem:** Multiple components need synchronized cinematic progress.

**Solution:** Static progress getter, updated once per tick.

```
DeathCinematicClientHandler (tick update)
            ↓
  Static getProgress() method
            ↓
  ┌────────┴────────┬───────────────┬──────────────┐
  ↓                 ↓               ↓              ↓
CinematicCamera   DreadTexture   GeckoLib      Render
Controller        Animator       Animations    Layers
```

**Rationale:** Avoids drift from separate timers, ensures frame-perfect sync.

---

### Pattern 2: Render-State Pre-Computation

**Problem:** Cannot access entity during render thread (1.21+).

**Solution:** Pre-compute all render data in `extractRenderState()`.

```
Game Thread (tick):
  DreadEntity exists, can query state
            ↓
  extractRenderState(DreadRenderState state)
    → state.putData(UV_OFFSET, ...)
    → state.putData(TEXTURE_VARIANT, ...)
    → state.putData(BLOOD_OVERLAY_ALPHA, ...)
            ↓
Render Thread (async):
  DreadEntity reference invalid
  DreadRenderState contains all needed data
    → GeoModel.getTextureResource(renderState)
    → GeoRenderLayer.render(renderState)
```

**Rationale:** GeckoLib 5 architecture requirement, prevents race conditions.

---

### Pattern 3: Client-Side Effect Evaluation

**Problem:** Environmental effects need low latency, server doesn't care about visual-only effects.

**Solution:** Client evaluates triggers, applies effects locally, syncs only state changes.

```
ClientTickEvents.END_CLIENT_TICK
            ↓
  ProximityEffectManager.tick()
    → Calculates distance client-side
    → If distance < threshold:
        ├─ Play sound (client-only)
        ├─ Spawn particles (client-only)
        └─ Send packet to server (state change, e.g., close door)
            ↓
  Server receives packet
    → Validates request
    → Updates block state
    → Broadcasts to other clients
```

**Rationale:** Visual effects have no server authority, reduce network traffic, improve responsiveness.

---

## Architecture Patterns to Follow

### Pattern 1: Mixin Order Coordination

**What:** Use explicit order values to control mixin application sequence.

**When:** Multiple mixins target the same method (e.g., Camera.update).

**Example:**
```java
@Mixin(value = Camera.class, priority = 900)
public class CameraMixin {
    // Existing shake effect
}

@Mixin(value = Camera.class, priority = 950)
public class CinematicCameraControllerMixin {
    // NEW: Cinematic override (applied after shake)
}
```

**Rationale:** Higher priority mixins apply later, allowing override without conflict.

---

### Pattern 2: Render-State Data Tickets

**What:** Use GeckoLib's DataTicket system to pass data from game thread to render thread.

**When:** Render decisions depend on entity state unavailable during rendering.

**Example:**
```java
public class DreadEntity {
    public static final DataTicket<Float> CINEMATIC_PROGRESS =
        new DataTicket<>("cinematic_progress", 0.0f);

    @Override
    public void extractRenderData(DreadRenderState renderState) {
        float progress = DeathCinematicClientHandler.getProgress();
        renderState.putData(CINEMATIC_PROGRESS, progress);
    }
}

public class DreadRenderer {
    @Override
    public void render(...) {
        float progress = renderState.getData(DreadEntity.CINEMATIC_PROGRESS);
        // Use progress for render decisions
    }
}
```

**Rationale:** Thread-safe data passing, GeckoLib 5 best practice.

---

### Pattern 3: Event-Driven Effect Triggers

**What:** Use Fabric event system instead of polling or custom tick loops.

**When:** Triggering effects based on game state changes.

**Example:**
```java
// Registration
ClientTickEvents.END_CLIENT_TICK.register(ProximityEffectManager::tick);

// Unregister when no longer needed to prevent memory leaks
ClientTickEvents.END_CLIENT_TICK.unregister(ProximityEffectManager::tick);
```

**Rationale:** Integrates with Fabric lifecycle, avoids manual tick management, better mod compatibility.

---

### Pattern 4: Timeline-Based Keyframes

**What:** Define camera paths and animation timing as declarative timelines.

**When:** Complex sequences with multiple waypoints.

**Example:**
```java
Timeline DEATH_SEQUENCE = Timeline.builder()
    .keyframe(0.0f, CameraKeyframe.at(player).lookingAt(dread))
    .keyframe(1.5f, CameraKeyframe.circling(dread, 3, 1.5))
    .keyframe(3.0f, CameraKeyframe.closeup(dread.face))
    .keyframe(4.5f, CameraKeyframe.fadeToBlack())
    .interpolation(InterpolationType.SMOOTH)
    .build();

// Evaluate at render time
CameraKeyframe frame = DEATH_SEQUENCE.evaluate(progress);
camera.setPosition(frame.position);
camera.setRotation(frame.rotation);
```

**Rationale:** Easier to author/edit sequences, separates timing from logic, supports interpolation.

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Direct Entity Manipulation During Rendering

**What goes wrong:** Accessing entity fields directly in render methods.

**Why it happens:** Works in pre-1.21 versions, breaks with render thread separation.

**Consequences:** NullPointerException, race conditions, crashes.

**Prevention:**
```java
// BAD (1.21+)
@Override
public void render(DreadEntity entity, ...) {
    float health = entity.getHealth(); // Entity may not exist on render thread!
}

// GOOD
@Override
public void extractRenderState(DreadRenderState state) {
    state.putData(HEALTH_TICKET, this.getHealth());
}

@Override
public void render(DreadRenderState state, ...) {
    float health = state.getData(HEALTH_TICKET);
}
```

---

### Anti-Pattern 2: Separate Cinematic Timers

**What goes wrong:** Each component maintains its own cinematic timer.

**Why it happens:** Seems simpler than coordinating a shared timer.

**Consequences:** Components drift out of sync, texture changes don't match camera movements.

**Prevention:**
```java
// BAD
class CinematicCamera {
    private long myStartTime;
    private float getProgress() { return ...; } // Separate timer!
}

class DreadTextureAnimator {
    private long myStartTime;
    private float getProgress() { return ...; } // Another separate timer!
}

// GOOD
class DeathCinematicClientHandler {
    private static long startTime;
    public static float getProgress() { return ...; } // Single source of truth
}

// All components query DeathCinematicClientHandler.getProgress()
```

---

### Anti-Pattern 3: Networking Visual-Only Effects

**What goes wrong:** Sending packets to server for client-side-only particles/sounds.

**Why it happens:** Habit from server-authoritative game logic.

**Consequences:** Unnecessary network traffic, server load, lag.

**Prevention:**
```java
// BAD
void spawnBloodParticle() {
    NetworkHandler.sendToServer(new SpawnBloodParticlePacket(pos));
    // Server has to broadcast to all clients, then clients render
}

// GOOD
void spawnBloodParticle() {
    Minecraft.getInstance().level.addParticle(ParticleTypes.BLOOD, pos, ...);
    // Client spawns directly, no network round-trip
}
```

---

### Anti-Pattern 4: Mixin Conflicts with Same Priority

**What goes wrong:** Multiple mixins target same method with default priority.

**Why it happens:** Not specifying explicit priority values.

**Consequences:** Unpredictable application order, different behavior depending on mod load order.

**Prevention:**
```java
// BAD
@Mixin(Camera.class) // Default priority (1000)
public class CameraMixin { ... }

@Mixin(Camera.class) // Also default priority (1000) - conflict!
public class CinematicCameraMixin { ... }

// GOOD
@Mixin(value = Camera.class, priority = 900)
public class CameraMixin { ... }

@Mixin(value = Camera.class, priority = 950)
public class CinematicCameraMixin { ... }
```

---

## Build Order Recommendations

**Phase structure based on dependencies:**

### Phase 1: Cinematic Camera Control (Foundation)
**Why first:** Camera override is the most complex component, all other features are visible through the camera.

**Components:**
- `CinematicCameraController` with basic position/rotation override
- Mixin integration with existing `CameraMixin`
- Timeline keyframe system
- Integration with `DeathCinematicClientHandler`

**Validation:** Camera follows predefined path during death sequence, shake still works.

**Estimated complexity:** HIGH (mixin coordination, render-time injection)

---

### Phase 2: Animated Textures (Visual Polish)
**Why second:** Depends on camera being positioned correctly to see effects, requires GeckoLib render state understanding.

**Components:**
- Texture variant switching in `DreadEntity.getTextureResource()`
- OR UV offset animation for glitch effect
- OR `GeoRenderLayer` for blood overlay
- Synchronization with `DeathCinematicClientHandler.getProgress()`

**Validation:** Dread texture changes during kill sequence, timing matches camera keyframes.

**Estimated complexity:** MEDIUM (GeckoLib 5 render state, timing sync)

---

### Phase 3: Environmental Effects - Proximity (Atmosphere)
**Why third:** Independent of camera/texture systems, can be developed in parallel with Phase 2.

**Components:**
- `ProximityEffectManager` with client tick event registration
- Door slam trigger + animation
- Light flicker trigger + particle effects
- Distance calculation optimization (spatial partitioning)

**Validation:** Doors close and lights flicker when Dread approaches during gameplay.

**Estimated complexity:** MEDIUM (performance optimization needed for chunk-wide scans)

---

### Phase 4: Environmental Effects - Crawl Trail (Final Polish)
**Why last:** Simplest component, depends on existing `DownedStateClientHandler`.

**Components:**
- `CrawlBloodTrailEffect` with particle spawning
- Integration with `DownedStateClientHandler` lifecycle
- Particle rate limiting (performance)

**Validation:** Blood particles trail behind crawling player.

**Estimated complexity:** LOW (straightforward particle system usage)

---

**Phase ordering rationale:**
- **Camera first:** Everything else is rendered through the camera; must work correctly before other visuals matter
- **Textures second:** Requires camera to be positioned to see effects; builds on GeckoLib understanding
- **Proximity third:** Can be developed in parallel with textures; independent system
- **Crawl trail last:** Simplest feature; good final polish item; no dependencies

**Parallelization opportunities:**
- Phases 2 and 3 can be developed simultaneously (different systems)
- Phase 4 is quick, can be done by separate developer while others work on 1-3

---

## Technical Risks & Mitigation

### Risk 1: Render Thread Separation Breaking Entity Access

**Likelihood:** HIGH (GeckoLib 5 architectural change)
**Impact:** CRITICAL (crashes, visual glitches)

**Mitigation:**
- Always use `extractRenderState()` for entity → render data transfer
- Test thoroughly on 1.21+ (thread separation active)
- Review GeckoLib 5 migration guide before implementation

---

### Risk 2: Mixin Conflicts with Other Mods

**Likelihood:** MEDIUM (Camera mixins are common)
**Impact:** MEDIUM (features don't work, or crash)

**Mitigation:**
- Use explicit priority values (900, 950)
- Inject at TAIL instead of RETURN when possible (less invasive)
- Test with popular camera mods (Freecam, Camera Utils, etc.)

---

### Risk 3: Performance Impact of Proximity Scanning

**Likelihood:** MEDIUM (depends on chunk size, door density)
**Impact:** MEDIUM (FPS drops in complex builds)

**Mitigation:**
- Cache nearby doors/lights (only rescan when chunks change)
- Use spatial partitioning (chunk-based lookups)
- Limit scan radius (12 blocks max)
- Throttle updates (every 5 ticks instead of every tick)

---

### Risk 4: Camera Timeline Desync

**Likelihood:** LOW (if single source of truth pattern followed)
**Impact:** HIGH (texture changes don't match camera position, breaks immersion)

**Mitigation:**
- Single `DeathCinematicClientHandler.getProgress()` method
- All components query this static method (no local timers)
- Unit test timeline evaluation (verify keyframes interpolate correctly)

---

## Sources

**HIGH Confidence (Official Documentation):**
- [Fabric Events](https://docs.fabricmc.net/develop/events)
- [Fabric Mixin @Inject](https://wiki.fabricmc.net/tutorial:mixin_injects)
- [Fabric Block Entities](https://docs.fabricmc.net/develop/blocks/block-entities)
- [GeckoLib 5 Changes](https://github.com/bernie-g/geckolib/wiki/Geckolib-5-Changes)
- [GeckoLib 4 Animated Textures](https://github.com/bernie-g/geckolib/wiki/Animated-Textures-(Geckolib4))
- [Minecraft Particles (Java Edition)](https://minecraft.wiki/w/Particles_(Java_Edition))
- [Fabric WorldRenderEvents API](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/client/rendering/v1/WorldRenderEvents.html)

**MEDIUM Confidence (Mod Analysis & Community):**
- [Free Camera API Architecture](https://deepwiki.com/AnECanSaiTin/Free-camera-API)
- [Camera Utils Mod](https://modrinth.com/mod/camera-utils)
- [Fabric ClientTickEvents](https://maven.fabricmc.net/docs/fabric-api-0.34.8+1.17/net/fabricmc/fabric/api/client/event/lifecycle/v1/ClientTickEvents.html)

**LOW Confidence (WebSearch - unverified):**
- [GeckoLib texture swapping runtime discussion](https://mcreator.net/forum/103630/how-change-geckolib-animated-mobs-texture-using-procedures)
