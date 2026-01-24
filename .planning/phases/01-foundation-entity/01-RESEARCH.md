# Phase 1: Foundation & Entity - Research

**Researched:** 2026-01-23
**Domain:** GeckoLib Animated Entity Creation for Fabric 1.21.x
**Confidence:** HIGH

## Summary

Phase 1 focuses on creating the Dread entity as a visible, animated horror creature using GeckoLib for complex animations and Fabric's entity system for registration and rendering. The standard approach combines PathAwareEntity for AI-ready mob foundation, GeckoLib 4.x for sophisticated animations (idle twitching, jerky movement, head tracking), and Fabric's entity registration APIs.

Research confirms that GeckoLib is the industry standard for complex entity animations in Fabric mods, providing keyframe-based animations, concurrent animation support, emissive textures for glowing effects, and seamless Blockbench integration. The Fabric entity system has straightforward registration patterns, but requires careful attention to client/server separation and entity tracking configuration.

Key implementation challenges identified: texture variant selection at runtime (for multiple Dread forms), torch extinguishing mechanics (requires block manipulation on spawn), performance optimization for complex models, and preventing entity memory leaks.

**Primary recommendation:** Use GeckoLib 4.x with AutoGlowingGeoLayer for emissive eyes/tentacles, implement texture variants via getTexture() override in renderer, set tracking range to 128+ blocks for stalking behavior, and implement remove() cleanup to prevent memory leaks from day one.

## Standard Stack

The established libraries/tools for creating animated custom entities in Fabric 1.21.x:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| GeckoLib | 4.8.2 (Fabric 1.21.1) | Entity animation engine | Industry standard for complex 3D keyframe animations. Provides concurrent animation support, 30+ easing functions, sound/particle keyframes, event triggers, and math-based animations. Superior to vanilla animations for multi-state creatures with twitching/jerky movement |
| Fabric API | 0.139.4+1.21.11 | Core modding hooks | Required for entity registration, rendering callbacks, and attribute management. Provides FabricEntityTypeBuilder and FabricDefaultAttributeRegistry |
| Blockbench | 4.x+ | Model & animation tool | Official GeckoLib modeling tool. GeckoLib Animation Utils plugin required. Exports .geo.json models and .animation.json files directly compatible with GeckoLib |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Yarn Mappings | 1.21.11+build.x | Readable code names | Standard for Fabric 1.21.11. Note: 1.21.11 is last Yarn-mapped version; future versions use Mojang mappings |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| GeckoLib | Vanilla Animations (1.20.4+) | Vanilla system lacks concurrent animations, sound/particle keyframes, and math-based animations needed for complex horror creature. GeckoLib provides animation controller logic essential for state-based twitching/jerky movement |
| PathAwareEntity | Custom Entity extending LivingEntity | PathAwareEntity provides built-in pathfinding and AI goal framework needed for future stalking behavior. Starting with simpler base requires refactor when AI added |
| Blockbench | Manual .geo.json writing | Blockbench's visual editor with GeckoLib plugin is industry standard. Manual JSON is error-prone and time-consuming for complex models with 20+ bones |

**Installation:**
```gradle
dependencies {
    minecraft "com.mojang:minecraft:1.21.11"
    mappings "net.fabricmc:yarn:1.21.11+build.x:v2"
    modImplementation "net.fabricmc:fabric-loader:0.18.2"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.139.4+1.21.11"

    // GeckoLib for entity animation
    modImplementation "software.bernie.geckolib:geckolib-fabric-1.21:4.8.2"
}
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/example/dread/
├── DreadMod.java                      # ModInitializer (common init)
├── entity/
│   ├── DreadEntity.java               # PathAwareEntity implementation
│   ├── DreadEntityRenderer.java       # Client renderer (CLIENT SIDE)
│   └── DreadEntityModel.java          # GeckoLib geo model (CLIENT SIDE)
├── registry/
│   └── ModEntities.java               # Entity type registration
└── client/
    └── DreadClient.java               # ClientModInitializer

src/main/resources/
└── assets/dread/
    ├── geo/
    │   └── dread_entity.geo.json      # Blockbench exported model
    ├── animations/
    │   └── dread_entity.animation.json # Blockbench exported animations
    ├── textures/entity/
    │   ├── dread_base.png             # Base form texture
    │   ├── dread_base_glowmask.png    # Emissive eyes/tentacles
    │   ├── dread_variant2.png         # Form 2 (more tentacles)
    │   ├── dread_variant2_glowmask.png
    │   ├── dread_variant3.png         # Form 3 (most distorted)
    │   └── dread_variant3_glowmask.png
    └── sounds.json                     # Sound event registration (future phase)
```

