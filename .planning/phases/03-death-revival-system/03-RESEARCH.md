# Phase 3: Death & Revival System - Research

**Researched:** 2026-01-24
**Domain:** Minecraft Fabric player death mechanics, client-server networking, camera manipulation, shader effects
**Confidence:** MEDIUM

## Summary

Phase 3 implements a multi-stage death system with cinematic sequences, downed state mechanics, and cooperative revival. The implementation spans five major technical domains: (1) death event interception using Fabric's ServerLivingEntityEvents.ALLOW_DEATH, (2) camera manipulation via MinecraftClient.setCameraEntity for forced viewpoint control, (3) post-processing shaders using Satin API for blur/vignette effects, (4) client-server packet synchronization for multiplayer revival mechanics, and (5) HUD overlay rendering for countdown timers and progress bars.

The standard approach involves canceling vanilla death, transitioning players to a custom "downed" state tracked server-side, applying visual effects client-side via shaders and HUD overlays, and implementing revival as a proximity-based interaction with uninterruptible progress tracking. Critical challenges include handling Totem of Undying event ordering, network synchronization for camera control, and maintaining consistent state across client-server boundaries.

**Primary recommendation:** Use Fabric's ServerLivingEntityEvents.ALLOW_DEATH for death interception (fires before totems), Satin API for post-processing blur effects, custom networking packets for client-side camera/shader triggers, and HUD API for timer/progress overlays. Track downed state in persistent world data similar to existing SpawnProbabilityState pattern.

## Standard Stack

The established libraries/tools for this domain:

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.116.8 | Death events, networking, HUD rendering | Official Fabric modding API, provides ServerLivingEntityEvents and networking framework |
| Satin API | 4.7.1+ | Post-processing shaders (blur/vignette) | De facto standard for shader effects in Fabric mods, handles screen effects with automatic resolution/reload management |
| GeckoLib | 4.7.1 | Entity animations (already in project) | Used for Dread entity death animations |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Minecraft Core Shaders | Built-in | Alternative shader approach | If avoiding Satin dependency, but more complex to implement |
| Fabric Rendering v1 | Part of Fabric API | HUD overlay rendering | For countdown timers and progress bars above entities |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Satin API | Manual post-processing shaders | More control but significantly more complex, must handle resolution changes and resource reloading manually |
| Custom packets | Vanilla game rules/NBT | Won't work for client-side effects (camera, shaders) that require immediate client response |
| ServerLivingEntityEvents | PlayerEntity.damage() mixins | More fragile, breaks with other mods, event API is compatibility-focused |

**Installation:**
```gradle
// Already in project: Fabric API 0.116.8, GeckoLib 4.7.1
// Add to build.gradle dependencies:
modImplementation "maven.modrinth:satin:mc1.21-1.17.0"
```

## Architecture Patterns

### Recommended Project Structure

```
src/main/java/com/dread/
├── death/
│   ├── DreadDeathManager.java          # Coordinates death/revival flow
│   ├── DownedPlayerState.java          # Per-player downed state tracking
│   └── RevivalInteractionHandler.java  # Proximity-based revival logic
├── cinematic/
│   ├── DeathCinematicController.java   # Camera lock + entity positioning
│   └── CinematicSequence.java          # Timing and animation coordination
├── network/
│   ├── packets/
│   │   ├── CinematicTriggerS2C.java    # Trigger death cinematic on client
│   │   ├── DownedStateUpdateS2C.java   # Sync downed state to client
│   │   └── ReviveRequestC2S.java       # Client requests to revive player
│   └── DreadNetworking.java            # Packet registration
├── client/
│   ├── DownedStateRenderer.java        # Blur/vignette shader management
│   ├── DownedHudOverlay.java           # Timer countdown display
│   └── RevivalProgressOverlay.java     # Progress bar above downed player
└── state/
    └── DownedPlayersState.java         # Persistent world data (server)
```

### Pattern 1: Death Event Interception

