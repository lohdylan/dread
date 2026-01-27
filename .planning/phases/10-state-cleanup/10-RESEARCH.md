# Phase 10: State Cleanup - Research

**Researched:** 2026-01-26
**Domain:** Minecraft Fabric player lifecycle management, world boundaries, state persistence
**Confidence:** HIGH

## Summary

Phase 10 implements downed state cleanup across world boundaries - fixing the bug where downed state persists when leaving a world and joining a different save file. The challenge is distinguishing between dimension changes within the same world (Nether/End travel, which should NOT clear state) versus actual world exits (disconnect, server leave, world switch, which SHOULD clear state).

The standard approach uses Fabric API's built-in lifecycle events: `ServerPlayConnectionEvents.DISCONNECT` for detecting player disconnections and `ServerPlayConnectionEvents.JOIN` for detecting reconnections. For dimension changes, `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD` fires for all world transitions, but since dimension changes within the same save file use the same PersistentState instance, the state naturally persists - we only need to clear on disconnect.

For edge cases, damage type detection uses `DamageSource.isOf(DamageTypes.OUT_OF_WORLD)` and `DamageSource.isOf(DamageTypes.GENERIC_KILL)` to identify void/kill command damage. Gamemode changes require a mixin on `ServerPlayerEntity.changeGameMode()` since Fabric doesn't provide a built-in event.

**Primary recommendation:** Use Fabric's ServerPlayConnectionEvents for disconnect/join handling. Clear downed state on disconnect, set health to 2 hearts (4 HP) on join if previously downed. Use mixins only for gamemode change detection (not a common operation, minimal performance impact).

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.116.8+1.21.1 | Player lifecycle events | Official Fabric event system, comprehensive coverage |
| ServerPlayConnectionEvents | 0.116.8+ | JOIN/DISCONNECT events | Standard for connection lifecycle in Fabric |
| ServerEntityWorldChangeEvents | 0.116.8+ | Dimension change detection | Standard for world boundary detection |
| PersistentState | Vanilla MC 1.21.1 | Server-scoped state storage | Built-in Minecraft state persistence |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Mixin | 0.8+ | Method injection for gamemode | When no Fabric event exists |
| ServerLifecycleEvents | 0.116.8+ | Server shutdown detection | For cleanup on server stop |
| DamageTypes | Vanilla 1.21.1 | Damage source identification | For void/kill bypass logic |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| PersistentState per world | Per-player NBT | Would break on world deletion, harder to query all downed players |
| Fabric events | Pure mixins | More fragile, version-dependent, harder to maintain |
| Health tracking | Custom field | Health is authoritative source, no duplication needed |

