# Technology Stack: v2.0 Atmosphere & Dread

**Project:** Dread Horror Mod
**Version:** 2.0 (Cinematic Camera Control, Animated Textures, Environmental Effects)
**Researched:** 2026-01-27
**Confidence:** HIGH

## Executive Summary

v2.0 requires NO new runtime dependencies beyond the validated v1.0 stack. All four new feature categories (camera position control, animated entity textures, environmental effects, blood trail particles) use existing Fabric API, GeckoLib 4.7.1, and standard Minecraft entity/rendering systems.

**Key Finding:** Minecraft's Camera class and existing CameraMixin provide sufficient control for cinematic camera positioning. Animated textures have THREE viable approaches (GeckoLib .mcmeta, custom RenderLayer with UV offsets, shader-based), each with distinct tradeoffs. Environmental effects use standard AI goals and block state manipulation—no special libraries needed.

**Architecture Decision:** Use Camera.setPos() + moveTo() for camera positioning (not entity-based spectator mode), GeckoLib .mcmeta for simple animated textures (pulsing runes), custom RenderLayer with tick-based UV offsets for complex animations (writhing forms with precise control), and Fabric particle API for blood trails.

---

## Runtime Stack (NO CHANGES from v1.0)

| Component | Version | Status | Notes |
|-----------|---------|--------|-------|
| Minecraft | 1.21.1 | Unchanged | Camera class, Entity AI, particle system |
| Fabric Loader | 0.18.2 | Unchanged | Core mod loader |
| Fabric API | 0.116.8+ | Unchanged | Particle registration, entity events, client tick |
| GeckoLib | 4.7.1 | Unchanged | Entity animations, .mcmeta texture support |
| Satin API | 1.17.0 | Unchanged | Post-processing shaders (existing use only) |

**Rationale:** All v2.0 features leverage existing capabilities:
- Camera control → Camera.setPos(), Camera.moveTo() (vanilla API)
- Animated textures → GeckoLib .mcmeta OR custom RenderLayer with UV manipulation
- Environmental effects → Entity AI goals, block state changes
- Blood trail → FabricParticleTypes (Fabric API)

---

## New Stack Requirements (v2.0 Features)

### 1. Camera Position/Rotation Control (Death Cinematic)

**Goal:** During death cinematic, camera pulls back from player to show scene, then zooms to Dread's face

**Existing Integration Points:**
- `CameraMixin.java` (already in codebase) - mixins into Camera class for render-time manipulation
- `DeathCinematicClientHandler.java` (v1.0) - controls camera lock and shake

**New Requirements:** Position camera at arbitrary world coordinates, not locked to player entity

#### Approach: Camera.setPos() + Camera.moveTo()

**API:** Vanilla `Camera` class (yarn-mapped)
**Package:** `net.minecraft.client.render`
**Confidence:** HIGH (Camera class methods exist, third-party mods demonstrate usage)

**Camera Positioning Methods:**
```java
// Set camera position directly
camera.setPos(x, y, z);

// Set rotation (yaw = horizontal, pitch = vertical)
camera.moveBy(yawDelta, pitchDelta, 0);

// Update internal state (critical after position changes)
camera.update(
    world,               // ClientWorld
    entity,              // Dummy entity or player
    thirdPerson,         // true for third-person perspective
    inverseView,         // false for normal view
    tickDelta            // Frame interpolation
);
```

**Integration Pattern:**
```java
// In DeathCinematicClientHandler.tick()
MinecraftClient client = MinecraftClient.getInstance();
Camera camera = client.gameRenderer.getCamera();

// Phase 1: Pull back (first 20 ticks)
if (cinematicTimer < 20) {
    Vec3d pullBackPos = calculatePullBackPosition(player, dreadEntity);
    camera.setPos(pullBackPos.x, pullBackPos.y, pullBackPos.z);

    // Look at midpoint between player and Dread
    Vec3d lookTarget = player.getPos().lerp(dreadEntity.getPos(), 0.5);
    setRotationToLookAt(camera, lookTarget);
}

// Phase 2: Zoom to Dread's face (final 20 ticks)
else if (cinematicTimer >= 60 && cinematicTimer < 80) {
    Vec3d facePos = dreadEntity.getPos().add(0, dreadEntity.getHeight() * 0.8, 0);
    Vec3d zoomPos = facePos.add(dreadEntity.getRotationVec(1.0f).multiply(-0.5));
    camera.setPos(zoomPos.x, zoomPos.y, zoomPos.z);

    // Look directly at face
    setRotationToLookAt(camera, facePos);
}

// Update camera state after position changes
camera.update(client.world, client.player, true, false, client.getRenderTickCounter().getTickDelta(true));
```

