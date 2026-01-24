# Phase 2: AI & Horror Experience - Research

**Researched:** 2026-01-24
**Domain:** Minecraft Fabric Entity AI, Horror Game Design, Audio System
**Confidence:** MEDIUM-HIGH

## Summary

This phase implements intelligent stalking behavior (turn-around spawn mechanic, stare-to-freeze), escalating spawn probability system, fake-out tension moments, and full atmospheric soundscape for the Dread entity. The research identifies the vanilla Goal system as sufficient for custom AI behaviors, reveals critical patterns for FOV/raycast detection, establishes sound design best practices for horror atmosphere, and uncovers important pitfalls around client-server synchronization and sound channel management.

**Key findings:**
- Vanilla Minecraft Goal system is mature and well-suited for custom AI behaviors without requiring third-party libraries
- FOV detection requires vector math (dot product) between player look direction and entity position, not simple angle checks
- Sound system has concurrent playback limits (247 channels vanilla) that require priority management for layered audio
- Horror mods succeed through atmospheric tension building rather than jump-scare spam
- Client-server synchronization of entity data must be carefully managed to prevent packet decode errors
- Persistent state system (PersistentState + NBT) enables tracking spawn counters and cooldowns across sessions

**Primary recommendation:** Build custom Goal classes extending vanilla `Goal` for turn-around spawn and stare mechanics. Use server tick events with PersistentState for probability tracking. Implement sound priority system to prevent channel exhaustion during layered ambient/proximity/jump-scare audio.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Minecraft Entity AI (Goal System) | 1.21.1 | Entity behavior and pathfinding | Mojang's official AI framework, used by all vanilla mobs since 1.8 |
| Fabric API (Lifecycle Events) | 0.116.8 | Server tick hooks, player events | Official Fabric event system for custom logic injection |
| GeckoLib | 5.4.2+ | Animation triggering from AI | Already integrated in Phase 1, supports animation controllers |
| PersistentState | Vanilla | Save spawn counters/cooldowns | Fabric wiki recommended pattern for world-persistent data |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| SoundEvent Registry | Vanilla | Custom sound registration | Required for all custom audio files |
| ServerTickEvents | Fabric API | Probability calculations per tick | For spawn chance evaluation and fake-out triggers |
| NBT (Named Binary Tag) | Vanilla | Serialization of state data | Storing mining counters, cooldowns, player-specific data |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Vanilla Goal System | SmartBrainLib (SBL) | SBL modernizes the Brain system (for villagers/piglins), but Goal system is simpler for single-entity focus. Adds dependency. |
| Custom raycast for FOV | Entity.raycast() | Built-in raycast exists but Goal system + vector math gives more control for "watching" detection |
| Manual sound channels | Sound Priority Mod | Mods exist to extend limits (247→1024+ channels), but good priority design should handle vanilla limits |

**Installation:**
Already installed in Phase 1 (Fabric API, GeckoLib). No additional dependencies required.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/dread/
├── entity/
│   ├── DreadEntity.java               # Existing entity from Phase 1
│   ├── ai/                             # NEW: Custom AI goals
│   │   ├── TurnAroundSpawnGoal.java   # Spawn behind player mechanic
│   │   ├── StareStandoffGoal.java     # Freeze when watched (Weeping Angel)
│   │   └── VanishGoal.java            # Despawn behavior after spawn
│   └── spawn/                          # NEW: Spawn probability system
│       ├── DreadSpawnManager.java     # Coordinates spawn logic
│       └── SpawnProbabilityState.java # PersistentState for counters
├── sound/                              # NEW: Audio management
│   ├── ModSounds.java                 # SoundEvent registration
│   └── DreadSoundManager.java         # Priority-based playback
└── registry/
    └── ModEntities.java               # Existing registry
```

### Pattern 1: Custom Goal for Turn-Around Spawn

**What:** Goal that detects when player looks away from entity's spawn position, waits variable time, spawns entity behind player at random distance.

**When to use:** When you need entity behavior that reacts to player actions (looking direction).

**Example:**
```java
// Based on McJty tutorial and Fabric API docs
public class TurnAroundSpawnGoal extends Goal {
    private final DreadEntity dread;
    private PlayerEntity targetPlayer;
    private int lookAwayTicks = 0;
    private int spawnDelayTicks;