**What:** Cancel vanilla death and transition to downed state before Totem of Undying triggers
**When to use:** ServerLivingEntityEvents.ALLOW_DEATH fires before totem processing, giving mods priority
**Example:**
```java
// Source: Official Fabric API Documentation
// https://docs.fabricmc.net/develop/events

ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
    if (entity instanceof ServerPlayer player) {
        // Cancel death, transition to downed state
        // IMPORTANT: Must restore health or player dies next tick
        player.setHealth(1.0f);

        DownedPlayersState state = DownedPlayersState.get(player.serverLevel());
        state.setDowned(player, 300); // 300 second timer

        // Trigger cinematic on client
        ServerPlayNetworking.send(player, new CinematicTriggerS2CPayload(
            /* dread entity ID, player death position */
        ));

        return false; // Cancel death
    }
    return true; // Allow death for non-players
});
```

### Pattern 2: Client-Server Packet Synchronization

**What:** Use custom packets to trigger client-side effects (camera, shaders) and send interaction events to server
**When to use:** Any time client state needs to change based on server events, or server needs client input
**Example:**
```java
// Source: Fabric Networking Documentation
// https://docs.fabricmc.net/develop/networking

// S2C Packet - Trigger death cinematic on client
public record CinematicTriggerS2CPayload(int dreadEntityId, BlockPos deathPos)
    implements CustomPayload {

    public static final CustomPayload.Type<CinematicTriggerS2CPayload> ID =
        new CustomPayload.Type<>(Identifier.fromNamespaceAndPath("dread", "cinematic_trigger"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CinematicTriggerS2CPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CinematicTriggerS2CPayload::dreadEntityId,
            BlockPos.STREAM_CODEC, CinematicTriggerS2CPayload::deathPos,
            CinematicTriggerS2CPayload::new
        );

    @Override
    public Type<? extends CustomPayload> type() { return ID; }
}

// Client receiver registration (in DreadClient.java)
ClientPlayNetworking.registerGlobalReceiver(CinematicTriggerS2CPayload.ID,
    (payload, context) -> {
        ClientLevel level = context.client().level;
        Entity dread = level.getEntity(payload.dreadEntityId());

        // Lock camera to Dread entity
        context.client().setCameraEntity(dread);

        // Apply blur/vignette shader
        DownedStateRenderer.applyDownedEffects();
    });

// C2S Packet - Player wants to revive downed teammate
public record ReviveRequestC2SPayload(UUID downedPlayerUUID)
    implements CustomPayload { /* ... */ }

// Server receiver registration (in DreadMod.java)
ServerPlayNetworking.registerGlobalReceiver(ReviveRequestC2SPayload.ID,
    (payload, context) -> {
        ServerPlayer reviverPlayer = context.player();
        ServerPlayer downedPlayer = context.player().serverLevel()
            .getPlayerByUUID(payload.downedPlayerUUID());

        // Validate range (3-4 blocks), start revival
        if (downedPlayer != null &&
            reviverPlayer.distanceTo(downedPlayer) <= 4.0) {
            // Start uninterruptible 3-second revival
            DreadDeathManager.startRevival(reviverPlayer, downedPlayer);
        }
    });
```

### Pattern 3: HUD Overlay Rendering

**What:** Render countdown timer and revival progress bars using Fabric HUD API
**When to use:** For any on-screen UI elements that need to update dynamically
**Example:**
```java
// Source: Fabric HUD Rendering Documentation
// https://docs.fabricmc.net/develop/rendering/hud

public class DownedHudOverlay implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.VIGNETTE,
            Identifier.fromNamespaceAndPath("dread", "downed_timer"),
            DownedHudOverlay::renderTimer);
    }

    private static void renderTimer(GuiGraphics context, DeltaTracker tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Check if local player is downed (from client-side state)
        DownedClientState state = DownedClientState.getInstance();
        if (!state.isLocalPlayerDowned()) return;

        int remainingSeconds = state.getRemainingSeconds();

        // Render centered countdown timer
        String timerText = String.format("%d:%02d",
            remainingSeconds / 60, remainingSeconds % 60);
        int textWidth = client.font.width(timerText);
        int x = (context.guiWidth() - textWidth) / 2;
        int y = context.guiHeight() - 60;

        // Interpolate color from yellow to red as time runs out
        float timeRatio = remainingSeconds / 300.0f;
        int color = ARGB.linearLerp(1.0f - timeRatio, 0xFFFF0000, 0xFFFFFF00);

        context.drawString(client.font, timerText, x, y, color);
    }
}
```