**Why NOT Spectator Mode Approach:**
- Spectator mode (`client.setCameraEntity(dreadEntity)`) locks camera to entity's position/rotation
- No support for arbitrary positions (pull-back phase requires position between player and Dread)
- Entity-based camera inherits entity rotation, preventing smooth look-at-target control
- Switching camera entities causes jarring jumps, breaks smooth cinematics

**Why Camera.setPos() is Sufficient:**
- Direct position control for pull-back phase
- Independent rotation via moveBy() or direct rotation math
- Works with existing CameraMixin (shake already modifies rotation)
- No new dependencies or mixins needed beyond existing CameraMixin

**Integration with Existing CameraMixin:**
```java
// CameraMixin already intercepts camera updates for shake
// v2.0 position control happens BEFORE shake is applied
// Order: setPos() → update() → CameraMixin applies shake offset → render
```

**Alternatives Considered:**
| Approach | Why Not |
|----------|---------|
| **Invisible marker entity + setCameraEntity()** | Requires entity creation, teleportation sync, entity collision concerns, overly complex |
| **GameRenderer mixin for projection matrix** | Too invasive, breaks compatibility with shader mods, unnecessary for position control |
| **Camera Utils mod integration** | Adds dependency for camera detachment feature we can implement with vanilla API |

---

### 2. Animated Entity Textures

**Goal:** Pulsing runes, writhing organic forms, eye reveals during kill sequence

**Requirement Analysis:**
- **Pulsing runes:** Simple opacity fade (alpha channel animation) or glow intensity
- **Writhing forms:** Complex UV offset animation (texture coordinates shift over time)
- **Eye reveals:** Texture swap or alpha mask animation (hidden → revealed)

#### Three Viable Approaches (Use Different Techniques for Different Effects)

##### Approach A: GeckoLib .mcmeta (RECOMMENDED for Simple Animations)

**When to Use:** Pulsing runes, eye blink cycles, simple repeating patterns

**API:** GeckoLib 4.7.1 (already in project) + Minecraft .mcmeta standard
**Package:** Assets directory (`textures/entity/dread/`)
**Confidence:** HIGH (GeckoLib wiki documents, verified in v1.0)

**Setup:**
```
resources/assets/dread/textures/entity/dread/
├── dread_eyes.png           # 64x64 texture with 4 frames stacked vertically = 64x256 total
└── dread_eyes.png.mcmeta    # Animation configuration
```

**dread_eyes.png.mcmeta:**
```json
{
  "animation": {
    "frametime": 10,
    "interpolate": false,
    "frames": [0, 1, 2, 3, 2, 1]
  }
}
```

**Requirements:**
- Each frame must be square (64x64 per frame for 64-wide texture)
- Texture height = frame_height * frame_count (64x256 for 4 frames)
- Works automatically with GeckoLib renderer (no code changes)

**Limitations (CRITICAL):**
- **Not compatible with GeckoLib emissive textures** (glow masks)
- Cannot trigger via animation events (plays continuously)
- All frames same dimensions (no morphing)

**Why Use This:**
- Zero code for simple looping animations
- Minecraft's texture system handles it automatically
- BlockBench integration (preview animated textures)
- Existing GeckoLib renderer "just works"

**Use Cases in v2.0:**
- Pulsing runes on Dread's body (continuous subtle pulse)
- Eye blink cycles during idle/hunt states
- Emissive orb pulsing (if not using glow masks)

##### Approach B: Custom RenderLayer with Tick-Based UV Offsets (RECOMMENDED for Complex Animations)

**When to Use:** Writhing organic forms, triggered animations (eye reveal on kill), UV-based distortion effects

**API:** Fabric RenderLayer system + custom FeatureRenderer
**Package:** Custom renderer extending `FeatureRenderer<DreadEntity, DreadEntityModel>`
**Confidence:** HIGH (Fabric docs, GitHub discussion verifies approach, slime outer layer example)