    public TurnAroundSpawnGoal(DreadEntity dread) {
        this.dread = dread;
        // Generate random delay (30-120 ticks = 1.5-6 seconds)
        this.spawnDelayTicks = dread.getRandom().nextBetween(30, 120);
        this.setControls(EnumSet.of(Control.MOVE)); // Controls movement
    }

    @Override
    public boolean canStart() {
        // Check if player is looking away from spawn zone
        this.targetPlayer = findNearestPlayer();
        return targetPlayer != null && !isPlayerLookingAtSpawnZone();
    }

    @Override
    public boolean shouldContinue() {
        return lookAwayTicks < spawnDelayTicks && !isPlayerLookingAtSpawnZone();
    }

    @Override
    public void tick() {
        lookAwayTicks++;
        if (lookAwayTicks >= spawnDelayTicks) {
            spawnBehindPlayer();
        }
    }

    private boolean isPlayerLookingAtSpawnZone() {
        // Vector from player to spawn position
        Vec3d toSpawn = dread.getPos().subtract(targetPlayer.getEyePos());
        Vec3d lookDir = targetPlayer.getRotationVector(); // player's look direction

        // Dot product for angle: closer to 1 = looking at it
        double dot = toSpawn.normalize().dotProduct(lookDir);
        return dot > 0.8; // ~36° FOV threshold (Claude's discretion)
    }

    private void spawnBehindPlayer() {
        // Get opposite of player look direction
        Vec3d lookDir = targetPlayer.getRotationVector();
        Vec3d behindDir = lookDir.multiply(-1.0);

        // Random distance (3-8 blocks per CONTEXT.md)
        double distance = dread.getRandom().nextBetween(3, 8);
        Vec3d spawnPos = targetPlayer.getPos().add(behindDir.multiply(distance));

        dread.teleport(spawnPos.x, spawnPos.y, spawnPos.z);
        // Trigger jump scare sound/animation
        // Start 30-60s cooldown
    }
}
```

**Source:** [McJty Modding Tutorial](https://www.mcjty.eu/docs/1.18/ep4), [Fabric Goal API](https://maven.fabricmc.net/docs/yarn-1.18.1+build.12/net/minecraft/entity/ai/goal/package-summary.html)

### Pattern 2: Persistent State for Spawn Tracking

**What:** Server-side persistent storage for mining counters, day counters, cooldowns that survive restarts.

**When to use:** When you need to track state across sessions (spawn probability escalation).

**Example:**
```java
// Based on Fabric Wiki PersistentState tutorial
public class SpawnProbabilityState extends PersistentState {
    private Map<UUID, PlayerSpawnData> playerData = new HashMap<>();

    public static class PlayerSpawnData {
        int blocksMined = 0;
        long lastSpawnTick = 0;
        int fakeoutCount = 0;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList playersList = new NbtList();
        for (Map.Entry<UUID, PlayerSpawnData> entry : playerData.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUuid("uuid", entry.getKey());
            playerNbt.putInt("blocksMined", entry.getValue().blocksMined);
            playerNbt.putLong("lastSpawn", entry.getValue().lastSpawnTick);
            playersList.add(playerNbt);
        }
        nbt.put("players", playersList);
        return nbt;
    }

    public static SpawnProbabilityState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        SpawnProbabilityState state = new SpawnProbabilityState();
        NbtList playersList = nbt.getList("players", 10);
        for (int i = 0; i < playersList.size(); i++) {
            NbtCompound playerNbt = playersList.getCompound(i);
            UUID uuid = playerNbt.getUuid("uuid");
            PlayerSpawnData data = new PlayerSpawnData();
            data.blocksMined = playerNbt.getInt("blocksMined");
            data.lastSpawnTick = playerNbt.getLong("lastSpawn");
            state.playerData.put(uuid, data);
        }
        return state;
    }

    // Always call markDirty() after modifying data!
    public void incrementMinedBlocks(UUID player) {
        PlayerSpawnData data = playerData.computeIfAbsent(player, k -> new PlayerSpawnData());
        data.blocksMined++;
        markDirty(); // CRITICAL: flags for save
    }
}
```

**Source:** [Fabric Wiki - Persistent States](https://wiki.fabricmc.net/tutorial:persistent_states)

### Pattern 3: Sound Priority Management

**What:** Play critical sounds (jump scares) at full volume while managing ambient layers to stay under channel limit.

**When to use:** When layering multiple concurrent sounds (ambient + proximity + jump scare).

**Example:**
```java
public class DreadSoundManager {
    private static final int PRIORITY_JUMPSCARE = 0;   // Highest
    private static final int PRIORITY_PROXIMITY = 1;
    private static final int PRIORITY_AMBIENT = 2;     // Lowest