### Pattern 4: Post-Processing Shaders with Satin

**What:** Apply blur and vignette effects to screen when player is downed
**When to use:** For full-screen visual effects that process the rendered frame
**Example:**
```java
// Source: Satin API GitHub - https://github.com/Ladysnake/Satin

public class DownedStateRenderer {
    private static ManagedShaderEffect downedShader;

    public static void initialize() {
        // Lazily initialized, auto-reloads on resolution change
        downedShader = ShaderEffectManager.getInstance().manage(
            Identifier.fromNamespaceAndPath("dread", "shaders/post/downed_effect.json")
        );
    }

    public static void applyDownedEffects() {
        if (downedShader != null) {
            // Set shader uniforms (blur radius, vignette intensity)
            downedShader.setUniformValue("BlurRadius", 15.0f);
            downedShader.setUniformValue("VignetteIntensity", 0.8f);
            downedShader.setEnabled(true);
        }
    }

    public static void removeDownedEffects() {
        if (downedShader != null) {
            downedShader.setEnabled(false);
        }
    }
}

// Shader JSON: assets/dread/shaders/post/downed_effect.json
// Based on vanilla blur.json with added vignette pass
// Source: Minecraft vanilla shaders + Satin documentation
```

### Pattern 5: Camera Entity Control

**What:** Lock player camera to Dread entity during death cinematic
**When to use:** For forced camera perspectives during cutscenes
**Example:**
```java
// Source: Fabric mod examples (Camera Utils, Locked On)
// https://github.com/henkelmax/camera-utils

// Client-side only - triggered by S2C packet
public class DeathCinematicController {
    private Entity originalCameraEntity;
    private int cinematicTicks = 0;

    public void startCinematic(Entity dreadEntity) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Save original camera
        originalCameraEntity = client.getCameraEntity();

        // Lock to Dread
        client.setCameraEntity(dreadEntity);

        cinematicTicks = 0;
    }

    public void tick() {
        cinematicTicks++;

        // Cinematic duration: 4-5 seconds (80-100 ticks)
        if (cinematicTicks >= 90) {
            endCinematic();
        }
    }

    public void endCinematic() {
        MinecraftClient client = MinecraftClient.getInstance();

        // Restore camera to player
        client.setCameraEntity(originalCameraEntity);

        // Transition to downed state visuals
        DownedStateRenderer.applyDownedEffects();
    }
}
```

### Pattern 6: Proximity-Based Interaction Detection

**What:** Detect when a player crouches near a downed player to initiate revival
**When to use:** For interaction mechanics based on player position and state
**Example:**
```java
// Server-side tick in DreadDeathManager
public void tick(ServerLevel level) {
    DownedPlayersState state = DownedPlayersState.get(level);

    for (ServerPlayer downedPlayer : state.getDownedPlayers()) {
        // Update countdown timer
        int remaining = state.decrementTimer(downedPlayer);

        if (remaining <= 0) {
            // Timer expired - transition to spectator
            downedPlayer.setGameMode(GameMode.SPECTATOR);
            level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(downedPlayer.getName().getString() +
                    " has been claimed by the Dread"),
                false
            );
            state.removeDowned(downedPlayer);
            continue;
        }

        // Check for nearby revivers
        List<ServerPlayer> nearbyPlayers = level.getPlayers(player ->
            player != downedPlayer &&
            player.distanceTo(downedPlayer) <= 4.0 &&
            player.isCrouching()
        );

        for (ServerPlayer reviver : nearbyPlayers) {
            if (!state.isBeingRevived(downedPlayer)) {
                startRevival(reviver, downedPlayer);
                break; // Only one reviver at a time
            }
        }
    }

    // Update ongoing revivals
    updateRevivals(level);
}

private void startRevival(ServerPlayer reviver, ServerPlayer downed) {
    // 3-second uninterruptible revival
    state.startRevival(downed, reviver, 60); // 60 ticks = 3 seconds

    // Send progress bar update to all nearby players
    for (ServerPlayer nearby : level.getPlayers(p -> p.distanceTo(downed) <= 16.0)) {
        ServerPlayNetworking.send(nearby,
            new RevivalProgressS2CPayload(downed.getUUID(), true, 0.0f));
    }
}
```