**Setup:**
```java
// 1. Create custom FeatureRenderer for animated layer
public class DreadAnimatedTextureRenderer extends FeatureRenderer<DreadEntity, DreadEntityModel> {
    private static final Identifier WRITHING_TEXTURE =
        Identifier.of("dread", "textures/entity/dread/dread_writhing.png");

    private float uvOffsetU = 0.0f;
    private float uvOffsetV = 0.0f;

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, DreadEntity entity, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {

        // Tick-based UV animation (frame-rate independent)
        float ticks = entity.age + tickDelta;
        uvOffsetU = (float)Math.sin(ticks * 0.05f) * 0.1f;  // Horizontal writhing
        uvOffsetV = ticks * 0.02f % 1.0f;                    // Vertical crawling

        // Triggered animation: eye reveal during kill
        if (entity.isInKillAnimation()) {
            uvOffsetV = entity.getKillAnimationProgress();  // 0.0 → 1.0 reveals eyes
        }

        // Get appropriate RenderLayer
        RenderLayer layer = RenderLayer.getEntityTranslucent(WRITHING_TEXTURE);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);

        // Render model with UV offset applied
        this.getContextModel().render(matrices, vertexConsumer, light,
            OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 0.6f);
    }
}

// 2. Register in DreadEntityRenderer constructor
public DreadEntityRenderer(EntityRendererProvider.Context context) {
    super(context, new DreadEntityModel(context.getPart(DREAD_LAYER)), 0.5f);
    this.addFeature(new DreadAnimatedTextureRenderer(this));
}

// 3. UV offset application in model (requires custom vertex consumer or shader)
// ALTERNATIVE: Use separate texture atlas with pre-offset UVs per frame
```

**RenderLayer Options:**
| Layer | Use Case | Alpha | Culling |
|-------|----------|-------|---------|
| `EntityTranslucent` | Semi-transparent writhing forms | Yes | No backface culling |
| `EntityCutoutNoCull` | Opaque animated layers | Sharp alpha | No culling |
| `Eyes` | Glowing animated eyes | Fullbright | No culling |

**Why Use This:**
- **Full control:** Trigger animations via entity state (kill sequence, phase changes)
- **Complex UV manipulation:** Writhing/distortion effects impossible with .mcmeta
- **Multiple layers:** Combine with base texture and emissive glow masks
- **Event-driven:** Animation tied to gameplay state, not continuous loop

**Limitations:**
- Requires Java code (not just asset changes)
- More complex than .mcmeta approach
- UV offset application requires either custom VertexConsumer manipulation OR pre-baked texture atlas

**Use Cases in v2.0:**
- Writhing organic forms during kill sequence
- Eye reveal triggered by animation event (hidden until climax)
- Distortion effects tied to Dread's emotional state

**Technical Note: UV Offset Application**

UV offset in RenderLayer requires one of two approaches:

**Option 1: Shader-based (requires Satin integration)**
```java
// Apply UV offset via shader uniform (Satin API already in project)
ManagedShaderEffect shader = satinManager.getShader("dread:entity_uv_offset");
shader.setUniformValue("UVOffset", uvOffsetU, uvOffsetV);
```

**Option 2: Vertex manipulation (more control, more complex)**
```java
// Manually offset UV coordinates when building vertices
// Requires custom model part rendering or vertex consumer wrapper
```

**RECOMMENDATION for v2.0:** Start with Option 2 (vertex manipulation) for maximum control and no shader dependency. Option 1 (shader-based) is cleaner but requires creating custom entity shader.

##### Approach C: Shader-Based Animated Textures (NOT RECOMMENDED for v2.0)

**When to Use:** Procedural effects (flowing energy, distortion fields), global time-based animations

**API:** Satin API (already in project) + custom GLSL shaders
**Confidence:** MEDIUM (Satin docs verify capability, but entity shader integration less documented)

**Why NOT Recommended for v2.0:**
- **Overkill for texture animation:** .mcmeta and UV offsets cover all v2.0 needs
- **Complexity:** Requires GLSL shader authoring, uniform management, entity RenderLayer integration
- **Maintenance:** Custom shaders break with Minecraft rendering updates
- **Satin is for post-processing:** Designed for screen-space effects (vignette, blur), not entity textures