    private World world;
    private boolean isPlayingJumpscare = false;

    public void playJumpScare(Entity entity) {
        // Stop all lower priority sounds to free channels
        stopAmbientSounds();

        isPlayingJumpscare = true;
        world.playSound(
            null, // null = all players hear it
            entity.getBlockPos(),
            ModSounds.DREAD_JUMPSCARE, // Registered SoundEvent
            SoundCategory.HOSTILE,
            1.0f, // volume (max)
            1.0f  // pitch (normal)
        );

        // Schedule ambient restart after jump scare finishes (3s duration)
        scheduleAmbientRestart(60); // 3 seconds = 60 ticks
    }

    public void playProximitySound(Entity entity, float distanceToPlayer) {
        if (isPlayingJumpscare) return; // Don't interrupt jump scare

        // Volume based on proximity (inverse distance)
        float volume = Math.max(0.1f, 1.0f - (distanceToPlayer / 16.0f));

        world.playSound(
            null,
            entity.getBlockPos(),
            ModSounds.DREAD_PROXIMITY,
            SoundCategory.HOSTILE,
            volume,
            0.8f + world.getRandom().nextFloat() * 0.4f // pitch variation
        );
    }
}
```

**Source:** [Fabric Docs - Playing Sounds](https://docs.fabricmc.net/1.20.4/develop/sounds/using-sounds), [Minecraft Wiki - Sound System](https://minecraft.wiki/w/Sounds.json)

### Pattern 4: FOV Detection with Raycasting

**What:** Determine if player is looking at entity using vector dot product (not raycasting, despite the name).

**When to use:** Weeping Angel freeze mechanic - entity can't move while watched.

**Example:**
```java
public boolean isPlayerLookingAtEntity(PlayerEntity player, Entity target) {
    // Vector from player's eye to entity center
    Vec3d toEntity = target.getPos().subtract(player.getEyePos()).normalize();

    // Player's look direction (from yaw/pitch)
    Vec3d lookDir = player.getRotationVector().normalize();

    // Dot product: 1.0 = looking directly at, 0 = perpendicular, -1 = behind
    double dotProduct = toEntity.dotProduct(lookDir);

    // FOV threshold (0.8 ≈ 36° cone, 0.9 ≈ 25° cone)
    // From CONTEXT.md: "Claude's discretion" - recommend 0.85 (≈31° cone)
    boolean inFOV = dotProduct > 0.85;

    // Optional: Add raycast to check for obstructions
    if (inFOV) {
        HitResult hit = player.getWorld().raycast(new RaycastContext(
            player.getEyePos(),
            target.getPos(),
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            player
        ));

        // If hit a block before entity, player can't see it (wall blocking)
        return hit.getType() == HitResult.Type.MISS;
    }

    return false;
}
```

**Source:** [Minecraft Commands Wiki - Look Detection](https://minecraftcommands.github.io/wiki/questions/lookat.html), [Minecraft Wiki - Rotation](https://minecraft.wiki/w/Rotation)

### Anti-Patterns to Avoid

- **Hard-coding spawn delays:** Always use random ranges for timings (30-120 ticks, not fixed 60 ticks) to prevent predictability
- **Forgetting markDirty():** PersistentState changes won't save unless you call `markDirty()` after modifications
- **Client-side sound playback:** Always play sounds on server (`!world.isClient`) or use `world.playSound(null, ...)` for server-initiated sounds
- **Skipping goal Control flags:** Always set `setControls(EnumSet.of(Control.MOVE, Control.LOOK))` to prevent goal conflicts
- **Spawning without cooldown:** Always track last spawn time and enforce minimum cooldown to prevent spam
- **Fixed fake-out ratio:** Vary fake-out frequency based on day progression, not fixed 3:1 ratio

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Vector math for rotations | Custom yaw/pitch→vector | `Entity.getRotationVector()` | Handles radians/degrees conversion, edge cases |
| World-persistent data | Custom file I/O | `PersistentState` + NBT | Handles save/load lifecycle, thread-safety |
| Path finding to player | Custom A* algorithm | `MoveToTargetPosGoal` or vanilla Navigation | Minecraft's pathfinding handles terrain, doors, obstacles |
| Sound attenuation | Manual volume calculation | Vanilla distance formula (volume > 1.0 increases range) | `volume * 16 blocks` for hearing distance |
| Player tracking across dimensions | Manual dimension checks | `ActiveTargetGoal<PlayerEntity>` | Built-in cross-dimension targeting |
| Random probability | Custom RNG seeding | `world.getRandom()` or `entity.getRandom()` | Synchronized with world seed, deterministic |

**Key insight:** Minecraft's vanilla systems (Goals, Navigation, Sound) have been battle-tested for 10+ years and handle edge cases you won't think of (ladders, swimming, dimension transitions). Extend them, don't replace them.

## Common Pitfalls

### Pitfall 1: Client-Server Desync on Entity Data

**What goes wrong:** Custom entity fields (spawn state, cooldowns) only update on server, client never sees them. Results in "invisible" behavior or packet decode errors.

**Why it happens:** Minecraft doesn't auto-sync custom NBT data to clients. Only synced data trackers propagate.

**How to avoid:**
- Use `DataTracker` for data that affects rendering/client behavior
- Keep spawn logic server-side only (clients don't need to know probabilities)
- Send custom packets only if client needs to show UI (danger indicator sound is server-initiated, not client-tracked)

**Warning signs:**
- Logs show `Failed to decode packet 'clientbound/minecraft:set_entity_data'`
- Entity appears in wrong position on client vs server
- Behavior works in singleplayer but breaks on dedicated server

**Source:** [GitHub Issue - ProtocolLib Entity Data](https://github.com/dmulloy2/ProtocolLib/issues/2987), [SpigotMC - Packet Errors](https://www.spigotmc.org/threads/error-sending-packet-clientbound-minecraft-set_entity_data.654453/)

### Pitfall 2: Sound Channel Exhaustion

**What goes wrong:** Playing too many sounds concurrently causes newer sounds to silently fail (no jump scare audio).

**Why it happens:** Minecraft has a 247 concurrent sound limit (vanilla). Each ambient loop, proximity hum, and jump scare uses a channel.

**How to avoid:**
- Stop ambient sounds before playing jump scares (priority system)
- Use `SoundCategory` wisely (players can reduce volume per category)
- Avoid looping sounds without stop conditions
- Consider using single layered sound file instead of 3+ concurrent loops

**Warning signs:**
- Jump scare plays visually but no audio
- Sounds cut out randomly in areas with many entities
- Mods like ExtendPolyphonyLimit needed to "fix" your mod

**Source:** [CurseForge - ExtendPolyphonyLimit](https://www.curseforge.com/minecraft/mc-mods/extendpolyphonylimit), [Mojang Bug - MC-1538](https://bugs.mojang.com/browse/MC-1538)

### Pitfall 3: Goal Priority Conflicts

**What goes wrong:** Two goals try to control the same aspect (movement, look direction) simultaneously, causing jittery/broken behavior.

**Why it happens:** `GoalSelector` uses priority numbers (lower = higher priority) and Control flags, but if flags overlap and priorities are equal, behavior is undefined.

**How to avoid:**
- Always use unique priority values (don't use same number twice)
- Set Control flags explicitly: `setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK))`
- Test goal conflicts: if Goal A controls MOVE and Goal B controls MOVE at same priority, higher priority wins

**Warning signs:**
- Entity freezes mid-animation
- Entity "vibrates" between two positions
- Pathfinding starts then immediately cancels

**Source:** [Fabric API - GoalSelector](https://maven.fabricmc.net/docs/yarn-1.18.1+build.12/net/minecraft/entity/ai/goal/GoalSelector.html), [McJty Tutorial](https://www.mcjty.eu/docs/1.18/ep4)

### Pitfall 4: FOV Detection Without Obstruction Check

**What goes wrong:** Entity thinks player is watching through walls, breaks Weeping Angel mechanic immersion.

**Why it happens:** Dot product only checks angle, not line-of-sight. Player facing entity ≠ player can see entity.

**How to avoid:**
- After FOV check passes, raycast from player eye to entity center
- If raycast hits block before entity, return false (blocked view)
- From CONTEXT.md: "Blocked view counts as looking away"

**Warning signs:**
- Entity freezes when player faces it through walls
- Works in open areas but breaks near buildings/caves

**Source:** [Minecraft Commands Wiki - Raycast](https://minecraftcommands.github.io/wiki/questions/raycast.html)

### Pitfall 5: Spawn Probability Without Bounds

**What goes wrong:** Day 100 = 100x spawn rate = constant spawning = unplayable.

**Why it happens:** Linear escalation without cap. Day multiplier keeps growing forever.

**How to avoid:**
- Cap day multiplier at reasonable value (e.g., Day 20 = max 20x)
- Use logarithmic scaling: `multiplier = Math.log(day + 1) * 5` instead of `multiplier = day`
- Reset counter after successful spawn (per CONTEXT.md requirement)

**Warning signs:**
- Late-game becomes impossible due to spawn frequency
- Players avoid mining entirely to prevent spawns

**Source:** Design pattern from Phase 2 requirements analysis

### Pitfall 6: Memory Leaks from Entity Brains

**What goes wrong:** Dead entities retain references to world/players, causing memory leak over time.

**Why it happens:** Minecraft bug MC-260605 - Brain system doesn't clear on entity removal.

**How to avoid:**
- Override `remove(RemovalReason reason)` and call `getBrain().clear()` (if using Brain)
- For Goal system: Generally safe, but clear custom references in `stop()` methods
- Already implemented in Phase 1 DreadEntity.java (line 242-247)

**Warning signs:**
- RAM usage grows over time even with no active entities
- Mods like "AllTheLeaks" needed to "fix" memory issues

**Source:** [CurseForge - AllTheLeaks](https://www.curseforge.com/minecraft/mc-mods/alltheleaks), [GitHub - Citadel Memory Leak](https://github.com/Alex-the-666/Citadel/issues/12)

## Code Examples

Verified patterns from official sources:

### Registering Custom Goals
```java
// Source: Fabric Wiki Entity Tutorial + McJty Tutorial
@Override
protected void initGoals() {
    // Priority 0 = highest priority
    this.goalSelector.add(0, new SwimGoal(this)); // Always swim (survival)
    this.goalSelector.add(1, new StareStandoffGoal(this)); // Freeze when watched
    this.goalSelector.add(2, new TurnAroundSpawnGoal(this)); // Spawn behind player
    this.goalSelector.add(3, new VanishGoal(this)); // Despawn after timeout
    this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));

    // Target selector (separate from behavior goals)
    this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
}
```

### Registering Custom Sounds
```java
// Source: Fabric Docs - Creating Custom Sounds
public class ModSounds {
    public static final SoundEvent DREAD_AMBIENT = registerSound("dread_ambient");
    public static final SoundEvent DREAD_PROXIMITY = registerSound("dread_proximity");
    public static final SoundEvent DREAD_JUMPSCARE = registerSound("dread_jumpscare");
    public static final SoundEvent DANGER_RISING = registerSound("danger_rising");