### Pattern 1: GeckoLib Entity Implementation (Four-Part System)
**What:** GeckoLib entities require four coordinated classes: Entity (logic), Model (structure), Renderer (display), and Animation Controller (state management).

**When to use:** Any entity requiring complex keyframe animations, concurrent animation playback, or animated model parts.

**Example:**
```java
// 1. ENTITY CLASS (src/main/java, common side)
public class DreadEntity extends PathAwareEntity implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int currentFormVariant = 0; // 0=base, 1=more tentacles, 2=most distorted

    public DreadEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));

        // Head tracking controller (separate from body movement)
        controllers.add(new AnimationController<>(this, "head_controller", 0, state -> {
            return state.setAndContinue(RawAnimation.begin().thenLoop("head_track"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // NBT persistence for form variant
    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("FormVariant", this.currentFormVariant);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.currentFormVariant = nbt.getInt("FormVariant");
    }

    public int getFormVariant() {
        return this.currentFormVariant;
    }

    public void setFormVariant(int variant) {
        this.currentFormVariant = variant;
    }
}

// 2. MODEL CLASS (src/client/java, CLIENT ONLY)
public class DreadEntityModel extends GeoModel<DreadEntity> {
    @Override
    public Identifier getModelResource(DreadEntity entity) {
        return Identifier.of("dread", "geo/dread_entity.geo.json");
    }

    @Override
    public Identifier getTextureResource(DreadEntity entity) {
        // Texture variant selection based on form
        return switch (entity.getFormVariant()) {
            case 1 -> Identifier.of("dread", "textures/entity/dread_variant2.png");
            case 2 -> Identifier.of("dread", "textures/entity/dread_variant3.png");
            default -> Identifier.of("dread", "textures/entity/dread_base.png");
        };
    }

    @Override
    public Identifier getAnimationResource(DreadEntity entity) {
        return Identifier.of("dread", "animations/dread_entity.animation.json");
    }
}

// 3. RENDERER CLASS (src/client/java, CLIENT ONLY)
public class DreadEntityRenderer extends GeoEntityRenderer<DreadEntity> {
    public DreadEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new DreadEntityModel());

        // Add emissive layer for glowing eyes/tentacles
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}

// 4. REGISTRATION (common side)
public class ModEntities {
    public static final EntityType<DreadEntity> DREAD = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("dread", "dread_entity"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DreadEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 2.2f)) // 2.2 blocks tall
            .trackingRange(128) // High range for stalking behavior
            .trackingTickInterval(2)
            .forceTrackedVelocityUpdates(true)
            .build()
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(DREAD, DreadEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 80.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 10.0)
        );
    }
}

// 5. CLIENT REGISTRATION (src/client/java)
public class DreadClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.DREAD, DreadEntityRenderer::new);
    }
}
```