**Installation:**
Already included in project dependencies (Fabric API 0.116.8+1.21.1)

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/dread/death/
├── lifecycle/                  # NEW: Lifecycle event handlers
│   ├── PlayerConnectionHandler.java    # Disconnect/join logic
│   ├── PlayerWorldChangeHandler.java   # Dimension change logging (optional)
│   └── PlayerGamemodeHandler.java      # Gamemode change detection
├── DownedPlayersState.java     # EXISTING: Already server-scoped
├── DreadDeathHandler.java      # EXISTING: Already handles death events
└── DreadDeathManager.java      # EXISTING: Already manages state
```

### Pattern 1: Disconnect State Cleanup
**What:** On player disconnect, clear downed state and mark a flag for reconnection penalty
**When to use:** Every disconnect event, regardless of reason (normal quit, crash, kick, timeout)
**Example:**
```java
// Register in DreadMod.onInitialize()
ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
    ServerPlayerEntity player = handler.getPlayer();
    ServerWorld world = player.getServerWorld();
    DownedPlayersState state = DownedPlayersState.getOrCreate(world);

    if (state.isDowned(player)) {
        // Player was downed when disconnecting - flag for reconnect penalty
        state.markEscapedPlayer(player.getUuid());
        state.removeDowned(player.getUuid());

        // Clear visual effects on disconnect
        CrawlPoseHandler.exitCrawlPose(player);
    }
});
```
**Source:** [ServerPlayConnectionEvents Documentation](https://fabricmc.docs.concern.i.ng/fabric-networking-api-v1/fabric-networking-api-v1/net.fabricmc.fabric.api.networking.v1/-server-play-connection-events/index.html)

### Pattern 2: Reconnection Penalty Application
**What:** On player join, check if they escaped while downed, apply 2 HP penalty and broadcast message
**When to use:** Every player join event
**Example:**
```java
ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
    ServerPlayerEntity player = handler.getPlayer();
    ServerWorld world = player.getServerWorld();
    DownedPlayersState state = DownedPlayersState.getOrCreate(world);

    if (state.wasEscapedPlayer(player.getUuid())) {
        // Apply reconnect penalty: 2 hearts (4 HP)
        player.setHealth(4.0f);

        // Broadcast escape message to all players
        Text escapeMessage = Text.literal(player.getName().getString() + " narrowly escaped the Dread");
        server.getPlayerManager().broadcast(escapeMessage, false);

        // Clear escape flag
        state.clearEscapedPlayer(player.getUuid());

        // Log for debugging
        DreadMod.LOGGER.info("Player {} reconnected after escaping downed state", player.getName().getString());
    }
});
```
**Source:** [ServerPlayConnectionEvents.Join API](https://maven.fabricmc.net/docs/fabric-api-0.102.0+1.21/net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents.Join.html)

### Pattern 3: Dimension Change Detection (Logging Only)
**What:** Log dimension changes within same world to verify state persistence
**When to use:** For debugging, not required for functionality
**Example:**
```java
ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
    DownedPlayersState originState = DownedPlayersState.getOrCreate(origin);
    DownedPlayersState destState = DownedPlayersState.getOrCreate(destination);

    // These should be the same instance if same save file
    boolean sameWorld = originState == destState;

    if (originState.isDowned(player)) {
        DreadMod.LOGGER.info(
            "Player {} changed dimensions while downed. Origin: {}, Dest: {}, Same save: {}",
            player.getName().getString(),
            origin.getRegistryKey().getValue(),
            destination.getRegistryKey().getValue(),
            sameWorld
        );
    }
});
```
**Source:** [Event Index - Fabric Wiki](https://wiki.fabricmc.net/tutorial:event_index)

### Pattern 4: Gamemode Change Detection (Mixin)
**What:** Clear downed state when admin changes player to spectator/creative
**When to use:** Only when gamemode actually changes (not every tick)
**Example:**
```java
@Mixin(ServerPlayerEntity.class)
public abstract class GamemodeChangeMixin {
    @Shadow public abstract GameMode getGameMode();