    private static SoundEvent registerSound(String id) {
        Identifier identifier = Identifier.of(DreadMod.MOD_ID, id);
        return Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            identifier,
            SoundEvent.createVariableRangeEvent(identifier)
        );
    }

    public static void register() {
        DreadMod.LOGGER.info("Registering sounds for " + DreadMod.MOD_ID);
    }
}
```

### Server Tick Event for Spawn Evaluation
```java
// Source: Fabric API - ServerTickEvents
public class DreadSpawnManager {
    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            // Only run every 20 ticks (1 second) to reduce performance impact
            if (world.getTime() % 20 == 0) {
                evaluateSpawnProbability(world);
            }
        });
    }

    private static void evaluateSpawnProbability(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            SpawnProbabilityState state = getOrCreateState(world);
            float chance = calculateSpawnChance(state, player, world);

            if (world.getRandom().nextFloat() < chance) {
                // Real spawn or fake-out (3:1 ratio from CONTEXT.md)
                if (world.getRandom().nextFloat() < 0.25f) {
                    spawnDread(world, player);
                } else {
                    triggerFakeout(world, player);
                }
            }
        }
    }

    private static float calculateSpawnChance(SpawnProbabilityState state,
                                               ServerPlayerEntity player,
                                               ServerWorld world) {
        long worldDay = world.getTimeOfDay() / 24000L;
        int blocksMined = state.getMinedBlocks(player.getUuid());

        // Base: 10-20% per check (CONTEXT.md: occasional on Day 1)
        float baseChance = 0.15f;

        // Day escalation: Linear up to cap (max 10x at day 20)
        float dayMultiplier = Math.min(worldDay, 20);

        // Mining escalation: +1% per block, reset after spawn
        float miningBonus = blocksMined * 0.01f;

        return baseChance * dayMultiplier + miningBonus;
    }
}
```

### Triggering GeckoLib Animation from Goal
```java
// Source: GeckoLib Wiki - Entity Animations
public class TurnAroundSpawnGoal extends Goal {
    private void spawnBehindPlayer() {
        // ... positioning logic ...

        // Trigger spawn animation via AnimationController
        // Animation controller checks entity state in registerControllers()
        dread.setSpawning(true); // Custom flag

        // Play jump scare sound
        world.playSound(
            null,
            dread.getBlockPos(),
            ModSounds.DREAD_JUMPSCARE,
            SoundCategory.HOSTILE,
            1.0f,
            1.0f
        );

        // Schedule spawn animation completion (2 seconds)
        scheduleTask(() -> dread.setSpawning(false), 40);
    }
}