**When This Would Be Appropriate:**
- Procedural distortion (heat shimmer around Dread)
- Global time-based effects (all Dread forms pulse synchronized)
- Performance optimization (GPU-side animation instead of CPU UV calculations)

**If Needed Later:**
```glsl
// entity_animated.fsh (fragment shader)
uniform sampler2D Sampler0;
uniform float GameTime;  // Provided by Minecraft

in vec2 texCoord0;
out vec4 fragColor;

void main() {
    // Offset UV based on time
    vec2 animatedUV = texCoord0;
    animatedUV.y += sin(GameTime * 1000.0 + texCoord0.x * 10.0) * 0.05;

    fragColor = texture(Sampler0, animatedUV);
}
```

---

### 3. Environmental Effects (Door Slams, Light Flickers)

**Goal:** Dread entity interacts with environment (closes doors, extinguishes lights) to create presence

**Existing Capabilities:**
- Entity AI goals (vanilla system, GeckoLib entities support)
- Block state manipulation (vanilla API)
- Server-side logic (prevent client-side desync)

#### Door Interaction

**API:** Custom AI Goal extending vanilla goal system
**Package:** `net.minecraft.entity.ai.goal`
**Confidence:** HIGH (vanilla system, well-documented)

**Implementation Pattern:**
```java
public class DreadDoorSlamGoal extends Goal {
    private final DreadEntity entity;
    private BlockPos targetDoorPos;
    private int slamCooldown = 0;

    public DreadDoorSlamGoal(DreadEntity entity) {
        this.entity = entity;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // Only during hunt/ambient presence phases
        if (!entity.isInHuntingMode() || slamCooldown > 0) return false;

        // Find nearby open doors
        targetDoorPos = findNearbyOpenDoor(entity.getBlockPos(), 8);
        return targetDoorPos != null;
    }

    @Override
    public void tick() {
        // Look at door menacingly
        entity.getLookControl().lookAt(
            targetDoorPos.getX() + 0.5,
            targetDoorPos.getY() + 0.5,
            targetDoorPos.getZ() + 0.5
        );

        // When close enough, slam door
        if (entity.getBlockPos().isWithinDistance(targetDoorPos, 3.0)) {
            slamDoor(targetDoorPos);
            slamCooldown = 200; // 10 seconds
            this.stop();
        }
    }

    private void slamDoor(BlockPos pos) {
        BlockState doorState = entity.getWorld().getBlockState(pos);
        if (doorState.getBlock() instanceof DoorBlock && doorState.get(DoorBlock.OPEN)) {
            // Close door with block state change
            entity.getWorld().setBlockState(pos, doorState.with(DoorBlock.OPEN, false));

            // Play door slam sound (louder than normal close)
            entity.getWorld().playSound(null, pos, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,
                SoundCategory.BLOCKS, 1.5f, 0.8f);  // Louder, deeper pitch

            // Optional: Trigger nearby players' fear
            triggerFearResponseInRadius(pos, 10.0);
        }
    }
}

// Register in DreadEntity.initGoals()
@Override
protected void initGoals() {
    this.goalSelector.add(2, new DreadDoorSlamGoal(this));  // High priority (2)
    // ... existing goals
}
```

**Why This Works:**
- Standard Entity AI system (no new dependencies)
- Server-authoritative (block state changes sync to clients)
- Works with all door types (wooden, iron, modded)
- LookControl makes Dread face door before slamming (cinematic)

**Alternatives Considered:**
| Approach | Why Not |
|----------|---------|
| **Client-side door animation only** | Desync issues, doors appear open server-side, collision broken |
| **Sensor component + behavior tree** | Overkill, AI goals are simpler for single-action behaviors |
| **Direct block breaking** | Destructive, doors should close not vanish |

#### Light Source Manipulation

**API:** Block state changes for light-emitting blocks
**Confidence:** MEDIUM (vanilla API exists, but torch/lantern extinguishing requires creative approach)

**Challenge:** Minecraft light sources (torches, lanterns) don't have "extinguished" states in vanilla

**Solution Options:**