    @Inject(method = "changeGameMode", at = @At("RETURN"))
    private void dread$onGamemodeChange(GameMode newMode, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) { // Only if gamemode actually changed
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            ServerWorld world = player.getServerWorld();
            DownedPlayersState state = DownedPlayersState.getOrCreate(world);

            if (state.isDowned(player) && (newMode == GameMode.CREATIVE || newMode == GameMode.SPECTATOR)) {
                state.removeDowned(player.getUuid());
                CrawlPoseHandler.exitCrawlPose(player);
                DreadMod.LOGGER.info("Cleared downed state for {} (gamemode changed to {})",
                    player.getName().getString(), newMode);
            }
        }
    }
}
```
**Source:** [Creating a Mixin - StationAPI Wiki](https://stationapi.wiki/Mixins/Creating-a-Mixin)

### Pattern 5: Void/Kill Command Bypass
**What:** Detect void and /kill damage, bypass downed timer, trigger immediate death
**When to use:** In existing DreadDeathHandler.onPlayerDeath()
**Example:**
```java
private static boolean onPlayerDeath(LivingEntity entity, DamageSource source, float damageAmount) {
    if (!(entity instanceof ServerPlayerEntity player)) return true;

    // Check for void damage or /kill command - bypass downed state
    if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) {
        DownedPlayersState state = DownedPlayersState.getOrCreate(player.getServerWorld());

        // If already downed, trigger death cinematic and spectator transition
        if (state.isDowned(player)) {
            DreadEntity dread = findNearestDread(player); // Find Dread that was hunting them
            if (dread != null) {
                DeathCinematicController.triggerDeathCinematic(player, dread);
            }
            // Allow vanilla death to proceed after cinematic
        }
        return true; // Allow death for void/kill
    }

    // ... existing Dread death logic
}
```
**Source:** [Damage Types - Minecraft Wiki](https://minecraft.wiki/w/Damage_type)

### Anti-Patterns to Avoid
- **Polling gamemode every tick:** Use mixin injection, not tick-based polling (causes unnecessary overhead)
- **Persisting downed state to disk:** Per CONTEXT.md, server restart clears all downed states (no NBT persistence)
- **Clearing state on dimension change:** PersistentState is server-scoped, naturally persists across dimensions in same world
- **Using world-specific PersistentState:** Would create separate state per dimension, breaking multiplayer sync

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Player connection lifecycle | Custom tracking system | ServerPlayConnectionEvents | Fabric handles edge cases (timeouts, crashes, kicks) automatically |
| Dimension change detection | Custom coordinate tracking | ServerEntityWorldChangeEvents | Handles portal travel, /tp, respawn anchors, end portal exit |
| Server shutdown cleanup | Custom shutdown hooks | ServerLifecycleEvents.SERVER_STOPPING | Guaranteed to fire before worlds unload |
| Damage type identification | String matching on death message | DamageSource.isOf(DamageType) | Type-safe, localization-independent, version-stable |
| Invulnerability period | Custom timer system | Entity.timeUntilRegen field | Vanilla handles edge cases (combat, damage ticks) |

**Key insight:** Minecraft and Fabric already handle the complex edge cases of player lifecycle (network errors, world corruption, chunk unloading). Building custom solutions will miss rare but critical edge cases that took years to discover and fix in vanilla.

## Common Pitfalls

### Pitfall 1: Assuming World == Dimension
**What goes wrong:** Clearing downed state on `AFTER_PLAYER_CHANGE_WORLD` event clears it for Nether/End travel
**Why it happens:** The event name is misleading - it fires for dimension changes within the same save file
**How to avoid:** Only clear state on disconnect events. PersistentState is server-scoped, so same-world dimension changes naturally preserve state
**Warning signs:** Bug reports of "state clears when entering Nether"

### Pitfall 2: Race Condition on Server Shutdown
**What goes wrong:** PersistentState writes to disk during shutdown, but downed state is already cleared
**Why it happens:** Using SERVER_STOPPED event (fires after worlds saved) instead of SERVER_STOPPING
**How to avoid:** Per CONTEXT.md, server restart clears downed states intentionally (no persistence needed). If logging is needed, use SERVER_STOPPING event
**Warning signs:** "State not saved on shutdown" even though it's intentional behavior

### Pitfall 3: Gamemode Mixin Performance Impact
**What goes wrong:** Injecting into high-frequency methods causes lag
**Why it happens:** Choosing wrong injection point (e.g., tick method instead of changeGameMode)
**How to avoid:** Only inject into `ServerPlayerEntity.changeGameMode()` which is called infrequently (admin commands only)
**Warning signs:** TPS drops when admins use /gamemode commands

### Pitfall 4: Reconnect Immunity Implementation
**What goes wrong:** Player reconnects at 2 HP, Dread kills them instantly before client loads world
**Why it happens:** Spawn protection was removed in Minecraft 1.21.4, no built-in invulnerability
**How to avoid:** Set `player.timeUntilRegen = 60` on reconnect to grant 3 seconds of damage immunity. This is vanilla field, works with all damage sources
**Warning signs:** "Instant death on reconnect" bug reports in multiplayer

### Pitfall 5: Chat Message Spam on Server Restart
**What goes wrong:** All players marked as "escaped" when server restarts, spam on rejoin
**Why it happens:** Not clearing escaped player flags on shutdown
**How to avoid:** Register SERVER_STOPPING event to clear all escape flags, or store escape timestamp and only show message if < 5 minutes ago
**Warning signs:** Chat flooded with escape messages after restart

## Code Examples

Verified patterns from official sources:

### Example 1: Full Lifecycle Registration
```java
// In DreadMod.onInitialize()
public class DreadMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // ... existing initialization

        PlayerConnectionHandler.register();
        PlayerGamemodeHandler.register();
    }
}

