# Architecture Patterns: Fabric 1.21.x Horror Mod

**Domain:** Minecraft Horror Mod (Fabric 1.21.x)
**Researched:** 2026-01-23
**Confidence:** MEDIUM (official docs verified, specific horror patterns from ecosystem survey)

## Recommended Architecture

Fabric mods follow a modular, event-driven architecture with strict client/server separation. For a horror mod like Dread, the architecture consists of five core systems that communicate through Fabric's networking and event APIs.

```
┌─────────────────────────────────────────────────────────────┐
│                     FABRIC MOD LOADER                        │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┴─────────────────────┐
        │                                           │
   ┌────▼────┐                                 ┌────▼────┐
   │ SERVER  │◄────── Networking ──────────────┤ CLIENT  │
   │  SIDE   │        (Packets)                │  SIDE   │
   └────┬────┘                                 └────┬────┘
        │                                           │
   ┌────▼────────────────────┐          ┌──────────▼─────────────┐
   │ Entity System           │          │ Rendering System       │
   │ - Dread Entity          │          │ - Entity Renderer      │
   │ - AI Goals/Pathfinding  │          │ - HUD Overlays         │
   │ - Spawn Logic           │          │ - Screen Effects       │
   │ - Attack Logic          │          │ - Cinematic Sequence   │
   └─────────────────────────┘          └────────────────────────┘

   ┌─────────────────────────┐          ┌────────────────────────┐
   │ Game State System       │          │ Audio System           │
   │ - Player State Manager  │          │ - Sound Events         │
   │ - Downed State Logic    │          │ - Ambient Sounds       │
   │ - Revive Mechanics      │          │ - Jump Scare Audio     │
   │ - Spawn Ticker          │          │ - Spatial Audio        │
   └─────────────────────────┘          └────────────────────────┘
```

### Component Boundaries

| Component | Responsibility | Communicates With | Side |
|-----------|---------------|-------------------|------|
| **Entity System** | Custom Dread entity with AI, pathfinding, attack behavior | Game State (spawn checks), Audio (triggers), Client (via tracking) | Server |
| **Game State System** | Player state tracking, downed state, revive timer, spawn ticker | Entity System (spawn events), Networking (state sync), Client (HUD data) | Server |
| **Audio System** | Sound registration, playback control, spatial audio | Game State (events), Client (playback), Entity System (entity sounds) | Both |
| **Rendering System** | Entity visuals, HUD overlays, screen effects, cinematic | Audio (synchronized playback), Networking (state data) | Client |
| **Networking System** | Client/server state synchronization | All systems (as bridge) | Both |

### Data Flow

**Spawn Flow (Server → Client):**
```
1. Game State System ticks spawn probability
2. Spawn check passes → Entity System creates Dread entity
3. Entity spawns behind player (server-side positioning)
4. Entity tracking automatically syncs to client
5. Client receives entity data → Rendering System displays entity
6. Audio System plays tension/jump scare sounds
```

**Kill Flow (Server → Client):**
```
1. Dread entity attacks player (server-side)
2. Game State System transitions player to "downed" state
3. Custom packet sent to client with state data
4. Client Rendering System triggers cinematic sequence
5. Audio System plays death sounds (synchronized)
6. HUD updates to show downed timer and blur effect
7. Server starts 300-second countdown
```

**Revive Flow (Client → Server → All Clients):**
```
1. Player crouches near downed player (client input)
2. Client sends revive request packet to server
3. Server validates proximity and state
4. Game State System removes downed state
5. Server broadcasts state change to all clients
6. All clients update rendering (remove effects)
```

## Standard Fabric Project Structure

Based on official Fabric documentation, the recommended structure for this mod:

```
dread-mod/
├── src/
│   ├── main/
│   │   ├── java/com/example/dread/
│   │   │   ├── DreadMod.java              # ModInitializer (common init)
│   │   │   ├── entity/
│   │   │   │   ├── DreadEntity.java       # Custom entity class
│   │   │   │   └── DreadEntityAI.java     # AI goals and behaviors
│   │   │   ├── state/
│   │   │   │   ├── PlayerStateManager.java # Downed state logic
│   │   │   │   ├── SpawnTickerManager.java # Spawn probability system
│   │   │   │   └── DownedPlayerComponent.java # Cardinal Components API
│   │   │   ├── network/
│   │   │   │   ├── DreadNetworking.java   # Packet registration
│   │   │   │   └── payloads/
│   │   │   │       ├── DownedStatePayload.java
│   │   │   │       └── RevivePayload.java
│   │   │   ├── sound/
│   │   │   │   └── DreadSounds.java       # Sound event registration
│   │   │   └── mixin/
│   │   │       └── ServerPlayerMixin.java # Player state hooks
│   │   └── resources/
│   │       ├── fabric.mod.json             # Mod metadata
│   │       ├── dread.mixins.json          # Mixin configuration
│   │       └── assets/dread/
│   │           ├── sounds.json             # Sound definitions
│   │           ├── sounds/
│   │           │   ├── ambient_tension.ogg
│   │           │   ├── jumpscare.ogg
│   │           │   └── death_sequence.ogg
│   │           ├── textures/entity/
│   │           │   └── dread.png
│   │           └── models/entity/
│   │               └── dread.json
│   └── client/
│       ├── java/com/example/dread/client/
│       │   ├── DreadClientMod.java         # ClientModInitializer
│       │   ├── render/
│       │   │   ├── DreadEntityRenderer.java
│       │   │   ├── DreadEntityModel.java
│       │   │   └── CinematicRenderer.java  # Death sequence rendering
│       │   ├── hud/
│       │   │   └── DownedStateHud.java     # Timer + blur overlay
│       │   └── mixin/
│       │       └── GameRendererMixin.java  # Screen shake/effects
│       └── resources/
│           └── assets/dread/
│               └── shaders/                # Post-processing effects
│                   └── blur.json
└── gradle/                                  # Build configuration
```

## Patterns to Follow

### Pattern 1: Client/Server Separation
**What:** Strictly separate client-only code from server/common code using Fabric's dual entrypoint system.

**When:** Always. Mixing client and server code causes crashes on dedicated servers.

**Example:**
```java
// src/main/java - Common/Server code
public class DreadMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register entity, sounds, networking
        DreadEntity.register();
        DreadSounds.register();
        DreadNetworking.register();
    }
}

// src/client/java - Client-only code
public class DreadClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register renderers, HUD elements
        EntityRendererRegistry.register(DreadEntity.TYPE, DreadEntityRenderer::new);
        HudRenderCallback.EVENT.register(new DownedStateHud());
    }
}
```

### Pattern 2: Event-Driven State Management
**What:** Use Fabric's event callbacks for lifecycle hooks rather than polling or mixins when events exist.

**When:** State changes, tick events, rendering callbacks, networking events.

**Example:**
```java
// Register server tick event for spawn ticker
ServerTickEvents.END_SERVER_TICK.register(server -> {
    SpawnTickerManager.tick(server);
});

// Register HUD rendering (deprecated in 1.21.6+, use HudElementRegistry)
HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
    if (PlayerStateManager.isPlayerDowned(client.player)) {
        renderDownedOverlay(drawContext);
    }
});
```