### Anti-Patterns to Avoid

- **Direct health modification without state tracking:** Setting player health to 0.5 hearts without server-side downed state will cause desync and confusion
- **Client-side state as source of truth:** Camera locks and shaders are client-only, but revival decisions must be server-authoritative
- **Blocking the main thread for timers:** Use tick-based countdown in server tick loop, not Thread.sleep() or scheduled tasks
- **Forgetting to clean up camera entity:** Always restore camera to player on disconnect, respawn, or revival - memory leak otherwise
- **Hardcoding entity references:** Dread entity may despawn or be invalid - always check entity existence before setCameraEntity()

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Screen blur/vignette effects | Custom OpenGL shader pipeline | Satin API ManagedShaderEffect | Handles resolution changes, resource reloading, uniform management, and graceful degradation automatically |
| Death event handling | PlayerEntity.damage() mixins | ServerLivingEntityEvents.ALLOW_DEATH | Fabric event fires before totems, provides compatibility with other mods, cleaner than mixins |
| Client-server sync | Custom socket connections or NBT files | Fabric Networking API (CustomPayload) | Type-safe, automatic serialization, handles packet lifecycle, validates on server |
| HUD countdown timer | Manual GL calls in render loop | Fabric HUD API with DeltaTracker | Handles layering, z-ordering, and integrates with vanilla HUD system properly |
| Proximity detection | Manual distance calculation every tick for all players | Level.getPlayers(Predicate) with distance filter | Optimized and cached by Minecraft, only iterates nearby entities |
| Player pose manipulation | Packet spoofing or reflection | EntityPose enum + setPose() | Official API, networked automatically, handles hitbox changes |

**Key insight:** Minecraft and Fabric already handle the "hard parts" of multiplayer synchronization, shader management, and event ordering. Custom implementations will miss edge cases like player disconnection during revival, totem interactions, or resolution changes during shader effects.

## Common Pitfalls

### Pitfall 1: Death Event Ordering with Totem of Undying

**What goes wrong:** ServerLivingEntityEvents.ALLOW_DEATH fires BEFORE totem processing. If you cancel death (return false) but the player has a totem, they keep the totem and bypass the downed state entirely.

**Why it happens:** Event ordering: ALLOW_DEATH → (if allowed) Totem check → (if no totem) actual death. Canceling at ALLOW_DEATH prevents totem consumption.

**How to avoid:**
- Explicitly check for and consume totems before canceling death
- Or allow death if totem is present, only intercept when no totem available
- Track totem usage with a mixin on TotemItem.use() if you want totems to work differently

**Warning signs:** Players report "wasting" totems when they still get downed, or totems preventing downed state when they shouldn't

### Pitfall 2: Camera Entity Desyncs in Multiplayer

**What goes wrong:** Camera control is CLIENT-SIDE ONLY. Calling setCameraEntity() on server does nothing. If server logic depends on camera state, it will fail.

**Why it happens:** MinecraftClient is a client-only class. Server has no concept of camera - it tracks player position/rotation but not viewport.

**How to avoid:**
- Always send S2C packet to trigger camera changes on client
- Never make gameplay decisions based on camera state
- Store cinematic state server-side separately from camera (e.g., "in cinematic" boolean flag)
- Handle edge case: player disconnects during cinematic → need to reset their state on reconnect

**Warning signs:** NullPointerException on dedicated servers when accessing MinecraftClient, camera locks not working for other players in multiplayer

### Pitfall 3: Shader State Persistence After Death

**What goes wrong:** Blur/vignette shaders remain active after player respawns or disconnects, making game unplayable.