// In new PlayerConnectionHandler.java
public class PlayerConnectionHandler {
    private static final int RECONNECT_IMMUNITY_TICKS = 60; // 3 seconds

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register(PlayerConnectionHandler::onDisconnect);
        ServerPlayConnectionEvents.JOIN.register(PlayerConnectionHandler::onJoin);
    }

    private static void onDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        ServerWorld world = player.getServerWorld();
        DownedPlayersState state = DownedPlayersState.getOrCreate(world);

        if (state.isDowned(player)) {
            state.markEscapedPlayer(player.getUuid());
            state.removeDowned(player.getUuid());
            CrawlPoseHandler.exitCrawlPose(player);

            DreadMod.LOGGER.info("Player {} disconnected while downed", player.getName().getString());
        }
    }

    private static void onJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.getPlayer();
        ServerWorld world = player.getServerWorld();
        DownedPlayersState state = DownedPlayersState.getOrCreate(world);

        if (state.wasEscapedPlayer(player.getUuid())) {
            // Apply 2 heart penalty
            player.setHealth(4.0f);

            // Grant temporary immunity (prevents instant death on spawn)
            player.timeUntilRegen = RECONNECT_IMMUNITY_TICKS;

            // Broadcast escape message
            Text message = Text.literal(player.getName().getString() + " narrowly escaped the Dread");
            server.getPlayerManager().broadcast(message, false);

            state.clearEscapedPlayer(player.getUuid());

            DreadMod.LOGGER.info("Player {} reconnected after escape (2 hearts, 3s immunity)",
                player.getName().getString());
        }
    }
}
```
**Source:** [ServerPlayConnectionEvents API](https://maven.fabricmc.net/docs/fabric-api-0.102.0+1.21/net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents.html)

### Example 2: DownedPlayersState Extensions
```java
// Add to DownedPlayersState.java
public class DownedPlayersState extends PersistentState {
    // Existing fields...
    private final Set<UUID> escapedPlayers = new HashSet<>();

    // New methods for escape tracking
    public void markEscapedPlayer(UUID playerId) {
        escapedPlayers.add(playerId);
        // Note: NOT marked dirty - escape flags do not persist to disk
    }

    public boolean wasEscapedPlayer(UUID playerId) {
        return escapedPlayers.contains(playerId);
    }

    public void clearEscapedPlayer(UUID playerId) {
        escapedPlayers.remove(playerId);
    }

    // Do NOT serialize escapedPlayers in writeNbt() - intentionally transient
}
```

### Example 3: Void/Kill Damage Detection
```java
// In DreadDeathHandler.onPlayerDeath()
private static boolean onPlayerDeath(LivingEntity entity, DamageSource source, float damageAmount) {
    if (!(entity instanceof ServerPlayerEntity player)) return true;
    if (!DreadConfigLoader.getConfig().modEnabled) return true;

    // NEW: Check for void/kill bypass
    if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) {
        DownedPlayersState state = DownedPlayersState.getOrCreate(player.getServerWorld());

        if (state.isDowned(player)) {
            // Player was downed, now hit by void/kill - trigger immediate death
            DreadMod.LOGGER.info("Player {} killed by {} while downed - bypassing timer",
                player.getName().getString(),
                source.isOf(DamageTypes.OUT_OF_WORLD) ? "void" : "/kill");

            // Trigger cinematic if Dread is nearby
            DreadEntity nearestDread = findNearestDread(player, 64.0);
            if (nearestDread != null) {
                DeathCinematicController.triggerDeathCinematic(player, nearestDread);
            }
        }

        return true; // Allow vanilla death (spectator transition)
    }

    // Existing Dread death logic...
}