### Pattern 3: Cardinal Components for Player State
**What:** Use Cardinal Components API (Fabric's recommended component system) to attach custom data to players.

**When:** Need to store custom state (downed status, timer) that persists and syncs automatically.

**Example:**
```java
public class DownedPlayerComponent implements Component {
    private boolean isDowned = false;
    private int downedTicksRemaining = 0;

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putBoolean("downed", isDowned);
        tag.putInt("downedTicks", downedTicksRemaining);
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        isDowned = tag.getBoolean("downed");
        downedTicksRemaining = tag.getInt("downedTicks");
    }
}
```

### Pattern 4: Custom Packets for State Synchronization
**What:** Define typed payloads using Java Records implementing `CustomPacketPayload` for client/server communication.

**When:** Syncing game state that doesn't fit vanilla tracking (downed state, revive events).

**Example:**
```java
public record DownedStatePayload(UUID playerId, int ticksRemaining)
    implements CustomPacketPayload {

    public static final Identifier ID = Identifier.of("dread", "downed_state");
    public static final PacketCodec<ByteBuf, DownedStatePayload> CODEC =
        PacketCodec.tuple(
            Uuids.PACKET_CODEC, DownedStatePayload::playerId,
            PacketCodecs.INTEGER, DownedStatePayload::ticksRemaining,
            DownedStatePayload::new
        );

    @Override
    public Id<? extends CustomPacketPayload> getId() {
        return new Id<>(ID);
    }
}

// Registration
PayloadTypeRegistry.playS2C().register(
    new PayloadType<>(DownedStatePayload.ID, DownedStatePayload.CODEC)
);
```

### Pattern 5: Entity AI Using Goals
**What:** Extend `PathAwareEntity` and use Minecraft's goal system for AI behaviors.

**When:** Creating custom mobs with complex behaviors (stalking, attacking, pathfinding).

**Example:**
```java
public class DreadEntity extends PathAwareEntity {
    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new StalkPlayerGoal(this));  // Custom goal
        this.goalSelector.add(3, new JumpScareAttackGoal(this));  // Custom goal
        this.goalSelector.add(4, new WanderAroundGoal(this, 0.8));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 100.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }
}
```

### Pattern 6: Mixin for Unavoidable Hooks
**What:** Use Spongepowered Mixins to inject code into vanilla classes when no event exists.

**When:** Need to hook player death, game mode switching, or other vanilla behavior without events.

**Example:**
```java
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerMixin {
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private void onPlayerDeath(DamageSource source, CallbackInfo ci) {
        if (source.getAttacker() instanceof DreadEntity) {
            // Transition to downed state instead of death
            PlayerStateManager.setDowned((ServerPlayerEntity)(Object)this);
            ci.cancel();  // Prevent vanilla death
        }
    }
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Client Code in Common Classes
**What:** Importing client-only classes (like `MinecraftClient`) in common/server code.

**Why bad:** Crashes dedicated servers with ClassNotFoundException because client classes don't exist on server JARs.

**Instead:** Use Fabric's `@Environment(EnvType.CLIENT)` or separate source sets entirely.

**Example:**
```java
// BAD - Will crash servers
public class DreadMod implements ModInitializer {
    public void onInitialize() {
        MinecraftClient.getInstance().player.sendMessage(...);  // CRASH!
    }
}

// GOOD - Client code in client source set
// src/client/java
public class DreadClientMod implements ClientModInitializer {
    public void onInitializeClient() {
        MinecraftClient.getInstance();  // Safe, only loaded on client
    }
}
```

### Anti-Pattern 2: Trusting Client Data
**What:** Accepting client packets without server-side validation.

**Why bad:** Players can send malicious packets (instant revive, teleport to downed player from across map).

**Instead:** Always validate on server: check proximity, state validity, cooldowns.

**Example:**
```java
// BAD - No validation
ServerPlayNetworking.registerGlobalReceiver(RevivePayload.ID, (payload, context) -> {
    PlayerStateManager.revivePlayer(payload.targetId());  // Exploitable!
});

// GOOD - Validate everything
ServerPlayNetworking.registerGlobalReceiver(RevivePayload.ID, (payload, context) -> {
    ServerPlayerEntity reviver = context.player();
    ServerPlayerEntity target = server.getPlayerManager().getPlayer(payload.targetId());

    if (target == null || !PlayerStateManager.isDowned(target)) return;
    if (reviver.squaredDistanceTo(target) > 9.0) return;  // 3 block radius
    if (!reviver.isSneaking()) return;

    PlayerStateManager.revivePlayer(target);  // Safe
});
```

### Anti-Pattern 3: Manual Entity Tracking
**What:** Manually sending entity spawn packets to clients.

**Why bad:** Fabric/Minecraft handles entity tracking automatically based on view distance.

**Instead:** Just spawn the entity server-side. Tracking syncs it to nearby clients automatically.

**Example:**
```java
// BAD - Unnecessary manual sync
DreadEntity dread = new DreadEntity(world);
world.spawnEntity(dread);
// Then manually sending custom packets to clients... NO!

// GOOD - Let Minecraft handle it
DreadEntity dread = new DreadEntity(EntityType.DREAD, world);
dread.setPosition(x, y, z);
world.spawnEntity(dread);  // Tracking handles client sync automatically
```

### Anti-Pattern 4: Polling Instead of Events
**What:** Checking conditions every tick in a loop instead of using event callbacks.

**Why bad:** Performance overhead, harder to maintain, misses edge cases.

**Instead:** Use Fabric's event system or entity goals for behavior.

**Example:**
```java
// BAD - Polling every tick
ServerTickEvents.END_SERVER_TICK.register(server -> {
    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
        if (/* check if player should be attacked */) {
            // Attack logic
        }
    }
});