**Why it happens:** Shaders are applied globally to the client viewport. If not explicitly disabled, they persist across game states.

**How to avoid:**
- Register event listener for player respawn/disconnect to call removeDownedEffects()
- Use try-finally pattern when enabling shaders
- Check shader state on client login to clear any stale effects
- Add keybind for emergency shader clear during development

**Warning signs:** Screen stays blurred after revival, players report "can't see" on respawn, shader effects stack on multiple deaths

### Pitfall 4: Revival Progress Desync

**What goes wrong:** Client shows 100% revival progress but server hasn't completed revival, or vice versa. Player expects revival but dies to timer expiration.

**Why it happens:** Network latency between S2C progress updates and actual server state. If packets are lost or delayed, client state diverges.

**How to avoid:**
- Make server state authoritative - only server decides when revival completes
- Send progress updates every tick (60 per revival) rather than only on start/complete
- Client should interpolate progress smoothly but always defer to server for completion
- Use sequence numbers in packets to detect out-of-order delivery

**Warning signs:** Players report "revival finished but I still died" or progress bar jumps/teleports rather than smooth fill

### Pitfall 5: Crawl Movement Speed Edge Cases

**What goes wrong:** Downed players move at full speed by jumping, or movement completely stops in certain blocks (water, soul sand, etc.).

**Why it happens:** EntityAttributeModifier applies to walking speed, but vanilla movement has many special cases (jumping, swimming, climbing, etc.).

**How to avoid:**
- Apply movement modifier with MULTIPLY_TOTAL operation (applies after all other modifiers)
- Set sneaking_speed attribute (controls both sneak and crawl speed)
- Explicitly disable jumping with mixin or by setting jump boost to -10 (negative prevents jumping)
- Test in water, lava, scaffolding, ladders, and soul sand

**Warning signs:** Downed players bunny-hopping at normal speed, players stuck unable to move, movement speed inconsistent across terrain

### Pitfall 6: Spectator Mode Visibility Conflicts

**What goes wrong:** Permanent spectators see game UI incorrectly, or living players can see spectators when they shouldn't.

**Why it happens:** Spectator mode has specific vanilla behaviors for UI, entity rendering, and collision that may conflict with mod expectations.

**How to avoid:**
- Test spectator transition thoroughly - check hotbar, health display, entity glows, chunk loading
- Decision from CONTEXT.md: "Spectators CAN see Dread when living players cannot" - requires custom rendering logic
- May need mixin in DreadEntity.shouldRender() to check if viewer is spectator
- Ensure spectators don't trigger spawn checks or AI targeting

**Warning signs:** Spectators see UI elements meant for living players, Dread appears/disappears incorrectly, spectators affect gameplay

## Code Examples

Verified patterns from official sources:

### Entity Attribute Modifier for Crawl Speed

```java
// Source: Fabric Entity Attributes Documentation
// https://docs.fabricmc.net/develop/entities/attributes

public void applyDownedMovementSpeed(ServerPlayer player) {
    AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);

    if (movementSpeed != null) {
        // Remove old modifier if exists
        movementSpeed.removeModifier(DOWNED_SPEED_MODIFIER_ID);

        // Apply 10% of normal speed (desperately slow crawl)
        AttributeModifier downedModifier = new AttributeModifier(
            Identifier.fromNamespaceAndPath("dread", "downed_crawl"),
            -0.9, // -90% speed
            AttributeModifier.Operation.MULTIPLY_TOTAL // Applied after all other modifiers
        );

        movementSpeed.addPermanentModifier(downedModifier);
    }

    // Also affect sneaking_speed (controls crawl in vanilla)
    AttributeInstance sneakingSpeed = player.getAttribute(Attributes.SNEAKING_SPEED);
    if (sneakingSpeed != null) {
        sneakingSpeed.addPermanentModifier(new AttributeModifier(
            Identifier.fromNamespaceAndPath("dread", "downed_sneak"),
            -0.9,
            AttributeModifier.Operation.MULTIPLY_TOTAL
        ));
    }
}

public void removeDownedMovementSpeed(ServerPlayer player) {
    AttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
    if (movementSpeed != null) {
        movementSpeed.removeModifier(DOWNED_SPEED_MODIFIER_ID);
    }

    AttributeInstance sneakingSpeed = player.getAttribute(Attributes.SNEAKING_SPEED);
    if (sneakingSpeed != null) {
        sneakingSpeed.removeModifier(DOWNED_SNEAK_MODIFIER_ID);
    }
}
```