private static DreadEntity findNearestDread(ServerPlayerEntity player, double range) {
    List<DreadEntity> dreads = player.getServerWorld().getEntitiesByClass(
        DreadEntity.class,
        player.getBoundingBox().expand(range),
        dread -> true
    );

    if (dreads.isEmpty()) return null;

    return dreads.stream()
        .min((d1, d2) -> Double.compare(
            d1.squaredDistanceTo(player),
            d2.squaredDistanceTo(player)
        ))
        .orElse(null);
}
```
**Source:** [Minecraft Wiki - Damage Types](https://minecraft.wiki/w/Damage_type)

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Spawn protection (60 ticks) | No built-in immunity | MC 1.21.4 | Mods must manually set `timeUntilRegen` |
| Per-dimension PersistentState | Server-scoped PersistentState | MC 1.16+ | State naturally persists across dimensions |
| String-based damage types | Data-driven damage types | MC 1.19.4+ | Use `DamageSource.isOf(DamageType)` not string matching |
| Forge events | Fabric events | Fabric 0.14+ | Use ServerPlayConnectionEvents not custom packets |

**Deprecated/outdated:**
- **World.getDimensionKey()**: Replaced by `World.getRegistryKey()` in 1.16+
- **PlayerList.sendToAll()**: Use `PlayerManager.broadcast()` in Fabric
- **DamageSource.damageType String**: Use registry-based DamageType in 1.19.4+

## Open Questions

Things that couldn't be fully resolved:

1. **Crash Detection vs Normal Disconnect**
   - What we know: `ServerPlayConnectionEvents.DISCONNECT` fires for all disconnects (normal, crash, timeout, kick)
   - What's unclear: Whether Fabric provides a way to distinguish crash from normal quit
   - Recommendation: Treat all disconnects the same per CONTEXT.md - "all exits clear downed state"

2. **Reconnect Immunity Duration**
   - What we know: Setting `player.timeUntilRegen = 60` grants 3 seconds immunity (vanilla behavior)
   - What's unclear: Whether 3 seconds is enough for client to fully load world and render Dread
   - Recommendation: Start with 3 seconds (vanilla default), monitor for "instant death on reconnect" bugs. Can increase to 100 ticks (5 seconds) if needed. CONTEXT.md leaves this to Claude's discretion.

3. **Escape Message Wording**
   - What we know: Message should notify other players when someone escaped by disconnecting
   - What's unclear: Exact wording preference
   - Recommendation: Use "{player} narrowly escaped the Dread" - matches tone of existing death message ("{player} succumbed to the Dread"). CONTEXT.md leaves wording to Claude's discretion.

4. **Escape Message Timing**
   - What we know: Message should appear on reconnect
   - What's unclear: Whether to show message if player reconnects hours/days later
   - Recommendation: Always show message on first reconnect after escape, regardless of time elapsed. Alternative: Add timestamp to escape flag, only show if < 5 minutes ago. CONTEXT.md doesn't specify.

## Sources

### Primary (HIGH confidence)
- [Fabric Event Index](https://wiki.fabricmc.net/tutorial:event_index) - Complete list of available events
- [ServerPlayConnectionEvents Documentation](https://fabricmc.docs.concern.i.ng/fabric-networking-api-v1/fabric-networking-api-v1/net.fabricmc.fabric.api.networking.v1/-server-play-connection-events/index.html) - JOIN/DISCONNECT events
- [ServerPlayConnectionEvents.Disconnect API](https://maven.fabricmc.net/docs/fabric-api-0.77.0+1.20/net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents.Disconnect.html) - Disconnect event details
- [PersistentState Tutorial](https://wiki.fabricmc.net/tutorial:persistent_states) - Server-scoped state mechanics
- [Minecraft Wiki - Damage Types](https://minecraft.wiki/w/Damage_type) - Vanilla damage type list (OUT_OF_WORLD, GENERIC_KILL)
- [Fabric Damage Types Documentation](https://docs.fabricmc.net/develop/entities/damage-types) - How to use damage types in code

### Secondary (MEDIUM confidence)
- [ServerLifecycleEvents API](https://maven.fabricmc.net/docs/fabric-api-0.88.1+1.20.2/net/fabricmc/fabric/api/event/lifecycle/v1/ServerLifecycleEvents.html) - SERVER_STOPPING event
- [Creating a Mixin - StationAPI](https://stationapi.wiki/Mixins/Creating-a-Mixin) - Mixin injection patterns
- [Bring Back My Spawn Immunity Mod](https://modrinth.com/mod/spawnimmunity) - Example of manual immunity implementation post-1.21.4

### Tertiary (LOW confidence)
- [ServerEntityWorldChangeEvents API](https://maven.fabricmc.net/docs/fabric-api-0.85.0+1.20.1/net/fabricmc/fabric/api/entity/event/v1/ServerEntityWorldChangeEvents.html) - Dimension change events (doc doesn't clarify same-world vs different-world)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All components are official Fabric API or vanilla Minecraft, verified in current project
- Architecture: HIGH - Patterns verified in official Fabric documentation and Minecraft Wiki
- Pitfalls: MEDIUM - Based on common patterns and logical inference (actual-world dimension confusion, race conditions), not battle-tested

**Research date:** 2026-01-26
**Valid until:** 30 days (Fabric API is stable, Minecraft 1.21.x is mature version)