**Option A: Replace with Unlit Variants (Recommended)**
```java
private void extinguishLight(BlockPos pos) {
    BlockState lightState = world.getBlockState(pos);

    // Replace torch with unlit version (requires custom block or air)
    if (lightState.getBlock() == Blocks.TORCH) {
        world.setBlockState(pos, Blocks.AIR.getDefaultState());  // Temporary removal

        // Schedule relight after delay
        scheduleLightRestore(pos, Blocks.TORCH.getDefaultState(), 200);  // 10 seconds
    }

    // Campfire has native extinguishing
    else if (lightState.getBlock() == Blocks.CAMPFIRE && lightState.get(CampfireBlock.LIT)) {
        world.setBlockState(pos, lightState.with(CampfireBlock.LIT, false));
    }

    // Redstone lamp via power removal (if near redstone)
    else if (lightState.getBlock() == Blocks.REDSTONE_LAMP && lightState.get(RedstoneLampBlock.LIT)) {
        world.setBlockState(pos, lightState.with(RedstoneLampBlock.LIT, false));
    }
}
```

**Option B: Particle Effect + Light Level Reduction (Non-Destructive)**
```java
// Don't actually change blocks, just reduce light temporarily via custom light tracking
// Requires LightingProvider manipulation (complex, high risk of bugs)
// NOT RECOMMENDED for v2.0 - too invasive
```

**Recommendation for v2.0:**
- **Phase 1:** Support campfires only (native LIT property, no block replacement needed)
- **Phase 2:** Consider adding custom "Unlit Torch" block (placeholder for removed torches)
- **Alternative:** Particle effect (smoke puff) + sound without actual light removal (visual suggestion, less technical risk)

**Environmental Effect AI Goal:**
```java
public class DreadEnvironmentManipulationGoal extends Goal {
    @Override
    public void tick() {
        // Prioritize based on context
        if (shouldExtinguishLights()) {
            BlockPos lightPos = findNearestLight(8);
            if (lightPos != null) extinguishLight(lightPos);
        }
        else if (shouldSlamDoors()) {
            // ... door logic
        }
    }
}
```

---

### 4. Blood Trail Particles (Crawling While Downed)

**Goal:** Leave particle trail when player crawls in downed state

**Existing Integration:**
- `DownedStateComponent.java` (tracks downed state)
- `DownedStateClientHandler.java` (client-side effects)

**API:** Fabric Particle API (already in Fabric API)
**Package:** `net.fabricmc.fabric.api.particle.v1`
**Confidence:** HIGH (official Fabric docs, v1.0 already uses particles)

**Implementation Pattern:**

**1. Register Particle Type (Server-Side)**
```java
// In DreadMod.onInitialize()
public static final SimpleParticleType BLOOD_DRIP = FabricParticleTypes.simple();

Registry.register(
    BuiltInRegistries.PARTICLE_TYPE,
    Identifier.of("dread", "blood_drip"),
    BLOOD_DRIP
);
```

**2. Register Particle Factory (Client-Side)**
```java
// In DreadModClient.onInitializeClient()
ParticleFactoryRegistry.getInstance().register(
    DreadMod.BLOOD_DRIP,
    DripParticle.BloodFactory::new  // Use vanilla drip particle behavior
);
```

**3. Define Particle Textures**
```
resources/assets/dread/
├── textures/particle/
│   └── blood_drip.png        # 8x8 red droplet texture
└── particles/
    └── blood_drip.json        # Particle configuration
```

**blood_drip.json:**
```json
{
  "textures": [
    "dread:blood_drip"
  ]
}
```

**4. Spawn Particles When Crawling**
```java
// In DownedStateClientHandler.tick() - client-side particle spawning
private static void tick() {
    if (isDownedAndCrawling()) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        // Spawn particles based on movement
        Vec3d velocity = player.getVelocity();
        if (velocity.horizontalLength() > 0.01) {  // Only when moving

            // Spawn every 5 ticks (4 times per second)
            if (player.age % 5 == 0) {
                // Spawn at player's torso position (lying down)
                double x = player.getX();
                double y = player.getY() + 0.2;  // Just above ground
                double z = player.getZ();

                // Slight random offset for natural spread
                x += (player.getRandom().nextFloat() - 0.5) * 0.4;
                z += (player.getRandom().nextFloat() - 0.5) * 0.4;

                player.getWorld().addParticle(
                    DreadMod.BLOOD_DRIP,
                    x, y, z,
                    0, 0, 0  // No initial velocity (falls to ground)
                );
            }
        }
    }
}
```