### Persistent World State (Downed Players Tracking)

```java
// Source: Existing project pattern from SpawnProbabilityState.java

public class DownedPlayersState extends SavedData {
    private static final String FILE_NAME = "dread_downed_players";

    private final Map<UUID, DownedPlayerData> downedPlayers = new HashMap<>();
    private final Map<UUID, RevivalProgress> activeRevivals = new HashMap<>();

    public static DownedPlayersState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                DownedPlayersState::new,
                DownedPlayersState::load,
                DataFixTypes.LEVEL
            ),
            FILE_NAME
        );
    }

    public void setDowned(ServerPlayer player, int durationSeconds) {
        UUID playerId = player.getUUID();
        downedPlayers.put(playerId, new DownedPlayerData(
            playerId,
            player.blockPosition(),
            durationSeconds * 20 // Convert to ticks
        ));
        setDirty();
    }

    public boolean isDowned(ServerPlayer player) {
        return downedPlayers.containsKey(player.getUUID());
    }

    public int decrementTimer(ServerPlayer player) {
        DownedPlayerData data = downedPlayers.get(player.getUUID());
        if (data != null) {
            data.remainingTicks--;
            setDirty();
            return data.remainingTicks / 20; // Return seconds
        }
        return 0;
    }

    @Override
    public NbtCompound save(NbtCompound nbt, HolderLookup.Provider provider) {
        NbtList downedList = new NbtList();
        for (DownedPlayerData data : downedPlayers.values()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUUID("UUID", data.playerId);
            playerNbt.putInt("RemainingTicks", data.remainingTicks);
            playerNbt.putLong("DownedPos", data.downedPos.asLong());
            downedList.add(playerNbt);
        }
        nbt.put("DownedPlayers", downedList);
        return nbt;
    }

    public static DownedPlayersState load(NbtCompound nbt, HolderLookup.Provider provider) {
        DownedPlayersState state = new DownedPlayersState();
        NbtList downedList = nbt.getList("DownedPlayers", 10); // 10 = Compound tag

        for (int i = 0; i < downedList.size(); i++) {
            NbtCompound playerNbt = downedList.getCompound(i);
            UUID playerId = playerNbt.getUUID("UUID");
            int remainingTicks = playerNbt.getInt("RemainingTicks");
            BlockPos downedPos = BlockPos.of(playerNbt.getLong("DownedPos"));

            state.downedPlayers.put(playerId,
                new DownedPlayerData(playerId, downedPos, remainingTicks));
        }

        return state;
    }

    private static class DownedPlayerData {
        UUID playerId;
        BlockPos downedPos;
        int remainingTicks;

        DownedPlayerData(UUID playerId, BlockPos downedPos, int remainingTicks) {
            this.playerId = playerId;
            this.downedPos = downedPos;
            this.remainingTicks = remainingTicks;
        }
    }
}
```

### Gamemode Transition to Spectator

```java
// Source: Minecraft ServerPlayer API + community mod examples
// https://github.com/aws404/Fabric-ObserverMode

public void transitionToSpectator(ServerPlayer player) {
    // Change game mode
    player.setGameMode(GameMode.SPECTATOR);

    // Clear downed state
    DownedPlayersState state = DownedPlayersState.get(player.serverLevel());
    state.removeDowned(player);

    // Remove visual effects on client
    ServerPlayNetworking.send(player, new RemoveDownedEffectsS2CPayload());

    // Broadcast death message (chat only, no death screen)
    player.server.getPlayerList().broadcastSystemMessage(
        Component.literal(player.getName().getString() +
            " has been claimed by the Dread")
            .withStyle(ChatFormatting.DARK_RED),
        false // Not system message (shows in chat)
    );

    // Note: Spectators can see Dread even when living players cannot
    // (handled in DreadEntity rendering logic, not here)
}
```