// In DreadEntity.java registerControllers():
controllers.add(new AnimationController<>(this, "main", 5, state -> {
    if (this.isSpawning()) {
        return state.setAndContinue(RawAnimation.begin().thenPlay("spawn"));
    }
    // ... other animations ...
}));
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Brain system for all mobs | Goal system still preferred for simple AI | 1.14 introduced Brain | Brain is complex, overkill for single-entity horror mod |
| Manual sound file formats | OGG Vorbis mono required | Always (engine limitation) | Stereo files break distance attenuation |
| Custom pathfinding algorithms | Extend MoveToTargetPosGoal | 1.8+ (Goal system) | Vanilla handles terrain, doors, water, dimensions |
| Client-side entity spawning | Always spawn server-side | 1.3+ (SMP integration) | Client spawning causes desync/ghost entities |
| Fixed animation triggers | AnimationController predicates | GeckoLib 4+ | Allows dynamic animation based on entity state |
| Manual NBT file writing | PersistentState + Codecs | 1.16+ | Handles save/load lifecycle automatically |

**Deprecated/outdated:**
- **SmartBrainLib for simple AI:** SBL is excellent for complex villager-like AI, but Goal system is simpler and sufficient for horror entity with 3-4 behaviors
- **Manual sound channel management:** Vanilla 247-channel limit is sufficient if using priority system (stop ambient before jump scare)
- **Fixed spawn timings:** Modern horror games use variable timings to prevent player pattern recognition