**Particle Behavior Options:**

| Factory | Behavior | Use Case |
|---------|----------|----------|
| `DripParticle.BloodFactory` | Falls to ground, small splash | Realistic dripping blood |
| `BlockParticle.Factory` | Custom texture, no gravity | Persistent blood decals |
| Custom factory | Full control | Unique effects (fading, spreading) |

**Recommendation:** Use `DripParticle.BloodFactory` (vanilla blood drop behavior) for v2.0 simplicity. Custom factory if persistence needed (blood stays on ground).

**Why This Works:**
- Fabric API already in project (no new dependency)
- Client-side particle spawning (no server load)
- Vanilla particle system handles rendering, physics, despawning
- Texture customization via simple PNG

**Alternatives Considered:**
| Approach | Why Not |
|----------|---------|
| **Block-based blood decals** | Persists forever, world pollution, server-side block changes |
| **Entity-based blood trail** | Overkill, entities expensive for cosmetic effect |
| **Shader-based trail rendering** | Complex, breaks with shader packs |

---

## What NOT to Add

| Technology | Reason to Avoid |
|------------|-----------------|
| **Camera Utils mod** | Adds dependency for camera detachment feature we implement with vanilla Camera.setPos() |
| **Entity Texture Features (ETF) mod** | Adds random/emissive texture system we don't need—GeckoLib handles our use case |
| **Custom light engine/LightingProvider mixin** | Too invasive for simple light extinguishing, high bug risk, compatibility issues |
| **Shader-based texture animation** | Overkill when .mcmeta and UV offsets solve all v2.0 needs, maintenance burden |
| **Bedrock camera preset system** | Java Edition only, different API, not applicable |
| **AI library (behavior trees, GOAP)** | Vanilla AI goals sufficient for door/light interactions, unnecessary complexity |

---

## Architectural Integration with Existing Stack

### Camera Control Integration

```
Existing v1.0:
CameraMixin.java (render-time shake) → Camera rotation offsets

New v2.0:
DeathCinematicClientHandler.tick() → Camera.setPos() → Camera.update()
    ↓
CameraMixin.java (applies shake on top of new position)
    ↓
GameRenderer renders cinematic
```

**Key:** Camera positioning happens in tick handler BEFORE CameraMixin applies shake. Shake remains additive effect.

### Animated Textures Integration

```
Existing v1.0:
GeckoLib animation system → DreadEntityRenderer → Base texture

New v2.0 (Layered Approach):
GeckoLib base animation
    ↓
DreadEntityRenderer renders base texture
    ↓
DreadAnimatedTextureRenderer (FeatureRenderer) renders animated layer
    ↓
    ├─> .mcmeta animation (pulsing runes) - automatic
    └─> Custom UV offset (writhing forms) - tick-based calculation
```