### World Render Progress Bar Above Player

```java
// Source: Fabric rendering patterns + community mod examples
// Requires mixin or Fabric rendering event for world rendering context

public class RevivalProgressRenderer {
    public static void renderProgressBar(
        GuiGraphics context,
        Camera camera,
        ServerPlayer downedPlayer,
        float progress // 0.0 to 1.0
    ) {
        Vec3 playerPos = downedPlayer.position().add(0, downedPlayer.getBbHeight() + 0.5, 0);
        Vec3 cameraPos = camera.getPosition();

        // Calculate screen position from world position
        PoseStack matrices = context.pose();
        matrices.pushPose();

        // Translate to player's world position
        matrices.translate(
            playerPos.x - cameraPos.x,
            playerPos.y - cameraPos.y,
            playerPos.z - cameraPos.z
        );

        // Billboard effect - always face camera
        matrices.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        matrices.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

        // Scale (progress bar is 32 pixels wide)
        float scale = 0.025f;
        matrices.scale(-scale, -scale, scale);

        // Render progress bar background
        int barWidth = 32;
        int barHeight = 4;
        context.fill(-barWidth/2, -barHeight/2, barWidth/2, barHeight/2, 0x80000000);

        // Render progress fill
        int fillWidth = (int)(barWidth * progress);
        int color = 0xFF00FF00; // Green
        context.fill(-barWidth/2, -barHeight/2, -barWidth/2 + fillWidth, barHeight/2, color);

        matrices.popPose();
    }
}

// Note: Actual implementation requires WorldRenderEvents.AFTER_ENTITIES or similar
// to hook into render pipeline at correct time
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| HudRenderCallback event | HudElementRegistry with DeltaTracker | Minecraft 1.20+ | Old callback deprecated, new API provides better timing control and layer management |
| Manual packet serialization with PacketByteBuf | CustomPayload with StreamCodec | Fabric API 0.90+ (2023) | Type-safe, cleaner API, automatic serialization |
| ServerPlayerEvents.ALLOW_DEATH | ServerLivingEntityEvents.ALLOW_DEATH | Fabric API 0.92+ (2023) | Player-specific event deprecated in favor of general living entity event |
| Direct OpenGL shader calls | Satin API ManagedShaderEffect | Established by 2021 | Eliminates boilerplate, handles resource management |
| Entity.setPose() reflection hacks | Official EntityPose API | Minecraft 1.14+ | Now public API with proper networking |

**Deprecated/outdated:**
- Using HudRenderCallback - replaced by HudElementRegistry
- ServerPlayerEvents.ALLOW_DEATH - use ServerLivingEntityEvents.ALLOW_DEATH with instanceof check
- Manual shader JSON creation without Satin - possible but significantly more complex

## Open Questions

Things that couldn't be fully resolved:

1. **Camera Lock Network Synchronization Reliability**
   - What we know: setCameraEntity() is client-side only, triggered by S2C packet
   - What's unclear: Best approach for handling packet loss during cinematic - should client retry, or server track cinematic state and re-send?
   - Recommendation: Implement server-side "cinematic state" flag with periodic S2C updates (every 10 ticks) to handle packet loss, client always defers to latest packet

2. **Spectator Seeing Dread Entity Rendering**
   - What we know: Requirement states spectators should see Dread when living players cannot
   - What's unclear: Exact mixin point in DreadEntity rendering - modify shouldRender() or renderNameTag() or use separate render layer?
   - Recommendation: Start with mixin in LivingEntityRenderer.shouldRender() checking if camera entity is in spectator mode, validate during implementation

3. **Crawl Movement in Complex Terrain**
   - What we know: EntityPose.SWIMMING can be forced, sneaking_speed attribute controls crawl speed
   - What's unclear: How vanilla movement modifiers interact - does soul sand stack with sneaking_speed penalty? What about swimming in water while "crawling"?
   - Recommendation: Apply MULTIPLY_TOTAL operation for attribute modifier (applies last), test extensively in water/lava/soul-sand during implementation phase

4. **Revival Progress Bar World Render Injection Point**
   - What we know: Need to render progress bar in world space above downed player
   - What's unclear: Fabric API doesn't expose direct WorldRenderEvents - may need mixin in WorldRenderer or alternative approach with entity renderer
   - Recommendation: Consider rendering progress bar as part of player entity renderer (mixin in PlayerRenderer) rather than separate world render event, or use invisible marker entity with custom renderer

5. **Uninterruptible Revival Edge Cases**
   - What we know: Revival should complete even if reviver takes damage or moves
   - What's unclear: What if reviver disconnects mid-revival? What if downed player disconnects? What if reviver dies?
   - Recommendation: Track revival server-side with both player UUIDs, cancel revival if either player disconnects or reviver dies, consider allowing revival to continue if reviver moves (within reason) but stays within range

## Sources

### Primary (HIGH confidence)

- [Fabric Events Documentation](https://docs.fabricmc.net/develop/events) - Event system overview, callback registration
- [Fabric Networking Documentation](https://docs.fabricmc.net/develop/networking) - CustomPayload, S2C/C2S packets, StreamCodec
- [Fabric HUD Rendering Documentation](https://docs.fabricmc.net/develop/rendering/hud) - HudElementRegistry, GuiGraphics, DeltaTracker
- [Fabric Entity Attributes Documentation](https://docs.fabricmc.net/develop/entities/attributes) - AttributeModifier, movement speed, custom attributes
- [Fabric API JavaDoc - ServerLivingEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.79.0+1.20/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html) - ALLOW_DEATH event timing
- [Satin API GitHub Repository](https://github.com/Ladysnake/Satin) - ManagedShaderEffect, post-processing shaders
- [Minecraft Wiki - Interaction Range](https://minecraft.wiki/w/Interaction_range) - Entity interaction distance mechanics
- [Minecraft Wiki - Shaders](https://minecraft.wiki/w/Shader) - Post-processing shader structure, JSON format

### Secondary (MEDIUM confidence)

- [Fabric Packet Migration Guide](https://gist.github.com/apple502j/9c6b9e5e8dec37cbf6f3916472a79d57) - Modern packet API patterns
- [Camera Utils Mod Source](https://github.com/henkelmax/camera-utils) - Camera manipulation examples
- [Locked On Mod Source](https://github.com/namecorp-llc/locked-on) - Camera entity locking implementation
- [Status Effect Timer Mod](https://github.com/magicus/statuseffecttimer) - HUD timer overlay example
- [Minecraft-Shaders-Wiki Core Shader List](https://github.com/McTsts/Minecraft-Shaders-Wiki/blob/main/Core%20Shader%20List.md) - Shader types and structure

### Tertiary (LOW confidence - requires validation)

- [Down But Not Out Mod](https://modrinth.com/mod/down-but-not-out) - Revival mechanic example (source not reviewed)
- [Incapacitated Mod](https://modrinth.com/mod/incapacitated) - Downed state implementation (source not reviewed)
- [Third Person Death Mod](https://modrinth.com/mod/thirdpersondeath) - Death camera transition (different approach)
- [Fabric-ObserverMode Source](https://github.com/aws404/Fabric-ObserverMode) - Spectator mode variants
- Community forum discussions on camera locking and entity poses - patterns mentioned but not officially documented

## Metadata

**Confidence breakdown:**
- Standard stack: MEDIUM - Fabric API and Satin are well-documented, but specific shader implementation details need validation during implementation
- Architecture: MEDIUM - Patterns verified against official docs, but world-space rendering and camera lock need testing for edge cases
- Pitfalls: HIGH - Event ordering and client-server sync pitfalls well-documented in Fabric community, totem interaction confirmed in API docs

**Research date:** 2026-01-24
**Valid until:** ~30 days (Fabric API is stable, next Minecraft version 1.21.5+ may introduce breaking changes)