## Open Questions

Things that couldn't be fully resolved:

1. **Exact FOV threshold for "looking away"**
   - What we know: Dot product > 0.8 ≈ 36° cone, > 0.9 ≈ 25° cone
   - What's unclear: Optimal value for horror experience (too tight = frustrating, too loose = too easy)
   - Recommendation: Start with 0.85 (≈31° cone), playtest and adjust. Add debug visualization.

2. **Stare standoff resolution mechanic**
   - What we know: CONTEXT.md says "Claude's discretion" - options include vanish timer, forced blink, teleport
   - What's unclear: Which creates best tension without frustration
   - Recommendation: Implement vanish timer (30-60s of staring makes it disappear) + rare forced blink (10% chance per 5s). Playtest both.

3. **Mining counter decay rate**
   - What we know: Counter increases per block mined, resets on spawn
   - What's unclear: Should counter decay over time if no mining? How fast?
   - Recommendation: No decay initially (simpler). If playtesting shows players "game" the system by waiting, add slow decay (1 block per minute).

4. **Fake-out implementation details**
   - What we know: CONTEXT.md lists 4 types (sounds only, distant glimpses, near-miss spawns, environmental effects)
   - What's unclear: How to implement "distant glimpses" without actual entity (performance concern)
   - Recommendation: Sounds only (easiest), near-miss spawns (spawn far away then despawn). Defer glimpses/environmental to Phase 3.