**Key:** Multiple texture layers via FeatureRenderer pattern (like vanilla's slime outer layer, horse armor).

### Environmental Effects Integration

```
Existing v1.0:
DreadEntity AI goals (hunt, stalk, attack)

New v2.0:
DreadEntity.initGoals() adds:
    ├─> DreadDoorSlamGoal (priority 2, high)
    └─> DreadLightExtinguishGoal (priority 3, medium)
        ↓
        Server-side block state changes
        ↓
        Synced to clients automatically
```

**Key:** AI goals integrate seamlessly with existing goal priority system. Environmental effects happen server-side (authoritative).

### Blood Trail Particles Integration

```
Existing v1.0:
DownedStateClientHandler.tick() → Vignette, blur, drip particles

New v2.0:
DownedStateClientHandler.tick() → ... existing effects ...
    ↓
    └─> if (isMoving) spawn BLOOD_DRIP particles
        ↓
        Minecraft particle system handles rendering
```

**Key:** Blood trail is additional client-side effect in existing downed state handler. No changes to downed state logic.

---

## Recommended Stack Additions (Optional Enhancements)

### BlockBench (Asset Creation - Already Recommended in v1.1)

**Purpose:** Create animated texture variations, preview .mcmeta animations
**Version:** Latest (2026)
**Use:** Paint texture variations for animation frames, preview in 3D

**Why Relevant for v2.0:**
- Preview .mcmeta animated textures on Dread model before export
- Create multiple texture frames for eye reveal sequence
- Validate UV mapping for custom RenderLayer animations

### Audacity (Audio - Already in v1.1 Stack)

**Purpose:** Edit door slam sounds, light extinguish sounds
**Use:** Pitch-shift vanilla door sound for ominous slam, create electrical spark sounds for lights

---

## Version Compatibility Notes

| Component | v1.0 Version | v2.0 Status | Notes |
|-----------|--------------|-------------|-------|
| Fabric API | 0.116.8 | Compatible | ParticleFactoryRegistry stable across 1.21.x |
| GeckoLib | 4.7.1 | Compatible | .mcmeta support added in 4.0, stable in 4.7.1 |
| Satin API | 1.17.0 | Compatible | Not required for v2.0, but existing use (vignette) unaffected |
| Camera class | Yarn 1.21.1 | Stable | Camera.setPos(), update() methods stable since 1.17 |
| Entity AI Goals | Vanilla | Stable | Goal system unchanged since 1.14, yarn mappings current |

**No breaking changes expected:** All v2.0 features use stable, mature APIs (3+ year track record).

---

## Development Workflow

### Camera Control Testing
```bash
# In-game testing sequence
1. Trigger death cinematic (/dread test_cinematic)
2. Verify camera pulls back smoothly (no jitter)
3. Verify camera zooms to face
4. Verify transition to downed state (camera returns to player)
5. Check multiplayer sync (camera position client-side only)
```

### Animated Texture Workflow
```bash
# .mcmeta approach (pulsing runes)
1. Create multi-frame texture in BlockBench (stack vertically)
2. Export as dread_runes.png (64x256 for 4 frames)
3. Create dread_runes.png.mcmeta with animation config
4. Test in-game (automatic, no code changes)

# Custom RenderLayer approach (writhing forms)
1. Create writhing_forms.png texture
2. Implement DreadAnimatedTextureRenderer with UV offset logic
3. Register as feature renderer in DreadEntityRenderer
4. Test tick-based animation, verify frame rate independence
5. Tune UV offset math for desired effect intensity
```

### Environmental Effects Testing
```bash
# Door slam testing
1. Build test room with various door types (wood, iron, trap)
2. Spawn Dread in hunting mode
3. Verify door slam triggers at correct distance
4. Check sound plays (louder than normal close)
5. Test multiplayer sync (all players see door close)

# Light extinguish testing
1. Place campfires (native support first)
2. Spawn Dread near lights
3. Verify lights extinguish when Dread nearby
4. Check relight timer (if implemented)
5. Performance test (no lag from light updates)
```

### Blood Trail Particle Testing
```bash
# Blood trail testing
1. Enter downed state (/dread test_downed)
2. Crawl forward (movement while downed)
3. Verify particles spawn at torso height
4. Check particle frequency (not too sparse, not overwhelming)
5. Test performance (no FPS drop from particles)
6. Verify particles despawn (no buildup)
```

---

## Sources

### Camera Control (HIGH Confidence)
- [Camera Class - Fabric Yarn Mappings](https://maven.fabricmc.net/docs/yarn-1.17+build.13/net/minecraft/client/render/Camera.html) - setPos(), moveTo(), update() methods
- [Camera Utils Mod Source](https://github.com/henkelmax/camera-utils) - Demonstrates camera detachment with setPos()
- [Minecraft Wiki: Third-Person View](https://minecraft.wiki/w/Third-person_view) - Camera mechanics background
- Existing codebase: `CameraMixin.java`, `DeathCinematicClientHandler.java`

### Animated Textures (HIGH Confidence)
- [GeckoLib Wiki: Animated Textures (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Animated-Textures-(Geckolib4)) - .mcmeta support, limitations (emissive incompatibility)
- [Fabric Discussion: Translucent Entity Layers](https://github.com/orgs/FabricMC/discussions/2087) - FeatureRenderer pattern, RenderLayer usage
- [Fabric Rendering Documentation](https://docs.fabricmc.net/develop/rendering/world) - RenderLayer system overview
- [Entity Texture Features Mod](https://github.com/Traben-0/Entity_Texture_Features) - Reference implementation (not using, but validates approach)

### Entity AI Goals (HIGH Confidence)
- [Minecraft Wiki: Mob AI](https://minecraft.wiki/w/Mob_AI) - Goal system explanation, priority system
- [Microsoft Learn: AI Goal List](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/entityreference/examples/aigoallist?view=minecraft-bedrock-stable) - AI goal types (Bedrock, but Java similar)
- [Forge JavaDocs: AI Goal Package](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.16.5/net/minecraft/entity/ai/goal/package-summary.html) - Goal class structure
- [Sponge Docs: Entity AI](https://docs.spongepowered.org/stable/en/plugin/entities/ai.html) - AI system architecture

### Particle System (HIGH Confidence)
- [Fabric Documentation: Creating Particles](https://docs.fabricmc.net/develop/rendering/particles/creating-particles) - Official guide (updated 2026-01-11)
- [Fabric Wiki: Adding Particles](https://wiki.fabricmc.net/tutorial:particles) - Registration, factories, JSON configuration
- [Entity Blood Particles Mod](https://modrinth.com/mod/entity-blood-particles) - Reference implementation for blood effects
- [Fabric API: FabricParticleTypes](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/particle/v1/FabricParticleTypes.html) - Particle registration API

### Block State Manipulation (MEDIUM Confidence)
- [Minecraft Wiki: Block States](https://minecraft.wiki/w/Block_states) - DoorBlock.OPEN, CampfireBlock.LIT properties
- Vanilla source code: DoorBlock, CampfireBlock, RedstoneLampBlock classes
- [Minecraft Commands: Entity Interaction](https://minecraftcommands.github.io/wiki/questions/lookat.html) - Block interaction patterns

---

## Confidence Assessment

| Area | Level | Rationale |
|------|-------|-----------|
| **Camera Control** | HIGH | Camera.setPos() documented, third-party mods demonstrate, existing CameraMixin integrates cleanly |
| **Animated Textures** | HIGH | GeckoLib .mcmeta officially documented, FeatureRenderer pattern verified in Fabric discussion, multiple approaches validated |
| **Environmental Effects** | HIGH | Vanilla AI goal system stable, block state changes standard, door/light properties documented |
| **Blood Trail Particles** | HIGH | Fabric API official docs (updated 2026), particle system unchanged since 1.16, existing mod references |
| **UV Offset Animation** | MEDIUM | Custom RenderLayer verified, but UV offset application requires testing (vertex manipulation approach less documented) |
| **Light Extinguishing** | MEDIUM | Campfire support certain, torch/lantern extinguishing requires creative solution (block replacement or custom block) |

---

## Next Steps for Roadmap

Based on stack research, recommended phase structure:

1. **Phase 1: Camera Positioning** - Camera.setPos() + moveTo() implementation, test pull-back and zoom
2. **Phase 2: Simple Animated Textures** - GeckoLib .mcmeta for pulsing runes (asset-only, no code)
3. **Phase 3: Blood Trail Particles** - Fabric particle registration, spawn logic in downed state
4. **Phase 4: Environmental AI Goals** - Door slam goal, campfire extinguishing (server-side logic)
5. **Phase 5: Complex Animated Textures** - Custom RenderLayer with UV offsets for writhing forms (code-heavy)

**Dependency notes:**
- Camera positioning should come first (validates core cinematic control)
- Simple animated textures (Phase 2) can parallelize with blood trail (Phase 3)
- Environmental effects (Phase 4) independent of texture/particle work
- Complex texture animation (Phase 5) last (highest technical risk, depends on Phase 2 validation)

**Research flags:**
- UV offset application method (vertex manipulation vs shader) needs prototyping in Phase 5
- Light extinguishing scope decision (campfire-only vs custom torch block) during Phase 4
- Camera position interpolation smoothness testing in Phase 1

---

## Validation Checklist

- [x] All v2.0 features covered (camera, animated textures, environmental, particles)
- [x] No new runtime dependencies (uses existing Fabric API, GeckoLib, vanilla systems)
- [x] Multiple approaches documented for animated textures (GeckoLib .mcmeta, custom RenderLayer, shader-based)
- [x] Integration points with existing stack identified (CameraMixin, GeckoLib renderer, AI goals)
- [x] Confidence levels assigned with rationale
- [x] Sources cited (official docs, GitHub discussions, mods, existing codebase)
- [x] Alternatives considered and rejected with reasoning
- [x] Phase ordering recommendations based on dependencies and risk
- [x] Testing workflows provided for each feature category
