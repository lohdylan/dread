# Domain Pitfalls: Fabric Minecraft Mod Development

**Domain:** Minecraft Horror Mod (Fabric 1.21.x)
**Researched:** 2026-01-23
**Project Context:** Dread - Custom entity horror mod with jump scares, cinematic death sequences, multiplayer revive mechanics

---

## Critical Pitfalls

Mistakes that cause rewrites, catastrophic failures, or fundamental design flaws.

### Pitfall 1: Client-Server Side Confusion
**What goes wrong:** Developers confuse physical and logical sides, leading to `ClassNotFoundException` crashes on dedicated servers or data desynchronization between clients and server.

**Why it happens:** A Minecraft client hosts its own integrated server for singleplayer/LAN, leading to the false assumption that "client code" and "server code" are separate when they're actually hierarchical. Many developers assume direct memory access between sides is valid.

**Consequences:**
- Crashes on dedicated servers with `ClassNotFoundException`
- Multiplayer desync where clients see different game states
- Jump scare timing desync (critical for Dread - one player sees entity, another doesn't)
- Downed state persistence failing in multiplayer

**Prevention:**
- **Never** exchange data directly between logical sides
- **Always** use packets for client-server communication (C2S and S2C)
- Use `world.isClient()` to check logical side, NOT `@Environment(EnvType.CLIENT)`
- For Dread: Entity spawn, jump scare triggers, and downed state MUST use S2C packets
- Server is authoritative - all game logic happens server-side, clients only render

**Detection:**
- Works in singleplayer but crashes in multiplayer
- Different players see different entity positions/states
- Log shows "Accessing client-only class on server"

**Phase Impact:** Phase 1 (Core Entity) - Entity registration must be sided correctly from the start or causes rewrites later.

**Sources:**
- [Fabric Wiki: Side Tutorial](https://wiki.fabricmc.net/tutorial:side)
- [Fabric Documentation: Networking](https://docs.fabricmc.net/develop/networking)

---

### Pitfall 2: Missing Server-Side Packet Validation
**What goes wrong:** Accepting client packets without validation allows exploits and causes crashes when clients send malformed data. Critical for multiplayer revive mechanics.

**Why it happens:** Developers trust that clients will only send valid packets. In multiplayer games, never trust the client.

**Consequences:**
- Players can revive themselves without meeting conditions
- Players can trigger jump scares remotely on other players
- Null pointer exceptions when targeted entity doesn't exist
- Downed state can be manipulated (e.g., teleporting while downed)

**Prevention:**
- **Always** validate packet contents on server
- Check entity existence before applying effects
- Range-check distance/position data
- Verify player has permission to perform action
- For Dread revive system: Verify reviver is within range, alive, and not in cooldown

```java
// GOOD: Server-side validation
ServerPlayNetworking.registerGlobalReceiver(REVIVE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
    UUID targetUuid = buf.readUuid();
    server.execute(() -> {
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetUuid);
        if (target == null) return; // Validation
        if (player.squaredDistanceTo(target) > MAX_REVIVE_DISTANCE * MAX_REVIVE_DISTANCE) return; // Range check
        if (!isPlayerDowned(target)) return; // State check
        // Now safe to revive
    });
});
```

**Detection:**
- Server logs show NullPointerException in packet handlers
- Players report being able to "cheat" mechanics
- Crashes when specific players perform actions

**Phase Impact:** Phase 3 (Multiplayer Sync) - Must validate all packets from day one to avoid security rewrites.

**Sources:**
- [Fabric Documentation: Networking](https://docs.fabricmc.net/develop/networking)

---

### Pitfall 3: NBT Data Not Marked Dirty
**What goes wrong:** Persistent data (downed state, player statistics, entity data) vanishes when server restarts because state wasn't marked as dirty.

**Why it happens:** Developers forget to call `markDirty()` after modifying persistent state. Minecraft only saves data that's been marked as changed.

**Consequences:**
- Downed players respawn normally after server restart (losing downed state)
- Jump scare statistics/cooldowns reset
- Custom entity data (appearance variants, AI state) lost
- Player progress (total kills, escapes) deleted

**Prevention:**
- **Always** call `markDirty()` immediately after changing persistent state
- Create wrapper methods that auto-mark dirty:

```java
public void setPlayerDowned(UUID playerUuid, boolean downed) {
    this.downedPlayers.put(playerUuid, downed);
    this.markDirty(); // CRITICAL: Mark dirty every time
}
```

- Test with server restart immediately after making state changes
- For Dread: Downed state, revive cooldowns, entity spawn positions must persist

**Detection:**
- Data works until server restart, then resets
- Players report "losing progress" after crashes
- Data exists in-game but not in level.dat after save

**Phase Impact:** Phase 3 (Multiplayer Sync), Phase 4 (Downed State) - Downed state persistence is core feature, must work from first implementation.

**Sources:**
- [Fabric Wiki: Persistent States](https://wiki.fabricmc.net/tutorial:persistent_states)

---

### Pitfall 4: Entity Memory Leak on Death/Despawn
**What goes wrong:** Entity memories (AI target data, pathfinding data) not cleared when entity dies, causing memory to accumulate until server crashes or severe lag.

**Why it happens:** Minecraft bug MC-260605 - entity memories aren't automatically cleaned up on death/removal. Custom entities that spawn frequently (like a stalking horror entity) leak memory rapidly.

**Consequences:**
- Server memory usage grows unbounded
- TPS drops over time as memory fills
- Eventually: OutOfMemoryError crash
- For Dread: If entity respawns after death/despawn, memory leak compounds quickly

**Prevention:**
- Manually clear entity brain/memories in entity's `remove()` method:

```java
@Override
public void remove(RemovalReason reason) {
    if (this.getBrain() != null) {
        this.getBrain().clear(); // Clear memories
    }
    super.remove(reason);
}
```

- Use MemoryLeakFix mod during development to detect leaks early
- Monitor server memory usage in long-running tests
- For Dread: Entity that stalks players and respawns needs aggressive cleanup

**Detection:**
- Server memory grows over hours of play
- `/forge track` or profiler shows entity references not being GC'd
- Performance degrades over time, improves after restart

**Phase Impact:** Phase 1 (Core Entity) - Must implement from the start as memory leaks are hard to debug retroactively.

**Sources:**
- [GitHub: MemoryLeakFix](https://github.com/FxMorin/MemoryLeakFix)
- [Modrinth: Memory Leak Fix](https://modrinth.com/mod/memoryleakfix)

---

### Pitfall 5: Sound Channel Limit Exceeded
**What goes wrong:** Playing too many sounds simultaneously (ambient horror sounds, jump scare sound, footsteps, music) causes OpenAL to crash or all sound to stop working until slots free up.

**Why it happens:** Minecraft uses OpenAL with a hard limit of 247 simultaneous sound channels. Horror mods often layer atmospheric sounds, creating channel exhaustion.

**Consequences:**
- Jump scare sound doesn't play (catastrophic failure for horror mod!)
- All game audio stops working mid-game
- OpenAL crashes: "AL lib: (EE) DoReset: Failed to initialize audio client"
- Horror atmosphere ruined when sounds cut out

**Prevention:**
- **Prioritize critical sounds** - Jump scare > entity sounds > ambient > music
- Stop ambient/looping sounds before playing high-priority sounds
- Use Audio Engine Tweaks mod's priority system during development
- Limit concurrent ambient sounds (max 2-3 atmospheric loops)
- For Dread jump scare: Stop ALL ambient sounds 0.5s before scare, play scare sound, resume ambient

```java
// Before jump scare
stopAllAmbientSounds(player);
world.playSound(null, player.getPos(), JUMP_SCARE_SOUND,
    SoundCategory.HOSTILE, 2.0f, 1.0f);
```

**Detection:**
- Sounds randomly stop playing
- Log shows OpenAL errors
- Players report "no sound during jump scare"
- Testing with 4+ players causes sound failures

**Phase Impact:** Phase 2 (Jump Scare Mechanics) - Must design sound priority from the start, retrofitting is complex.

**Sources:**
- [GitHub Issue: Sound Physics Lag with Many Sounds](https://github.com/henkelmax/sound-physics-remastered/issues/234)
- [Modrinth: Audio Engine Tweaks](https://modrinth.com/mod/audio-engine-tweaks)

---

## Moderate Pitfalls

Mistakes that cause delays, technical debt, or significant refactoring.

### Pitfall 6: Mixin Priority Conflicts
**What goes wrong:** Multiple mods (or multiple mixins in your mod) try to modify the same code with the same priority, causing crashes with "@Redirect conflict" or "@ModifyConstant conflict".

**Why it happens:** Default mixin priority is 1000. When two mixins target the same method at priority 1000, one gets randomly skipped, causing unpredictable crashes.

**Consequences:**
- Mod crashes on startup with MixinTransformerError
- Works in development, crashes when players use other mods
- Incompatibility with popular mods (Sodium, Iris, OptiFine)

**Prevention:**
- Set explicit priority in mixin config (lower = higher priority):

```json
{
  "priority": 900,  // Higher priority than default 1000
  "mixins": [...]
}
```

- Use Mixin Conflict Helper mod to detect conflicts during development
- Prefer `@Inject` over `@Redirect` (less likely to conflict)
- Test with popular mod combinations (Sodium, Iris, etc.)

**Detection:**
- Crash on startup with "Mixin transformation failed"
- Works alone but crashes with certain mod combinations
- Log shows "@Redirect conflict"

**Phase Impact:** Phase 1-5 (All phases using mixins) - Plan mixin strategy early, changing priorities later requires full regression testing.

**Sources:**
- [Modrinth: Mixin Conflict Helper](https://modrinth.com/mod/mixin-conflict-helper)
- [GitHub Discussion: Mixin Conflicts](https://lightrun.com/answers/devs-immortal-paradise-lost-crash-mixins)

---

### Pitfall 7: Rendering Transparency Z-Fighting
**What goes wrong:** Transparent parts of custom entity model (glowing eyes, ethereal effects) flicker or render incorrectly, especially with shaders enabled. Ruins horror atmosphere.

**Why it happens:** Multiple transparent layers render at same depth, causing z-fighting. Shader mods (Iris/Optifine) change rendering pipeline, breaking transparency sorting.

**Consequences:**
- Entity eyes/emissive textures flicker
- Transparent tentacles render solid
- Shaders make entity invisible or render as black box
- Horror aesthetic ruined by visual glitches

**Prevention:**
- Use `RenderLayer.getEntityTranslucentCull()` for transparent entity parts
- Separate emissive layers from base texture
- Test with Iris/Optifine shaders during development
- For complex models: Use Entity Texture Features compatible rendering modes
- Slightly offset transparent layers in model (0.01 blocks) to prevent z-fighting

**Detection:**
- Entity looks correct without shaders, breaks with shaders
- Flickering textures when entity moves
- GitHub issues from players using shader packs

**Phase Impact:** Phase 1 (Core Entity) - Entity appearance is first impression, fix rendering early before players associate entity with "buggy graphics".

**Sources:**
- [Modrinth: Entity Texture Features](https://modrinth.com/mod/entitytexturefeatures)
- [Forge Forums: Z-Fighting with Semi-Transparent Layers](https://forums.minecraftforge.net/topic/81735-1144-z-fighting-issues-with-semi-transparent-entity-layers/)

---

### Pitfall 8: Client-Only Persistent Data Access
**What goes wrong:** Attempting to read server-only persistent data (downed state, player stats) directly on client causes NPE or returns stale/null data.

**Why it happens:** `PersistentState` only exists on server. Developers forget to sync data to clients via packets.

**Consequences:**
- Downed state UI shows incorrect status
- Client can't render revive progress bar (no data)
- Player sees themselves as alive while server knows they're downed
- Desync between what player sees and actual game state

**Prevention:**
- Send initial sync packet when player joins:

```java
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    // Send current downed state to joining player
    sender.sendPacket(new DownedStateSyncPacket(getDownedPlayers()));
});
```

- Send update packets whenever state changes:

```java
public void setPlayerDowned(UUID uuid, boolean downed) {
    this.downedPlayers.put(uuid, downed);
    this.markDirty();

    // Sync to all clients
    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
        ServerPlayNetworking.send(player, new DownedStateUpdatePacket(uuid, downed));
    }
}
```

- Never access `PersistentState` on client code

**Detection:**
- UI shows placeholder/null values
- Log shows NPE when accessing state on client
- Data correct on server, wrong on client

**Phase Impact:** Phase 4 (Downed State) - UI needs data to render, must sync from first UI implementation.

**Sources:**
- [Fabric Wiki: Persistent States](https://wiki.fabricmc.net/tutorial:persistent_states)

---

### Pitfall 9: Entity Tracking Range Too Small
**What goes wrong:** Horror entity despawns when player looks away or moves too far, breaking tension. Player turns around, entity vanishes, turns back, entity is gone - scare ruined.

**Consequences:**
- Entity disappears mid-chase
- Jump scare interrupted by despawn
- Entity can't build tension by following player over distance
- Multiplayer: Entity visible to one player, invisible to another

**Prevention:**
- Increase tracking range in entity registration:

```java
FabricDefaultAttributeRegistry.register(DREAD_ENTITY,
    DreadEntity.createAttributes()
        .trackingRange(128)  // Default is 64, increase for stalking behavior
        .trackingTickInterval(2)  // How often to sync position
        .forceTrackedVelocityUpdates(true)  // Always sync velocity
);
```

- For stalking entity: Consider tracking range 128+ blocks
- Balance with network performance (larger range = more packets)

**Detection:**
- Entity vanishes when player backs away
- Players report "entity disappeared during chase"
- Multiplayer testing shows entity visible to host but not other players

**Phase Impact:** Phase 1 (Core Entity) - Tracking range set during registration, changing later requires migration/testing.

**Sources:**
- Community knowledge (Fabric entity registration)

---

### Pitfall 10: GeckoLib Animation Performance Drain
**What goes wrong:** Complex entity animations (tentacles, multiple moving parts) with high bone count cause FPS drops, especially with multiple entities or in multiplayer.

**Why it happens:** Each animated bone requires matrix calculations per frame. Horror mods often use complex models for atmosphere, but 50+ bones on multiple entities = performance hit.

**Consequences:**
- FPS drops when entity is visible
- Cinematic death sequence stutters/lags
- Multiplayer server TPS drops with multiple entities
- Players with low-end PCs can't play

**Prevention:**
- Keep entity bone count under 30 for performance
- Use vertex animations for subtle movement (cheaper than bones)
- Reduce animation tick rate for distant entities:

```java
@Override
public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    controllers.add(new AnimationController<>(this, "controller", 0, state -> {
        // Reduce animation updates when far from players
        if (state.getAnimatable().isOutOfPlayerRange()) {
            return state.setAndContinue(IDLE_ANIMATION);
        }
        return state.setAndContinue(COMPLEX_ANIMATION);
    }));
}
```

- Test with multiple entities spawned simultaneously
- Profile frame time with GeckoLib animations active

**Detection:**
- FPS counter drops when looking at entity
- F3 profiler shows high time in "renderEntity"
- Players report lag during jump scares

**Phase Impact:** Phase 1 (Core Entity) - Model bone count set during creation, reducing later requires model rebuild.

**Sources:**
- [Modrinth: GeckoLib](https://modrinth.com/mod/geckolib)
- [GitHub: GeckoLib Wiki - Entity Animations](https://github.com/bernie-g/geckolib/wiki/Entity-Animations)

---

## Minor Pitfalls

Mistakes that cause annoyance but are fixable without major refactoring.

### Pitfall 11: Forgetting super.writeNbt() / super.readNbt()
**What goes wrong:** Custom entity data saves but position/UUID lost, causing entity to teleport to 0,0,0 or fail to load.

**Why it happens:** Developers override `writeNbt()`/`readNbt()` without calling super methods, losing vanilla entity data.

**Consequences:**
- Entity spawns at world origin on reload
- Entity loses AI state on chunk reload
- Custom data saves but entity fundamentally broken

**Prevention:**
```java
@Override
public void writeNbt(NbtCompound nbt) {
    super.writeNbt(nbt);  // ALWAYS call super first
    nbt.putBoolean("hasJumpScared", this.hasJumpScared);
}

@Override
public void readNbt(NbtCompound nbt) {
    super.readNbt(nbt);  // ALWAYS call super first
    this.hasJumpScared = nbt.getBoolean("hasJumpScared");
}
```

**Detection:**
- Entity position resets on world reload
- Log shows "Could not load entity"

**Phase Impact:** Phase 1 (Core Entity) - Easy fix, catches in testing.

---

### Pitfall 12: Hardcoded Sleep/Delays in Packet Handlers
**What goes wrong:** Using `Thread.sleep()` or blocking operations in packet handlers freezes server/client.

**Why it happens:** Developers want to delay actions (e.g., 2 second revive time) and use sleep instead of scheduled tasks.

**Consequences:**
- Server freezes for all players during delay
- Client freezes during cinematic sequence
- Timeouts and kicks in multiplayer

**Prevention:**
```java
// BAD: Blocks server thread
ServerPlayNetworking.registerGlobalReceiver(REVIVE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
    Thread.sleep(2000);  // NEVER DO THIS
    revivePlayer(targetPlayer);
});

// GOOD: Schedule task
ServerPlayNetworking.registerGlobalReceiver(REVIVE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
    server.execute(() -> {
        // Schedule for later execution
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                server.execute(() -> revivePlayer(targetPlayer));
            }
        }, 2000);
    });
});
```

**Detection:**
- Server stops responding during actions
- All players freeze during one player's action

**Phase Impact:** Phase 3 (Multiplayer Sync) - Caught quickly in multiplayer testing.

---

### Pitfall 13: Not Testing with Fabric API Version Mismatches
**What goes wrong:** Mod works in dev environment but crashes for players with different Fabric API version.

**Why it happens:** Dev environment uses latest Fabric API, players may have older versions. API breaking changes cause crashes.

**Consequences:**
- Crashes on player launch
- GitHub issues: "Works for developer, crashes for me"
- Bad reviews from incompatibility

**Prevention:**
- Specify minimum Fabric API version in fabric.mod.json:

```json
{
  "depends": {
    "fabricloader": ">=0.15.0",
    "fabric-api": ">=0.92.0+1.21"
  }
}
```

- Test with minimum required version before release
- Check API changelog for breaking changes before updating

**Detection:**
- Works in dev, crashes in production
- Error mentions Fabric API methods not found

**Phase Impact:** Phase 5 (Release Prep) - Version compatibility testing before release.

---

### Pitfall 14: Incorrect Death Event Handling Order
**What goes wrong:** Attempting to modify player state in death event before vanilla code executes causes race conditions, duplicate death handling, or prevention of respawn.

**Why it happens:** Multiple mods and vanilla code handle death events. Incorrect event priority or missing cancellation checks cause conflicts.

**Consequences:**
- Player can't click respawn button (PaperMC Issue #11038)
- Downed state and death both trigger, causing confusion
- Totems of Undying interaction broken
- Death loot duplicated or lost

**Prevention:**
- Use `ServerLivingEntityEvents.ALLOW_DEATH` (fires before totems):

```java
ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
    if (entity instanceof ServerPlayerEntity player) {
        // Check if should enter downed state instead of dying
        if (shouldEnterDownedState(player)) {
            enterDownedState(player);
            return false;  // Cancel death
        }
    }
    return true;  // Allow normal death
});
```

- Don't modify player in both ALLOW_DEATH and AFTER_DEATH
- Test with Totems of Undying equipped
- Test respawn button functionality

**Detection:**
- Respawn button doesn't work
- Player stuck in death screen
- Totem doesn't save player when it should

**Phase Impact:** Phase 4 (Downed State) - Death event is core mechanic, must be correct before adding complexity.

**Sources:**
- [GitHub Issue: PaperMC Respawn Bug](https://github.com/PaperMC/Paper/issues/11038)
- [Fabric API: ServerLivingEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.79.0+1.20/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html)

---

### Pitfall 15: Jump Scare Becomes "Cheap"
**What goes wrong:** Jump scare relies only on loud sound + sudden appearance without buildup, causing players to feel annoyed rather than scared. Becomes predictable after first time.

**Why it happens:** Game design mistake - treating jump scare as sudden event rather than culmination of tension building. Horror mods often focus on the "scare" without the "horror".

**Consequences:**
- Players complain scare is "cheap gimmick"
- No replay value (scare only works once)
- Comedic instead of frightening
- Negative reviews citing "loud noise simulator"

**Prevention (Game Design):**
- **Build tension first**: 30-60 seconds of subtle cues before scare
  - Distant sounds (footsteps, breathing)
  - Environmental changes (lights flicker, fog)
  - Indirect evidence (moved objects, scratches on walls)
- **Vary the timing**: Don't make scare predictable
  - Sometimes tension builds and nothing happens (false alarm)
  - Sometimes scare happens earlier than expected
- **Use proximity + time**: Scare trigger requires both being in location AND time passed
- **Audio layering**:
  - Ambient sound drops slightly (tension)
  - Heartbeat sound increases (player's fear)
  - THEN jump scare sound (release)
- **Visual staging**:
  - Entity appears in peripheral vision first (player might not notice)
  - Brief glimpse, then disappears (did I see that?)
  - THEN full appearance (confirmation + scare)

**For Dread:**
```java
// Multi-stage scare system
if (playerInArea && !hasSeenEntityBefore) {
    // Stage 1: Subtle audio (30s)
    playDistantFootsteps();

    // Stage 2: Visual hint (10s later)
    spawnEntityAtEdgeOfView(despawnAfter: 2s);

    // Stage 3: Full scare (random 20-40s later)
    if (randomTrigger && playerVulnerable) {
        executeJumpScare();
    }
}
```

**Detection:**
- Playtesters laugh instead of getting scared
- Second playthrough has no tension
- Comments like "just a loud noise"

**Phase Impact:** Phase 2 (Jump Scare Mechanics) - Design philosophy must be correct from start, retrofitting atmosphere is difficult.

**Sources:**
- [Game Developer: A Lack of Fright - Examining Jump Scare Horror Game Design](https://www.gamedeveloper.com/design/a-lack-of-fright-examining-jump-scare-horror-game-design)
- [Steam Discussion: Darkwood Jumpscares](https://steamcommunity.com/app/274520/discussions/0/1471967615871858249/)

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| **Phase 1: Core Entity** | Entity memory leak (Critical #4) | Implement `remove()` cleanup from day one |
| | Z-fighting transparency (Moderate #7) | Test with shaders during initial model creation |
| | Tracking range too small (Moderate #9) | Set tracking range 128+ in registration |
| **Phase 2: Jump Scare** | Sound channel limit (Critical #5) | Implement sound priority system before adding ambient sounds |
| | Cheap scare design (Minor #15) | Design multi-stage tension system from start |
| **Phase 3: Multiplayer Sync** | Client-server confusion (Critical #1) | All entity state MUST sync via packets |
| | Missing packet validation (Critical #2) | Validate all C2S packets server-side |
| | Hardcoded delays (Minor #12) | Use scheduled tasks, never sleep() in handlers |
| **Phase 4: Downed State** | NBT not marked dirty (Critical #3) | Call markDirty() after every state change |
| | Client-only data access (Moderate #8) | Sync downed state to clients via packets |
| | Death event order (Minor #14) | Use ALLOW_DEATH event, test with totems |
| **Phase 5: Cinematic Death** | GeckoLib performance (Moderate #10) | Keep bone count under 30, test FPS with multiple entities |
| | Rendering during freeze (Moderate) | Test camera manipulation doesn't cause nausea/disorientation |

---

## Additional Horror-Specific Warnings

### Atmosphere Destruction via Bugs
Horror relies on immersion. Technical bugs that break immersion are **10x worse** for horror mods than other genres:

- Entity despawning mid-scare: Comedic instead of scary
- Sound cutting out: Tension immediately lost
- Lighting bugs: Horror visibility relies on precise lighting
- Multiplayer desync: "I see it!" "I don't see anything" ruins group fear

**Recommendation:** Prioritize bug-free experience over feature count. One working scare > three broken scares.

### Performance = Fear
FPS drops during scares are instant immersion breakers:
- Stuttering during cinematic = not scary, just annoying
- Lag during chase = frustration, not fear
- Frame drops when entity appears = player focuses on performance, not entity

**Recommendation:** Target 60 FPS minimum during all scare sequences, even on lower-end hardware.

### Multiplayer Social Dynamics
Players experience horror differently in groups:
- Solo player: Pure fear, easier to scare
- Group of 2-3: Shared fear amplifies if working, comedy if broken
- Large group (4+): Difficult to maintain horror, becomes comedy

**Recommendation:** Design scares for 1-3 players. In larger groups, focus on atmosphere over jump scares.

---

## Testing Checklist for Pitfall Prevention

Before each phase completion:

**Phase 1 (Core Entity):**
- [ ] Entity spawns/despawns without memory leaks (run for 1 hour, check memory)
- [ ] Entity visible with Iris/Optifine shaders (no z-fighting)
- [ ] Entity doesn't despawn when player backs away
- [ ] Entity syncs correctly in multiplayer (2+ clients)

**Phase 2 (Jump Scare):**
- [ ] Sound plays reliably (test 10 scares in a row)
- [ ] Multiple ambient sounds + scare sound don't exceed channel limit
- [ ] Scare feels earned, not cheap (external playtesters required)

**Phase 3 (Multiplayer Sync):**
- [ ] Entity position syncs to all clients within tracking range
- [ ] Malformed packets don't crash server (fuzz testing)
- [ ] Client-side code never accesses server-only data

**Phase 4 (Downed State):**
- [ ] Downed state persists through server restart
- [ ] Downed state visible to all players
- [ ] Totems of Undying still work correctly
- [ ] Respawn button works after true death

**Phase 5 (Cinematic Death):**
- [ ] Cinematic runs at 60+ FPS on low-end hardware
- [ ] Camera manipulation doesn't cause motion sickness
- [ ] Cinematic syncs in multiplayer (all players see same sequence)

---

## Emergency Debugging

If critical pitfall discovered in production:

### Immediate Triage
1. **Critical bugs first**: Crashes > Desyncs > Performance > Aesthetics
2. **Scope assessment**: Single player only? Multiplayer only? Specific mod combinations?
3. **Rollback decision**: If fix requires days, consider rolling back to previous version

### Common Emergency Fixes

**"Mod crashes on dedicated server"**
- Likely: Client-server side confusion (Critical #1)
- Quick fix: Move client-only code to client entrypoint
- Test: Run on dedicated server, not just singleplayer

**"Players can't respawn"**
- Likely: Death event order (Minor #14)
- Quick fix: Check event cancellation logic
- Test: Die with totem, die without totem, check respawn button

**"Downed state lost on restart"**
- Likely: NBT not marked dirty (Critical #3)
- Quick fix: Add `markDirty()` calls after all state changes
- Test: Set downed state, save & quit, rejoin - state should persist

**"Entity disappears in multiplayer"**
- Likely: Missing entity sync packets (Critical #1)
- Quick fix: Add S2C packets for entity spawn/despawn
- Test: Two clients, spawn entity near one client, other client should see it

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Client-Server Pitfalls | **HIGH** | Verified with official Fabric documentation |
| Networking/Packets | **HIGH** | Official Fabric networking docs |
| NBT/Persistence | **HIGH** | Official Fabric persistent state docs |
| Entity Memory Leaks | **MEDIUM** | Community mod fixes, documented bug MC-260605 |
| Sound Channel Limits | **MEDIUM** | Multiple community sources, OpenAL limitations documented |
| Horror Game Design | **MEDIUM** | Game design articles, not Minecraft-specific |
| GeckoLib Performance | **MEDIUM** | Community knowledge, no official benchmarks |
| Mixin Conflicts | **MEDIUM** | Community tools and discussions |

---

## Sources

### Official Documentation (HIGH confidence)
- [Fabric Wiki: Side Tutorial](https://wiki.fabricmc.net/tutorial:side)
- [Fabric Documentation: Networking](https://docs.fabricmc.net/develop/networking)
- [Fabric Wiki: Persistent States](https://wiki.fabricmc.net/tutorial:persistent_states)
- [Fabric API: ServerLivingEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.79.0+1.20/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html)

### Community Resources (MEDIUM confidence)
- [GitHub: MemoryLeakFix](https://github.com/FxMorin/MemoryLeakFix)
- [Modrinth: Audio Engine Tweaks](https://modrinth.com/mod/audio-engine-tweaks)
- [Modrinth: GeckoLib](https://modrinth.com/mod/geckolib)
- [Modrinth: Entity Texture Features](https://modrinth.com/mod/entitytexturefeatures)
- [Modrinth: Mixin Conflict Helper](https://modrinth.com/mod/mixin-conflict-helper)
- [GitHub: Sound Physics Performance Issues](https://github.com/henkelmax/sound-physics-remastered/issues/234)
- [GitHub: PaperMC Respawn Bug](https://github.com/PaperMC/Paper/issues/11038)

### Game Design Resources (MEDIUM confidence)
- [Game Developer: Jump Scare Horror Game Design](https://www.gamedeveloper.com/design/a-lack-of-fright-examining-jump-scare-horror-game-design)
- [Steam: Darkwood Jumpscares Discussion](https://steamcommunity.com/app/274520/discussions/0/1471967615871858249/)

### Additional References
- [CurseForge: Various Fabric Mods](https://www.curseforge.com/minecraft)
- [Forge Forums: Entity/Rendering Issues](https://forums.minecraftforge.net/)
- [Minecraft Wiki: Death Mechanics](https://minecraft.wiki/w/Death)