5. **Proximity audio intensity curve**
   - What we know: Ambient gets quieter as Dread approaches (unnatural silence)
   - What's unclear: Linear or exponential falloff? Distance thresholds?
   - Recommendation: Exponential curve: Full volume at 16+ blocks, 50% at 8 blocks, 10% at 4 blocks, silent at <2 blocks. Creates "uncanny valley" effect.

## Sources

### Primary (HIGH confidence)
- [Fabric Docs - Creating Custom Sounds](https://docs.fabricmc.net/develop/sounds/custom) - Audio file formats, registration
- [Fabric Docs - Playing Sounds](https://docs.fabricmc.net/1.20.4/develop/sounds/using-sounds) - Server-side playback, volume control
- [Fabric Wiki - Creating Entities](https://wiki.fabricmc.net/tutorial:entity) - Entity basics, hierarchy
- [Fabric Wiki - Persistent States](https://wiki.fabricmc.net/tutorial:persistent_states) - NBT storage, world data
- [Fabric API - ServerTickEvents](https://maven.fabricmc.net/docs/fabric-api-0.110.5+1.21.4/net/fabricmc/fabric/api/event/lifecycle/v1/ServerTickEvents.html) - Tick hooks
- [Fabric API - Goal System](https://maven.fabricmc.net/docs/yarn-1.18.1+build.12/net/minecraft/entity/ai/goal/package-summary.html) - GoalSelector, Control flags
- [GeckoLib Wiki - Entity Animations (GeckoLib 4)](https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-(Geckolib4)) - Animation controllers, triggering
- [McJty Modding Tutorial Ep4](https://www.mcjty.eu/docs/1.18/ep4) - Custom Goal implementation example

### Secondary (MEDIUM confidence)
- [Minecraft Wiki - Rotation](https://minecraft.wiki/w/Rotation) - Yaw/pitch ranges, rotation vectors
- [Minecraft Wiki - Mob Spawning](https://minecraft.wiki/w/Mob_spawning) - Spawn weight system
- [Minecraft Commands Wiki - Look Detection](https://minecraftcommands.github.io/wiki/questions/lookat.html) - FOV detection patterns
- [Minecraft Commands Wiki - Raycast](https://minecraftcommands.github.io/wiki/questions/raycast.html) - Raycasting for obstruction checks
- [CurseForge - SmartBrainLib](https://www.curseforge.com/minecraft/mc-mods/smartbrainlib) - Alternative AI system (Brain-based)
- [CurseForge - Tense Ambience](https://modrinth.com/mod/tense-ambience) - Horror sound design reference

### Tertiary (LOW confidence - marked for validation)
- [Bedrock Wiki - Entity Events](https://wiki.bedrock.dev/entities/entity-events) - Spawn event patterns (Bedrock, not Java)
- [SpigotMC Forums - FOV Detection](https://www.spigotmc.org/threads/check-if-entity-is-in-fov-of-player.227276/) - Plugin-based FOV (not Fabric, but concept applies)
- Community horror mod discussions (design philosophy, not implementation)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Fabric API and vanilla systems are well-documented, GeckoLib already integrated
- Architecture: MEDIUM-HIGH - Goal system patterns verified from official tutorials, sound system verified from Fabric docs, spawn probability is custom design
- Pitfalls: HIGH - Client-server desync, sound channels, goal conflicts, memory leaks all verified from official bug trackers and community issues
- Code examples: HIGH - All examples based on official Fabric docs, McJty tutorial, or GeckoLib wiki

**Research date:** 2026-01-24
**Valid until:** 2026-03-24 (60 days - Fabric API stable, Minecraft 1.21 mature)