// GOOD - Entity AI handles it
public class JumpScareAttackGoal extends Goal {
    @Override
    public boolean canStart() {
        PlayerEntity target = this.mob.world.getClosestPlayer(this.mob, 3.0);
        return target != null && canJumpScare(target);
    }

    @Override
    public void tick() {
        executeJumpScare();
    }
}
```

### Anti-Pattern 5: Hardcoded Sound Paths
**What:** Playing sounds using string literals scattered throughout code.

**Why bad:** Typos cause silent failures, hard to maintain, no compile-time safety.

**Instead:** Register `SoundEvent` constants and reference them.

**Example:**
```java
// BAD
world.playSound(null, pos, Identifier.of("dread", "jumpscare"), ...);  // Typo risk

// GOOD
public class DreadSounds {
    public static final SoundEvent JUMPSCARE = register("jumpscare");
    public static final SoundEvent AMBIENT = register("ambient_tension");

    private static SoundEvent register(String id) {
        Identifier identifier = Identifier.of("dread", id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier,
            SoundEvent.createVariableRangeEvent(identifier));
    }
}

// Usage
world.playSound(null, pos, DreadSounds.JUMPSCARE, ...);  // Type-safe
```

## Build Order and Dependencies

Based on component dependencies, recommended implementation order:

### Phase 1: Foundation (No Dependencies)
1. **Project Structure** - Set up Fabric mod skeleton with proper source sets
2. **Networking System** - Define payload types and registration (needed by all systems)
3. **Sound System** - Register sound events and create placeholder audio files

**Why first:** These are foundational. Other systems depend on networking and sounds.

### Phase 2: Entity System (Depends on: Foundation)
4. **Entity Class** - Create `DreadEntity` extending `PathAwareEntity`
5. **Entity Registration** - Register entity type, attributes, spawn egg (optional)
6. **Client Renderer** - Model, texture, and renderer (client-side)

**Why second:** Entity is the core mechanic. Once visible, you can test rendering and movement.

### Phase 3: Game State (Depends on: Entity, Networking)
7. **Player State Component** - Cardinal Components API for downed state
8. **Downed State Logic** - Server-side state transitions (death → downed → spectator)
9. **Spawn Ticker** - Probability system that increases over time

**Why third:** Requires entity to exist (for kill events) and networking (for state sync).

### Phase 4: Client Rendering (Depends on: Game State, Networking)
10. **HUD Overlays** - Downed state timer, blur effect
11. **Screen Effects** - Post-processing shaders for blur/darkness
12. **Cinematic Sequence** - Camera control and synchronized rendering

**Why fourth:** Needs game state data from server to know what to render.

### Phase 5: AI and Behavior (Depends on: Entity, Game State)
13. **Custom AI Goals** - Stalking behavior, jump scare positioning
14. **Attack Logic** - Trigger downed state on successful attack
15. **Spawn Logic Integration** - Connect spawn ticker to entity spawning

**Why fifth:** Entity must exist, game state must handle transitions. AI brings it all together.

### Phase 6: Revive System (Depends on: All Above)
16. **Revive Detection** - Server-side proximity and crouch detection
17. **Revive Networking** - Client request, server validation, broadcast
18. **Multiplayer Testing** - Ensure state syncs correctly across clients

**Why last:** Most complex system, depends on everything else working correctly.

## Multiplayer Synchronization Strategy

For a horror mod with critical timing (jump scares, death sequences), synchronization is essential.

### What Needs Syncing

| Data | Sync Method | Direction | Frequency |
|------|-------------|-----------|-----------|
| Dread entity position/rotation | Automatic tracking | Server → Client | Every tick (in view distance) |
| Player downed state | Custom packet | Server → All Clients | On state change |
| Downed timer | Custom packet | Server → Client | Every 20 ticks (1 second) |
| Revive request | Custom packet | Client → Server | On crouch input |
| Spawn ticker value | No sync needed | Server only | N/A |
| Jump scare trigger | Entity attack event | Server → Client | On attack |
| Cinematic playback | Custom packet | Server → Client | On kill |

### Synchronization Patterns

**State Change Pattern:**
```
1. Server changes player state (downed = true)
2. Server sends DownedStatePayload to all tracking clients
3. Clients receive packet, update local state cache
4. Client rendering system queries cache, shows HUD
```

**Timer Update Pattern:**
```
1. Server ticks downed timer (every tick)
2. Every 20 ticks (1 second), send updated value to affected client
3. Client interpolates display to smooth out network jitter
```

**Client Action Pattern:**
```
1. Client detects input (crouch near downed player)
2. Client sends RevivePayload to server
3. Server validates request (proximity, state, cooldown)
4. If valid: Server updates state, broadcasts to all clients
5. If invalid: Server ignores (no response = failed)
```

### Desync Prevention

Common desync scenarios and solutions:

| Scenario | Problem | Solution |
|----------|---------|----------|
| Client disconnects while downed | State lost on rejoin | Use Cardinal Components with NBT serialization |
| Packet loss during revive | Client thinks they're revived but aren't | Server is authoritative; client must wait for confirmation packet |
| Cinematic interrupted by lag | Sequence desyncs from audio | Use server tick count as timeline reference, client interpolates |
| Entity spawns behind player | Client renders before turn | Server sends spawn packet with initial rotation aligned to player view |

## Performance Considerations

Horror mods have unique performance requirements (jump scares can't lag).

| System | Performance Strategy | Rationale |
|--------|---------------------|-----------|
| **Entity Spawning** | Limit to 1 Dread entity per player max | Prevents spawn spam, reduces AI load |
| **AI Goals** | Use cached target searches (every 10 ticks, not every tick) | Pathfinding is expensive, stalking doesn't need tick-perfect precision |
| **Audio System** | Preload jump scare sounds during tension phase | Prevents disk I/O lag during critical moment |
| **Cinematic Rendering** | Use interpolated camera movements, not per-tick calculations | Smooth 60fps playback even on 20 TPS server |
| **HUD Overlays** | Only render when player is downed | Avoid rendering overhead for non-downed players |
| **Networking** | Batch state updates (1 packet/second for timer) | Reduce network bandwidth, 1-second precision is sufficient |

## Sources

**HIGH CONFIDENCE (Official Documentation):**
- [Project Structure - Fabric Documentation](https://docs.fabricmc.net/develop/getting-started/project-structure)
- [Networking - Fabric Documentation](https://docs.fabricmc.net/develop/networking)
- [Creating an Entity - Fabric Wiki](https://wiki.fabricmc.net/tutorial:entity)
- [Creating Custom Sounds - Fabric Documentation](https://docs.fabricmc.net/develop/sounds/custom)
- [Mixin Injects - Fabric Wiki](https://wiki.fabricmc.net/tutorial:mixin_injects)
- [Rendering in the HUD - Fabric Documentation](https://docs.fabricmc.net/develop/rendering/hud)

**MEDIUM CONFIDENCE (Official APIs with examples):**
- [Registry Synchronization - FabricMC DeepWiki](https://deepwiki.com/FabricMC/fabric/4-registry-synchronization)
- [Event Index - Fabric Wiki](https://fabricmc.net/wiki/tutorial:event_index)
- [SmartBrainLib - Advanced AI System](https://www.curseforge.com/minecraft/mc-mods/smartbrainlib)

**LOW CONFIDENCE (Community patterns, unverified for 1.21.x):**
- Horror mod ecosystem survey from CurseForge (design patterns, not technical architecture)
- Community mods for spectator mode handling (implementation-specific)