**Source:** [GeckoLib Entities (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-(Geckolib4))

### Pattern 2: Emissive Textures for Glowing Eyes
**What:** GeckoLib's AutoGlowingGeoLayer enables fullbright rendering for specific texture parts (eyes, tentacles) using a separate glowmask texture.

**When to use:** Any entity requiring glowing eyes, emissive patterns, or bioluminescent effects.

**Example:**
```java
// In renderer constructor
public DreadEntityRenderer(EntityRendererProvider.Context context) {
    super(context, new DreadEntityModel());
    this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
}

// Texture file structure:
// - dread_base.png (full texture with eyes/tentacles)
// - dread_base_glowmask.png (ONLY the pixels that should glow, rest transparent)
```

**Process:**
1. Create base texture with all entity details
2. Duplicate texture, rename with `_glowmask.png` suffix
3. In glowmask, delete all pixels except those that should emit light
4. Add AutoGlowingGeoLayer to renderer
5. GeckoLib automatically renders glowmask at full brightness

**Source:** [Emissive Textures (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Emissive-Textures-(Geckolib4))

### Pattern 3: Texture Variants via Renderer Override
**What:** Override getTextureResource() in GeoModel to dynamically select texture based on entity NBT data or world state.

**When to use:** Multiple entity forms/variants that share same model structure but differ in appearance.

**Example:**
```java
@Override
public Identifier getTextureResource(DreadEntity entity) {
    // Progress-based selection (example: based on world day count)
    int worldDay = (int) (entity.getWorld().getTimeOfDay() / 24000L);

    if (worldDay >= 8) {
        return Identifier.of("dread", "textures/entity/dread_variant3.png");
    } else if (worldDay >= 4) {
        return Identifier.of("dread", "textures/entity/dread_variant2.png");
    } else {
        return Identifier.of("dread", "textures/entity/dread_base.png");
    }
}
```

**Alternative:** Store variant in entity NBT and read in getTextureResource() for per-entity variants.

**Source:** Community pattern, verified via [MCreator texture variant discussions](https://mcreator.net/forum/95047/texture-variants-geckolib-entity)

### Pattern 4: Light Extinguishing Mechanic
**What:** Detect nearby torch blocks and remove them programmatically when entity spawns or moves through area.

**When to use:** Environmental manipulation to create darkness/atmosphere around entity.

**Example:**
```java
public class DreadEntity extends PathAwareEntity implements GeoEntity {
    private static final int EXTINGUISH_RANGE = 8; // blocks
    private int extinguishCooldown = 0;

    @Override
    public void tick() {
        super.tick();

        // Only server-side
        if (!this.getWorld().isClient && extinguishCooldown <= 0) {
            extinguishNearbyTorches();
            extinguishCooldown = 40; // Every 2 seconds
        } else {
            extinguishCooldown--;
        }
    }

    private void extinguishNearbyTorches() {
        BlockPos entityPos = this.getBlockPos();
        List<BlockPos> torchPositions = new ArrayList<>();

        // Scan area for torches
        for (BlockPos pos : BlockPos.iterateOutwards(entityPos, EXTINGUISH_RANGE, EXTINGUISH_RANGE, EXTINGUISH_RANGE)) {
            BlockState state = this.getWorld().getBlockState(pos);
            if (state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)) {
                torchPositions.add(pos.toImmutable());
            }
        }

        // Randomly extinguish one torch per tick (gradual effect)
        if (!torchPositions.isEmpty()) {
            BlockPos targetTorch = torchPositions.get(this.random.nextInt(torchPositions.size()));

            // Spawn particle effect before extinguishing
            this.getWorld().addParticle(
                ParticleTypes.SMOKE,
                targetTorch.getX() + 0.5,
                targetTorch.getY() + 0.5,
                targetTorch.getZ() + 0.5,
                0.0, 0.05, 0.0
            );

            // Remove torch (set to air)
            this.getWorld().setBlockState(targetTorch, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
}
```

**Source:** [Fabric Block States Documentation](https://docs.fabricmc.net/1.21.4/develop/blocks/blockstates)

### Pattern 5: Entity Memory Leak Prevention
**What:** Override remove() to manually clear entity brain and pathfinding data, preventing memory accumulation bug MC-260605.

**When to use:** ALL custom entities, especially those that spawn/despawn frequently.

**Example:**
```java
@Override
public void remove(RemovalReason reason) {
    // CRITICAL: Clear entity brain to prevent memory leak
    if (this.getBrain() != null) {
        this.getBrain().clear();
    }
    super.remove(reason);
}
```

**Source:** [MemoryLeakFix mod documentation](https://github.com/FxMorin/MemoryLeakFix)

### Anti-Patterns to Avoid
- **Client-only code in common classes:** Importing MinecraftClient or client-side renderers in entity class causes ClassNotFoundException on dedicated servers. Always use @Environment(EnvType.CLIENT) or separate source sets.
- **Forgetting super.writeNbt()/readNbt():** Custom NBT data saves but position/rotation lost, causing entity to teleport to 0,0,0 on reload.
- **Tracking range too small:** Default 64 blocks causes stalking entity to despawn when player backs away. Horror entities need 128+ range.
- **Complex models without performance testing:** 50+ bone models with multiple entities cause FPS drops. Keep under 30 bones for performance.
- **Not calling markDirty() after NBT changes:** Entity variant data vanishes on server restart.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Entity animations | Custom render logic with manual rotation/translation | GeckoLib | GeckoLib provides keyframe system, easing functions, concurrent animations, sound/particle sync. Building from scratch requires reimplementing Blockbench export format, animation controllers, state machines - hundreds of hours |
| Emissive textures | Custom shader or render layer | AutoGlowingGeoLayer | GeckoLib's built-in layer handles fullbright rendering, shader compatibility (Iris/Optifine), and glowmask texture loading. Custom shaders break with shader mods |
| Entity variants | Multiple EntityType registrations | Single EntityType with texture selection | Registering separate entity types bloats registry, complicates AI code sharing, breaks persistence when switching variants. Texture override is standard pattern |
| Head tracking player | Manual rotation math in tick() | LookAtEntityGoal or animation controller | Goal system handles edge cases (rotation limits, smooth interpolation, obstruction detection). Manual math causes head snapping, 180° flips, gimbal lock |
| Particle spawn on despawn | Manual particle spawning logic | GeckoLib event keyframes or onRemove() with ParticleTypes | GeckoLib can trigger particles via animation keyframes. Manual systems miss edge cases (chunk unload, /kill command, dimension change) |

**Key insight:** GeckoLib is a mature animation engine with 5+ years development and thousands of production mods. Complex animation features (concurrent playback, molang expressions, event keyframes) are non-trivial to implement. Use the library.

## Common Pitfalls

### Pitfall 1: Client-Server Side Confusion
**What goes wrong:** Entity class imports client-only classes like MinecraftClient, EntityRendererRegistry, or rendering utilities, causing `ClassNotFoundException` crashes on dedicated servers.

**Why it happens:** Developers test in singleplayer (integrated server) where client classes exist. Dedicated servers don't have client JARs.

**How to avoid:**
- Entity, model registration, AI logic → common source set (src/main/java)
- Renderer, model classes, client registration → client source set (src/client/java)
- Use @Environment(EnvType.CLIENT) only as last resort, prefer source set separation
- Test with dedicated server before release

**Warning signs:**
- Works in singleplayer, crashes in multiplayer
- Log shows "ClassNotFoundException: net.minecraft.client.*"

**Phase 1 impact:** CRITICAL - Incorrect separation requires refactoring entire entity structure.

### Pitfall 2: Entity Memory Leak (MC-260605)
**What goes wrong:** Entity brain/pathfinding memories not cleared on death/despawn, causing unbounded memory growth until OutOfMemoryError crash.

**Why it happens:** Minecraft bug MC-260605 - PathAwareEntity brain memories aren't automatically garbage collected. Affects all custom entities extending PathAwareEntity.

**How to avoid:**
```java
@Override
public void remove(RemovalReason reason) {
    if (this.getBrain() != null) {
        this.getBrain().clear(); // MANDATORY
    }
    super.remove(reason);
}
```

**Warning signs:**
- Server memory usage grows over hours
- TPS degrades over time, improves after restart
- Profiler shows entity references not being GC'd

**Phase 1 impact:** HIGH - Memory leaks are hard to debug retroactively. Implement from day one.

### Pitfall 3: Forgetting super.writeNbt() / super.readNbt()
**What goes wrong:** Entity saves custom data (form variant) but loses position, UUID, or health on chunk reload, teleporting to 0,0,0.

**Why it happens:** Overriding writeNbt/readNbt without calling super methods loses vanilla entity data.

**How to avoid:**
```java
@Override
public void writeCustomDataToNbt(NbtCompound nbt) {
    super.writeCustomDataToNbt(nbt); // ALWAYS FIRST
    nbt.putInt("FormVariant", this.currentFormVariant);
}

@Override
public void readCustomDataFromNbt(NbtCompound nbt) {
    super.readCustomDataFromNbt(nbt); // ALWAYS FIRST
    this.currentFormVariant = nbt.getInt("FormVariant");
}
```

**Warning signs:**
- Entity position resets on world reload
- Custom data persists but entity fundamentally broken

**Phase 1 impact:** MEDIUM - Caught quickly in testing, easy fix.

### Pitfall 4: Tracking Range Too Small
**What goes wrong:** Horror entity despawns when player turns around or backs away, breaking tension. Entity vanishes mid-chase.

**Why it happens:** Default tracking range is 64 blocks. When player moves beyond 64 blocks, server stops tracking entity to client, causing client to unload it.

**How to avoid:**
```java
FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DreadEntity::new)
    .trackingRange(128) // 2x default for stalking behavior
    .trackingTickInterval(2) // Sync frequency (lower = more accurate, higher = less network load)
    .forceTrackedVelocityUpdates(true) // Always sync movement
    .build()
```

**Warning signs:**
- Entity disappears when player backs away
- Multiplayer: entity visible to host, invisible to other players

**Phase 1 impact:** HIGH - Set during registration. Changing later requires entity re-registration and world reset.

### Pitfall 5: GeckoLib Animation Performance Drain
**What goes wrong:** Complex model with 50+ bones causes FPS drops, especially with multiple entities or during cinematic sequences.

**Why it happens:** Each animated bone requires matrix calculations per frame. Horror mod atmosphere tempts complex models, but performance suffers.

**How to avoid:**
- Keep bone count under 30 for performance
- Use texture detail instead of geometry where possible (painted tentacles vs modeled tentacles)
- Test with multiple entities spawned simultaneously
- Profile frame time with F3 (renderEntity should stay under 5ms)

**Warning signs:**
- FPS drops when looking at entity
- F3 profiler shows high time in "renderEntity"
- Players with low-end PCs report lag

**Phase 1 impact:** HIGH - Model bone count set during creation. Reducing bones later requires full model rebuild in Blockbench.

### Pitfall 6: Transparency Rendering Z-Fighting
**What goes wrong:** Transparent model parts (glowing eyes, ethereal effects) flicker or render incorrectly, especially with shaders (Iris/Optifine).

**Why it happens:** Multiple transparent layers at same depth cause z-fighting. Shader mods change rendering pipeline, breaking transparency sorting.

**How to avoid:**
- Test with Iris/Optifine shaders during development
- Use RenderLayer.getEntityTranslucentCull() for transparent parts
- Slightly offset transparent layers in Blockbench model (0.01 blocks) to prevent z-fighting
- For emissive textures, use AutoGlowingGeoLayer instead of transparent overlays

**Warning signs:**
- Entity looks correct without shaders, breaks with shaders
- Flickering textures when entity rotates
- GitHub issues from players using shader packs

**Phase 1 impact:** MEDIUM - Rendering issues damage horror atmosphere. Test with shaders from start.

### Pitfall 7: Not Marking NBT Dirty
**What goes wrong:** Entity form variant resets to default after server restart. Player sees Dread as base form despite world day count triggering variant 3.

**Why it happens:** Minecraft only saves data marked as dirty. Changing NBT field without calling markDirty() means it's never written to disk.

**How to avoid:**
```java
public void setFormVariant(int variant) {
    this.currentFormVariant = variant;
    // If using PersistentState, call markDirty()
    // For entity NBT, data is auto-marked when writeCustomDataToNbt called
}
```

**Warning signs:**
- Data works until server restart, then resets
- Players report "losing progress" after crashes

**Phase 1 impact:** MEDIUM - Form variant is core visual feature. Test with server restart immediately after variant change.

## Code Examples

Verified patterns from official sources:

### Blockbench Model Creation
```
1. Install GeckoLib Animation Utils plugin (Blockbench plugin browser)
2. File → New → GeckoLib Animated Model
3. Create model structure:
   - Root group: "dread" (pivot at 0,0,0)
   - Child groups: "head", "body", "left_arm", "right_arm", "left_leg", "right_leg", "tentacles"
   - IMPORTANT: Only groups can be animated, cubes cannot
4. Create animations:
   - "idle": Subtle twitching, random head tilts (loop)
   - "walk": Jerky movement with stutter keyframes (loop)
   - "attack": Lunge forward with tentacle lash
   - "head_track": Head rotation separate from body (loop)
   - "spawn": Materialization effect
   - "despawn": Fade/dissolve effect
5. Export:
   - File → Export → GeckoLib Model (saves to geo/dread_entity.geo.json)
   - File → Export → GeckoLib Animations (saves to animations/dread_entity.animation.json)
   - Right-click texture → Save As (saves to textures/entity/dread_base.png)
```

**Source:** [Making Your Models (Blockbench)](https://github.com/bernie-g/geckolib/wiki/Making-Your-Models-(Blockbench))

### Animation Controller for State-Based Playback
```java
@Override
public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    // Main body controller
    controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
        // 5 tick transition time for smooth blending

        if (this.isDead()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("despawn"));
        }

        if (this.isAttacking()) {
            return state.setAndContinue(RawAnimation.begin().thenPlay("attack"));
        }

        if (state.isMoving()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
        }

        return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
    }));

    // Separate head tracking controller (runs concurrently with main)
    controllers.add(new AnimationController<>(this, "head_controller", 0, state -> {
        return state.setAndContinue(RawAnimation.begin().thenLoop("head_track"));
    }));
}
```

**Source:** [GeckoLib Entities (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-(Geckolib4))

### Custom Particle Spawn on Despawn
```java
@Override
public void remove(RemovalReason reason) {
    // Spawn particles before removal (client-side)
    if (this.getWorld().isClient && reason == RemovalReason.KILLED) {
        for (int i = 0; i < 20; i++) {
            this.getWorld().addParticle(
                ParticleTypes.LARGE_SMOKE,
                this.getX() + (this.random.nextDouble() - 0.5) * 2,
                this.getY() + this.random.nextDouble() * 2,
                this.getZ() + (this.random.nextDouble() - 0.5) * 2,
                0.0, 0.05, 0.0
            );
        }
    }

    // Clear brain to prevent memory leak
    if (this.getBrain() != null) {
        this.getBrain().clear();
    }

    super.remove(reason);
}
```

**Source:** [Fabric Particles Documentation](https://docs.fabricmc.net/develop/rendering/particles/creating-particles)

### Spawn Animation Trigger
```java
public class DreadEntity extends PathAwareEntity implements GeoEntity {
    private boolean hasPlayedSpawnAnimation = false;

    @Override
    public void tick() {
        super.tick();

        // Play spawn animation once on first tick
        if (!hasPlayedSpawnAnimation && this.age == 1) {
            hasPlayedSpawnAnimation = true;
            // Animation controller will pick up this state
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "spawn_controller", 0, state -> {
            if (!hasPlayedSpawnAnimation) {
                return state.setAndContinue(RawAnimation.begin().thenPlay("spawn"));
            }
            // After spawn animation, return null to let main controller take over
            return PlayState.STOP;
        }));
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Vanilla entity models with manual rotation | GeckoLib keyframe animations | 2019-2020 (GeckoLib 1.0) | Complex animations (concurrent, easing, events) now standard. Vanilla approach insufficient for horror creature with multiple animation states |
| Manual render layers for emissive | AutoGlowingGeoLayer | GeckoLib 4.0 (2023) | One-line setup vs custom shader code. Automatic glowmask texture loading and fullbright rendering |
| NBT for all persistent data | Data Components for items (1.20.5+) | Minecraft 1.20.5 (2024) | Entities still use NBT. Items use data components. Don't mix systems |
| HudRenderCallback for rendering | HudElementRegistry | Fabric API 0.116 (2024) | Deprecated callback replaced with registry system. Use HudElementRegistry for future-proofing |
| Manual packet system | Payload-based networking (CustomPacketPayload) | Minecraft 1.20.5 (2024) | Type-safe, structured packets. Old packet system removed |

**Deprecated/outdated:**
- **Vanilla animations for complex entities:** Minecraft 1.20.4 added basic animation system, but lacks concurrent playback, sound/particle keyframes, and event system GeckoLib provides. Not suitable for multi-state horror creature
- **HudRenderCallback:** Deprecated since Fabric API 0.116. Use HudElementRegistry (future phase)
- **Legacy packet system:** Removed in 1.20.5. Use CustomPacketPayload records

## Open Questions

Things that couldn't be fully resolved:

1. **Optimal torch extinguishing range**
   - What we know: Needs to create darkness without destroying player's base lighting. Community mods suggest 8-16 block range
   - What's unclear: Exact range that balances gameplay vs atmosphere for Dread's horror aesthetic
   - Recommendation: Start with 8 blocks, make configurable, gather playtester feedback

2. **Torch restoration timing**
   - What we know: Options are (a) never restore, (b) restore after Dread leaves area, (c) restore on Dread death
   - What's unclear: Which approach best serves horror gameplay loop without frustrating players
   - Recommendation: Start with no restoration (permanent), add config option, test player reaction. Restoring on Dread death preserves consequence while not permanently breaking bases

3. **Form variant progression trigger**
   - What we know: CONTEXT.md specifies "progress-based selection" with "scarier variants as game days increase"
   - What's unclear: Should variant be per-entity (stored in NBT) or global (all Dreads same variant based on world day)?
   - Recommendation: Per-entity variant (NBT-based) allows mixed forms for variety. Set variant on spawn based on world day, persist in entity data

4. **Animation timing for jerky movement**
   - What we know: Jerky movement requires intentional stutter keyframes in Blockbench. "Wrong" movement crucial for horror
   - What's unclear: Exact keyframe spacing to achieve "unsettling" without looking broken/buggy
   - Recommendation: Reference horror game animations (The Rake, SCP:CB creatures). Test with players unfamiliar with mod - ask "does it look broken or intentionally creepy?"

5. **Head tracking implementation**
   - What we know: GeckoLib supports concurrent animations. Head can track player while body plays walk/idle animation
   - What's unclear: Whether to use separate animation controller or integrate into Blockbench animations. Performance impact of constant head rotation calculations
   - Recommendation: Use separate AnimationController for head tracking (cleaner separation). Calculate target rotation in tick(), apply in animation controller. Limit update frequency to every 5 ticks for performance

## Sources

### Primary (HIGH confidence)
- [GeckoLib Official GitHub](https://github.com/bernie-g/geckolib) - Animation engine documentation
- [GeckoLib Entities (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-(Geckolib4)) - Entity implementation guide
- [Emissive Textures (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Emissive-Textures-(Geckolib4)) - Glowing texture implementation
- [Making Your Models (Blockbench)](https://github.com/bernie-g/geckolib/wiki/Making-Your-Models-(Blockbench)) - Model creation workflow
- [Fabric Wiki: Creating an Entity](https://wiki.fabricmc.net/tutorial:entity) - Entity registration patterns
- [Fabric Documentation: Block States](https://docs.fabricmc.net/1.21.4/develop/blocks/blockstates) - Block manipulation for torch extinguishing
- [Fabric Documentation: Creating Particles](https://docs.fabricmc.net/develop/rendering/particles/creating-particles) - Particle spawn mechanics

### Secondary (MEDIUM confidence)
- [GeckoLib on CurseForge](https://www.curseforge.com/minecraft/mc-mods/geckolib) - Version 4.8.2 for Fabric 1.21.1 verified
- [GeckoLib on Modrinth](https://modrinth.com/mod/geckolib/versions) - Latest Fabric 5.4.2 for 1.21.11 (Jan 11, 2026)
- [MCreator: Texture Variants with GeckoLib Entity](https://mcreator.net/forum/95047/texture-variants-geckolib-entity) - Texture variant selection patterns
- [MemoryLeakFix GitHub](https://github.com/FxMorin/MemoryLeakFix) - Entity memory leak documentation
- [MCreator: GeckoLib Entity Server Lag](https://mcreator.net/forum/111236/geckolib-entity-causes-server-lag) - Performance concerns with complex models

### Tertiary (LOW confidence)
- Community discussions on head tracking and AI goals - pattern exists but no official documentation
- Torch extinguishing mechanics - inferred from block manipulation docs, no official "light extinguishing" pattern
- Form variant progression - community pattern, not standardized

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - GeckoLib version verified, Fabric entity API documented
- Architecture: HIGH - GeckoLib 4-part system from official wiki, Fabric patterns from official docs
- Emissive textures: HIGH - Official GeckoLib documentation with code examples
- Entity registration: HIGH - Official Fabric wiki with examples
- Texture variants: MEDIUM - Community pattern, no official GeckoLib variant system
- Torch extinguishing: MEDIUM - Block manipulation API verified, gameplay balance unclear
- Performance: MEDIUM - Community reports and general guidelines, no official bone count limits
- Pitfalls: HIGH - Memory leak from documented bug MC-260605, client/server from Fabric wiki

**Research date:** 2026-01-23
**Valid until:** 30 days (stable APIs, GeckoLib mature library)

**Note on future compatibility:** Minecraft 1.21.11 is the last Yarn-mapped version. GeckoLib 5.x released for 1.21.11 (Fabric 5.4.2). Next Minecraft version (26.1) will be unobfuscated and require migration to Mojang mappings. This research applies to 1.21.x series.
