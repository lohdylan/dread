# Architecture Integration: v1.1 Enhancements

**Project:** Dread Entity Mod
**Milestone:** v1.1 Polish & Immersion Features
**Researched:** 2026-01-25
**Confidence:** HIGH

## Executive Summary

v1.1 enhancements integrate cleanly with existing Dread architecture through well-established extension points. All five features use standard Forge/GeckoLib patterns:

1. **Crawl pose** → Entity pose system + client-side rendering
2. **Attack prevention** → Event cancellation in existing handlers
3. **Dread texture** → GeckoLib texture override (file replacement)
4. **Cinematic enhancement** → Extend existing client timer system
5. **Audio replacement** → OGG file swap (no code changes)

**Key finding:** Most work is additive (new event handlers, extended timers) rather than modification of existing systems. Only one existing file needs substantial changes (DownedStateClientHandler for pose).

---

## Existing Architecture Overview

### Server-Side Components
```
DreadEntity.java
├─ GeckoLib animated entity
├─ AI goals for behavior
└─ Form variant system (already supports multiple forms)

DeathCinematicController.java
├─ Triggers on player death event
├─ Teleports Dread face-to-face
└─ Server-authoritative positioning

DownedPlayersState.java
├─ Persistent map of downed players
├─ World-saved state
└─ Tracks respawn eligibility

RevivalInteractionHandler.java
├─ Handles teammate revival
└─ Movement speed modifier during revival
```

### Client-Side Components
```
DreadEntityModel.java
├─ GeoModel implementation
├─ Returns geometry resource
└─ getTextureResource() → selects texture by form variant

DreadEntityRenderer.java
├─ GeoEntityRenderer
└─ AutoGlowingGeoLayer for glowing eyes

DeathCinematicClientHandler.java
├─ 90-tick timer (4.5 seconds)
├─ Camera switches to Dread entity
└─ Client-side rendering state

DownedStateClientHandler.java
├─ Blur shader application
├─ Vignette effects
└─ Client visual feedback
```

### Resources
```
assets/dread/
├─ models/entity/dread.geo.json (GeckoLib geometry)
├─ animations/entity/dread.animation.json (GeckoLib animations)
├─ textures/entity/
│   ├─ dread_base.png
│   ├─ dread_variant2.png
│   └─ dread_variant3.png
└─ sounds.json (sound event definitions)

assets/dread/sounds/
└─ [placeholder OGG files]
```

---

## v1.1 Feature Integration

### 1. Crawl Pose for Downed Players

**Implementation Location:** `DownedStateClientHandler.java` (modify) + new server-side handler

**Architecture Pattern:**
```
Server-side:
PlayerTickEvent handler
├─ Check if player is downed (query DownedPlayersState)
├─ Call player.setPose(Pose.SWIMMING)
└─ Auto-syncs to client via SynchedEntityData

Client-side (DownedStateClientHandler):
├─ RenderPlayerEvent.Pre handler (NEW)
├─ Modify player model rotation if downed
└─ Apply crawl-specific rendering adjustments
```

**Key Technical Details:**

- **Pose.SWIMMING** is Minecraft's built-in crawl pose (used for 1-block-high spaces)
- **`setPose()` synchronizes automatically** via Forge's `SynchedEntityData` system (no custom packets needed)
- **Server authoritative:** Server sets pose, clients render it
- Player hitbox automatically shrinks to 0.6 blocks high (vanilla behavior)

**Code Changes Required:**

1. **New file:** `DownedPoseHandler.java` (server-side)
   - Subscribe to `TickEvent.ServerTickEvent` or `LivingEvent.LivingTickEvent`
   - Check `DownedPlayersState` for downed players
   - Apply `player.setPose(Pose.SWIMMING)` while downed
   - Reset to `Pose.STANDING` on revival/respawn

2. **Modify:** `DownedStateClientHandler.java`
   - Add `@SubscribeEvent` for `RenderPlayerEvent.Pre`
   - Adjust camera angle/model rotation for crawl pose
   - Coordinate with existing blur/vignette effects

**Sources:**
- [SetPose implementation - Forge Forums](https://forums.minecraftforge.net/topic/83874-setpose-implementation/)
- [Forge entity synchronization docs](https://docs.minecraftforge.net/en/latest/networking/entities/)
- [Pose JavaDocs (1.18.2)](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/Pose.html)

---

### 2. Attack Prevention for Downed Players

**Implementation Location:** New event handler class `DownedPlayerProtectionHandler.java`

**Architecture Pattern:**
```
Event-driven protection:
LivingAttackEvent (Forge event bus)
├─ Check if victim is downed player
├─ event.setCanceled(true) if downed
└─ Optionally play feedback sound/particles
```

**Integration Points:**

- **Query existing state:** `DownedPlayersState.isPlayerDowned(player)`
- **Event precedence:** HIGHEST priority to override other mods
- **Server-side only:** Register handler without `Dist.CLIENT` restriction

**Code Changes Required:**

1. **New file:** `DownedPlayerProtectionHandler.java`
   ```java
   @Mod.EventBusSubscriber(modid = "dread", bus = Bus.FORGE)
   public class DownedPlayerProtectionHandler {
       @SubscribeEvent(priority = EventPriority.HIGHEST)
       public static void onLivingAttack(LivingAttackEvent event) {
           if (event.getEntity() instanceof Player player) {
               if (DownedPlayersState.isPlayerDowned(player)) {
                   event.setCanceled(true);
                   // Optional: Play "invulnerable" sound
               }
           }
       }
   }
   ```

2. **Alternative approach:** Use `LivingHurtEvent` instead
   - Fired later in damage pipeline
   - Allows damage calculation but prevents actual hurt
   - Choice depends on desired feedback (knockback vs no reaction)

**Edge Cases to Handle:**
- Void damage (allow fall death to prevent softlock)
- /kill command (bypass protection for admin intervention)
- Environmental damage during revival (fire, lava)

**Sources:**
- [LivingAttackEvent JavaDocs](https://skmedix.github.io/ForgeJavaDocs/javadoc/forge/1.9.4-12.17.0.2051/net/minecraftforge/event/entity/living/LivingAttackEvent.html)
- [LivingHurtEvent code examples](https://www.tabnine.com/code/java/classes/net.minecraftforge.event.entity.living.LivingHurtEvent)
- [Event cancellation patterns](https://gist.github.com/Bricktricker/9ecec23188a2fcd0e54e817be8ce8d8d)

---

### 3. Dread Texture Replacement

**Implementation Location:** `assets/dread/textures/entity/` (resource files only)

**Architecture Pattern:**
```
GeckoLib texture system:
DreadEntityModel.getTextureResource()
├─ Already returns ResourceLocation based on form variant
├─ Points to texture files in assets/
└─ NO CODE CHANGES NEEDED for file replacement

File replacement:
assets/dread/textures/entity/
├─ dread_base.png → Replace with new texture
├─ dread_variant2.png → Replace with new texture
└─ dread_variant3.png → Replace with new texture
```

**Integration Details:**

- **Existing system:** `DreadEntityModel.getTextureResource()` already selects textures by form
- **No code modifications:** Texture paths are hardcoded ResourceLocations
- **Drop-in replacement:** New PNG files with same names override placeholders
- **Variant support:** Multiple forms already supported in existing architecture

**Process:**

1. Export new textures from Blockbench/art tool
2. Ensure same dimensions as current textures (GeckoLib doesn't require power-of-2)
3. Replace files in `assets/dread/textures/entity/`
4. Test in-game to verify all form variants display correctly

**Advanced: Runtime Texture Swapping (Optional Enhancement):**

If future features need dynamic texture changes beyond form variants:

```java
// In DreadEntityModel.java
@Override
public ResourceLocation getTextureResource(DreadEntity entity) {
    // Now receives renderer parameter in GeckoLib 4.7.3+
    if (entity.isEnraged()) {
        return ENRAGED_TEXTURE;
    } else if (entity.getFormVariant() == 2) {
        return VARIANT2_TEXTURE;
    }
    return BASE_TEXTURE;
}
```

**Sources:**
- [GeckoLib texture variants discussion](https://mcreator.net/forum/95047/texture-variants-geckolib-entity)
- [GeckoLib 4.7.3 changelog - getTextureResource receives renderer](https://modrinth.com/mod/geckolib/version/4.7.3)
- [GeckoLib animated textures wiki](https://github.com/bernie-g/geckolib/wiki/Animated-Textures-(Geckolib4))

---

### 4. Intense Cinematic Enhancement

**Implementation Location:** `DeathCinematicClientHandler.java` (modify)

**Architecture Pattern:**
```
Existing system:
DeathCinematicClientHandler
├─ 90-tick timer (4.5 seconds)
├─ Minecraft.getInstance().setCameraEntity(dreadEntity)
└─ Resets camera after timer expires

Enhanced system:
DeathCinematicClientHandler (extended)
├─ Longer timer (120-180 ticks = 6-9 seconds)
├─ Camera effects (shake, lerp, zoom)
├─ Post-processing shader intensification
└─ Coordinate with Dread animation triggers
```

**Code Changes Required:**

1. **Modify:** `DeathCinematicClientHandler.java`
   - Increase `CINEMATIC_DURATION` from 90 to 120-180 ticks
   - Add camera manipulation during cinematic:
     ```java
     @SubscribeEvent
     public static void onClientTick(TickEvent.ClientTickEvent event) {
         if (cinematicTicks > 0) {
             cinematicTicks--;

             // NEW: Camera effects
             if (cinematicTicks > 100) {
                 // Zoom phase
                 applyFOVTransition(90f, 60f); // Zoom in
             } else if (cinematicTicks > 80) {
                 // Shake phase
                 applyCameraShake(0.05f);
             }

             // Existing: Camera entity positioning
             Minecraft.getInstance().setCameraEntity(dreadEntity);

             // Reset at end (existing logic)
             if (cinematicTicks == 0) {
                 Minecraft.getInstance().setCameraEntity(player);
             }
         }
     }
     ```

2. **New helper methods:**
   - `applyFOVTransition(float from, float to)` - Smooth FOV changes
   - `applyCameraShake(float intensity)` - Small random offsets
   - Consider using RenderTickEvent for smoother transitions (60+ FPS vs 20 TPS)

3. **Coordinate with DownedStateClientHandler:**
   - Intensify blur/vignette during cinematic
   - Gradual fade-in rather than instant application

**Camera Manipulation Techniques:**

- **FOV changes:** Modify `GameRenderer.fov` during RenderTickEvent
- **Position offsets:** Small `camera.setPos()` adjustments for shake
- **Third-person distance:** Modify camera distance attribute for pull-back effect

**Limitations:**
- Client-side only (cosmetic, no gameplay impact)
- Must handle edge cases (player quits during cinematic, server disconnect)
- Smooth camera mods may interfere (test compatibility)

**Sources:**
- [CMDCam smooth camera transitions](https://www.curseforge.com/minecraft/mc-mods/cmdcam)
- [CameraLerp mod - smooth FOV](https://www.9minecraft.net/cameralerp-mod/)
- [Bedrock Edition camera command (reference)](https://minecraft.wiki/w/Commands/camera)

---

### 5. Real Audio Replacement

**Implementation Location:** `assets/dread/sounds/` (resource files only)

**Architecture Pattern:**
```
Forge sound system:
ModSounds.java (existing)
├─ Registers SoundEvents
└─ References sounds.json entries

assets/dread/sounds.json
├─ Maps sound event IDs to file paths
└─ Points to OGG files in assets/dread/sounds/

File replacement:
assets/dread/sounds/
├─ Replace placeholder OGG files
└─ NO CODE CHANGES NEEDED
```

**Integration Details:**

- **Zero code changes required** - sound file paths are in sounds.json
- **Format requirement:** Ogg Vorbis (.ogg) only
- **Mono vs Stereo:**
  - **Mono** (1 channel): Required for positional audio with distance attenuation
  - **Stereo** (2 channels): Always plays at player's head (no distance falloff)
- **Streaming:** For long audio (music, ambient), set `"stream": true` in sounds.json

**Process:**

1. **Export audio as Ogg Vorbis:**
   - Use Audacity: File → Export → Export as OGG
   - Use ffmpeg: `ffmpeg -i input.mp3 -c:a libvorbis output.ogg`
   - Ensure mono channel for positional sounds (Dread growl, jumpscare)

2. **Replace files:**
   ```
   assets/dread/sounds/
   ├─ dread_ambient.ogg → Replace
   ├─ dread_jumpscare.ogg → Replace
   ├─ dread_growl.ogg → Replace
   └─ [any other registered sounds]
   ```

3. **Verify sounds.json references:**
   ```json
   {
     "dread_jumpscare": {
       "subtitle": "dread.subtitle.jumpscare",
       "sounds": [ "dread:dread_jumpscare" ]
     }
   }
   ```
   - `"dread:dread_jumpscare"` resolves to `assets/dread/sounds/dread_jumpscare.ogg`

4. **Test in-game:** Trigger sound events to verify playback

**Advanced Configuration (Optional):**

- **Volume/pitch variation:**
  ```json
  "sounds": [
    {
      "name": "dread:dread_jumpscare",
      "volume": 1.0,
      "pitch": 1.0
    }
  ]
  ```
- **Multiple variants:** Array of sound files for random selection
- **Attenuation distance:** Controlled by SoundEvent type (not in sounds.json)

**Sources:**
- [Forge sounds documentation](https://docs.minecraftforge.net/en/latest/gameeffects/sounds/)
- [Forge Community Wiki - Sounds](https://forge.gemwire.uk/wiki/Sounds)
- [Ogg Vorbis format requirement](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/minecraft-mods/modification-development/2735552-is-it-possible-to-use-sound-files-aside-from-ogg)

---

## Component Modification Summary

| Component | Modification Type | Changes Required |
|-----------|------------------|------------------|
| **DownedStateClientHandler.java** | MODIFY | Add RenderPlayerEvent handler for crawl pose rendering |
| **DeathCinematicClientHandler.java** | EXTEND | Add camera effects, increase timer duration |
| **DownedPoseHandler.java** | NEW | Server-side pose management for downed players |
| **DownedPlayerProtectionHandler.java** | NEW | Event handler to cancel attacks on downed players |
| **assets/dread/textures/entity/*.png** | REPLACE | Drop-in texture replacements (no code changes) |
| **assets/dread/sounds/*.ogg** | REPLACE | Drop-in audio replacements (no code changes) |

**Files NOT Modified:**
- `DreadEntity.java` - No changes (existing AI/behavior sufficient)
- `DreadEntityModel.java` - No changes (texture system already supports variants)
- `DreadEntityRenderer.java` - No changes (rendering layer already correct)
- `DeathCinematicController.java` - No changes (server logic unchanged)
- `DownedPlayersState.java` - No changes (state management sufficient)
- `RevivalInteractionHandler.java` - No changes (revival mechanics unchanged)
- `ModSounds.java` - No changes (sound events already registered)

---

## Data Flow Diagrams

### Crawl Pose Flow
```
Server Tick
    ↓
DownedPoseHandler checks DownedPlayersState
    ↓
player.setPose(Pose.SWIMMING)
    ↓
SynchedEntityData auto-syncs to client
    ↓
Client renders player in crawl pose
    ↓
DownedStateClientHandler.RenderPlayerEvent adjusts rotation
```

### Attack Prevention Flow
```
Entity takes damage
    ↓
LivingAttackEvent fired (Forge event bus)
    ↓
DownedPlayerProtectionHandler checks victim
    ↓
Is victim downed? → Query DownedPlayersState
    ↓
YES: event.setCanceled(true) → No damage
NO: Event proceeds → Normal damage
```

### Cinematic Enhancement Flow
```
Player dies to Dread
    ↓
DeathCinematicController (server) teleports Dread
    ↓
Network packet → Client
    ↓
DeathCinematicClientHandler starts timer (120 ticks)
    ↓
Every tick:
    - Set camera to Dread entity
    - Apply FOV/shake effects
    - Intensify blur/vignette
    ↓
Timer expires → Reset camera to player
```

### Texture/Audio Flow
```
Resource pack load
    ↓
GeckoLib reads assets/dread/textures/entity/*.png
    ↓
DreadEntityModel.getTextureResource() returns ResourceLocation
    ↓
Renderer applies texture to model

Sound event triggered
    ↓
Forge reads assets/dread/sounds.json
    ↓
Resolves to assets/dread/sounds/*.ogg
    ↓
OpenAL plays audio (mono = positional, stereo = centered)
```

---

## Event Registration Architecture

### Existing Event Registration (Assume Already Present)
```java
// Likely in mod main class or dedicated event registrar
MinecraftForge.EVENT_BUS.register(DeathCinematicController.class);
MinecraftForge.EVENT_BUS.register(RevivalInteractionHandler.class);
// etc.
```

### New Event Registrations Required

**Client-Side Only:**
```java
@Mod.EventBusSubscriber(modid = "dread", bus = Bus.FORGE, value = Dist.CLIENT)
public class DownedStateClientHandler {
    // Existing blur/vignette handling

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // NEW: Crawl pose rendering adjustments
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        // NEW: Cinematic camera effects
    }
}
```

**Server-Side Only:**
```java
@Mod.EventBusSubscriber(modid = "dread", bus = Bus.FORGE)
public class DownedPoseHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Manage downed player poses
    }
}

@Mod.EventBusSubscriber(modid = "dread", bus = Bus.FORGE)
public class DownedPlayerProtectionHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        // Cancel attacks on downed players
    }
}
```

**Why Dist.CLIENT Matters:**
- Client-only events (RenderPlayerEvent, RenderTickEvent) crash dedicated servers if registered
- Server-only logic (pose management, attack prevention) wastes resources on client
- `@Mod.EventBusSubscriber(value = Dist.CLIENT)` ensures class only loads on physical client

**Sources:**
- [Forge events documentation](https://docs.minecraftforge.net/en/latest/concepts/events/)
- [Client-side event registration](https://forums.minecraftforge.net/topic/104094-1171-registering-client-sided-event-handler/)
- [EventBusSubscriber with Dist](https://forge.gemwire.uk/wiki/Events)

---

## Build Order & Dependencies

### Phase 1: Audio/Texture Replacement (No Code)
**Why first:** Zero code changes, immediate visual/audio upgrade

1. Replace texture PNGs in `assets/dread/textures/entity/`
2. Replace OGG files in `assets/dread/sounds/`
3. Test in-game to verify resources load correctly
4. **Deliverable:** Dread looks and sounds final

**Dependencies:** None (resource-only)

---

### Phase 2: Attack Prevention (Simple Event Handler)
**Why second:** Simplest code change, critical gameplay protection

1. Create `DownedPlayerProtectionHandler.java`
2. Implement `LivingAttackEvent` handler with cancellation logic
3. Register event subscriber (add `@Mod.EventBusSubscriber`)
4. Test: Verify downed players cannot be damaged
5. **Deliverable:** Downed players are invulnerable

**Dependencies:**
- Existing `DownedPlayersState` (already present)
- Forge event bus (core Forge)

---

### Phase 3: Crawl Pose (Moderate Complexity)
**Why third:** Requires both server and client changes

1. Create `DownedPoseHandler.java` (server-side)
   - Implement `TickEvent.ServerTickEvent` handler
   - Query `DownedPlayersState` and apply `setPose(Pose.SWIMMING)`

2. Modify `DownedStateClientHandler.java` (client-side)
   - Add `RenderPlayerEvent.Pre` handler
   - Adjust camera/model rotation for crawl pose

3. Test: Verify downed players render in crawl pose
4. **Deliverable:** Downed players visually crawl on ground

**Dependencies:**
- Forge entity pose system (vanilla Minecraft)
- `SynchedEntityData` auto-sync (Forge)
- Existing `DownedStateClientHandler` visual effects

---

### Phase 4: Cinematic Enhancement (Complex, Camera Manipulation)
**Why fourth:** Most complex, requires careful testing

1. Modify `DeathCinematicClientHandler.java`
   - Increase `CINEMATIC_DURATION` constant
   - Add camera shake logic in tick handler
   - Add FOV transition logic
   - Intensify blur/vignette during cinematic

2. Add helper methods:
   - `applyFOVTransition(float from, float to)`
   - `applyCameraShake(float intensity)`

3. Test: Verify smooth camera effects without motion sickness
4. Iterate on timing/intensity based on playtesting
5. **Deliverable:** Cinematic death sequence is intense and polished

**Dependencies:**
- Existing `DeathCinematicClientHandler` timer system
- `DownedStateClientHandler` blur/vignette effects
- Client-side camera manipulation APIs

---

### Dependency Graph
```
Audio/Texture Replacement (Phase 1)
    ↓
    [Independent, no code dependencies]

Attack Prevention (Phase 2)
    ↓
    Depends on: DownedPlayersState (existing)

Crawl Pose (Phase 3)
    ↓
    Depends on: DownedPlayersState (existing)
    Builds on: DownedStateClientHandler (existing)

Cinematic Enhancement (Phase 4)
    ↓
    Depends on: DeathCinematicClientHandler (existing)
    Enhances: DownedStateClientHandler effects (existing)
    Can reference: Crawl pose rendering (Phase 3)
```

**No circular dependencies.** Each phase can be tested independently before proceeding to the next.

---

## Architecture Patterns & Anti-Patterns

### Pattern 1: Event-Driven State Changes
**What:** Use Forge event bus for all state changes (pose, protection, cinematic)
**Why:** Decouples features from core entity logic, allows easy enable/disable via config
**Example:** `DownedPlayerProtectionHandler` cancels damage via `LivingAttackEvent` rather than modifying `DreadEntity` attack code

### Pattern 2: Client-Server Separation
**What:** Server manages authoritative state, client handles rendering/effects
**Why:** Prevents cheating, reduces network traffic, follows Minecraft's architecture
**Example:** Server sets `Pose.SWIMMING`, `SynchedEntityData` syncs automatically, client renders

### Pattern 3: Resource-Driven Content
**What:** Textures/audio as drop-in replacements, not hardcoded in Java
**Why:** Enables resource packs, easier iteration, no recompilation for art changes
**Example:** Replace `dread_base.png` file, no code changes needed

### Anti-Pattern 1: Mixing Rendering and Logic
**What:** DON'T put game logic in client-side event handlers
**Why:** Client can be modified (cheating), server won't see changes, desync issues
**Example:** ❌ Don't check if player is downed in `RenderPlayerEvent` and apply effects from there
**Instead:** ✅ Server sets pose via `DownedPoseHandler`, client renders existing pose

### Anti-Pattern 2: Hardcoded Resource Paths
**What:** DON'T build file paths with string concatenation in code
**Why:** Breaks resource pack overrides, difficult to maintain
**Example:** ❌ `new ResourceLocation("dread", "textures/entity/dread_" + variant + ".png")`
**Instead:** ✅ Use constants: `private static final ResourceLocation VARIANT2_TEXTURE = new ResourceLocation("dread", "textures/entity/dread_variant2.png");`

### Anti-Pattern 3: Tick-Heavy Operations
**What:** DON'T perform expensive calculations every tick (20 times/second)
**Why:** Performance impact, especially on servers with many players
**Example:** ❌ Iterating all players every tick to check downed state
**Instead:** ✅ Event-driven: Only check player when `LivingAttackEvent` fires

---

## Scalability Considerations

### At 10 Players (Typical Server)
- **Pose management:** Negligible overhead (only downed players, typically 0-1)
- **Attack prevention:** Event-based, only fires on actual attacks
- **Cinematic:** Client-side only, no server impact
- **Textures/audio:** Loaded once at startup, no runtime cost

**Performance:** Excellent

### At 100 Players (Large Server)
- **Pose management:** Still negligible (max 100 checks per tick, only if all downed)
- **Attack prevention:** Scales linearly with PvP frequency, typically low
- **Cinematic:** Client-side, no server overhead
- **Network traffic:** `setPose()` sync is minimal (1 byte per pose change)

**Performance:** Good
**Optimization:** Consider caching downed player list rather than querying map every tick

### At 1000 Players (Massive Server)
- **Pose management:** Potential bottleneck if many players downed simultaneously
- **Mitigation:** Batch pose updates, use `WorldTickEvent` instead of `ServerTickEvent`
- **Attack prevention:** Still event-driven, scales with actual attacks not player count
- **Cinematic:** Client-side, unaffected by server population

**Performance:** Acceptable with optimizations
**Critical optimization:** Only iterate downed players (maintained list) rather than all players

---

## Testing Checkpoints

### Crawl Pose Testing
- [ ] Downed player renders in crawl pose (0.6 blocks tall)
- [ ] Pose persists while downed, even after re-logging
- [ ] Pose resets to standing on revival
- [ ] Pose resets on respawn after death
- [ ] Multiplayer: Other players see downed player crawling
- [ ] Camera angle is appropriate (not clipping through ground)

### Attack Prevention Testing
- [ ] Downed player takes no damage from mobs
- [ ] Downed player takes no damage from other players (PvP)
- [ ] Downed player can still die to void damage (anti-softlock)
- [ ] Admin /kill command bypasses protection (server management)
- [ ] Damage sounds/particles don't play (clean feedback)
- [ ] Protection ends immediately on revival

### Cinematic Testing
- [ ] Camera switches to Dread entity on death
- [ ] Camera effects are smooth (no jarring jumps)
- [ ] FOV transition feels cinematic (not nauseating)
- [ ] Camera shake is subtle (not disorienting)
- [ ] Timer duration feels appropriate (not too long/short)
- [ ] Camera resets to player correctly after cinematic
- [ ] Works in multiplayer (client-side only, no interference)
- [ ] Handles edge case: Player quits during cinematic (no crash)

### Texture/Audio Testing
- [ ] All Dread texture variants display correctly
- [ ] Textures have correct resolution (no stretching/blurring)
- [ ] Animated texture frames play at correct speed (if applicable)
- [ ] Sounds play at correct volume
- [ ] Positional audio has correct attenuation (distance falloff)
- [ ] Sounds don't overlap/cut off incorrectly
- [ ] Subtitles display correctly (if configured)

---

## Confidence Assessment

| Feature | Confidence | Reasoning |
|---------|-----------|-----------|
| **Crawl Pose** | HIGH | Standard Forge pose system, well-documented, many mods use SWIMMING pose for crawling |
| **Attack Prevention** | HIGH | Event cancellation is core Forge pattern, extensively documented |
| **Texture Replacement** | HIGH | GeckoLib texture system already supports variants, drop-in replacement confirmed |
| **Audio Replacement** | HIGH | Forge sound system explicitly designed for OGG file replacement |
| **Cinematic Enhancement** | MEDIUM | Camera manipulation is well-known, but smooth effects require iteration/playtesting |

**Overall Project Confidence:** HIGH

**Risks:**
- **Cinematic camera effects:** May require tuning based on playtesting feedback (motion sickness concerns)
- **Multiplayer edge cases:** Pose/cinematic behavior when players join mid-death sequence (low probability, mitigable)
- **Mod compatibility:** Other mods that modify player rendering/pose may conflict (consider compatibility testing)

---

## Open Questions & Future Considerations

### Optional Enhancements (Post-v1.1)
1. **Configurable cinematic duration:** Allow players to adjust timer length via config file
2. **Sound variation:** Multiple OGG files for same event (random selection per trigger)
3. **Texture animation:** GeckoLib supports animated textures (UV scrolling, frame-based)
4. **Camera path system:** Bezier curve camera movement instead of static position
5. **Revival UI improvement:** Integrate crawl pose with HUD display (show nearby teammates)

### Compatibility Considerations
- **PlayerAnimator mod:** May override pose system, test compatibility
- **Smooth camera mods:** May interfere with cinematic effects, document known issues
- **Resource packs:** Ensure texture/audio overrides work correctly with modpack distributions

### Performance Profiling Targets
- Tick time impact of `DownedPoseHandler` (target: <0.01ms per tick)
- Memory footprint of cinematic timer (target: <1KB per active cinematic)
- Network bandwidth for pose synchronization (target: <10 bytes per pose change)

---

## Sources

**Minecraft Forge Documentation:**
- [Events - Forge Documentation](https://docs.minecraftforge.net/en/latest/concepts/events/)
- [Sounds - Forge Documentation](https://docs.minecraftforge.net/en/latest/gameeffects/sounds/)
- [Synchronizing Entities - Forge Documentation](https://docs.minecraftforge.net/en/latest/networking/entities/)
- [Sounds - Forge Community Wiki](https://forge.gemwire.uk/wiki/Sounds)
- [Events - Forge Community Wiki](https://forge.gemwire.uk/wiki/Events)

**GeckoLib Documentation:**
- [GeckoLib Wiki - Custom Entity](https://github.com/bernie-g/geckolib/wiki/Custom-GeckoLib-Entity)
- [GeckoLib Wiki - Animated Textures (Geckolib4)](https://github.com/bernie-g/geckolib/wiki/Animated-Textures-(Geckolib4))
- [GeckoLib Wiki - Render Layers (Geckolib5)](https://github.com/bernie-g/geckolib/wiki/Render-Layers-(Geckolib5))
- [GeckoLib Changelog](https://modrinth.com/mod/geckolib/changelog)

**Minecraft Official:**
- [Shader - Minecraft Wiki](https://minecraft.wiki/w/Shader)
- [Attribute - Minecraft Wiki](https://minecraft.wiki/w/Attribute)
- [Third Person View - Minecraft Wiki](https://minecraft.wiki/w/Third-person_view)

**Community Resources:**
- [Not Enough Animations - CurseForge](https://www.curseforge.com/minecraft/mc-mods/not-enough-animations) (crawl animation reference)
- [playerAnimator - CurseForge](https://www.curseforge.com/minecraft/mc-mods/playeranimator) (player animation library)
- [LivingAttackEvent JavaDocs](https://skmedix.github.io/ForgeJavaDocs/javadoc/forge/1.9.4-12.17.0.2051/net/minecraftforge/event/entity/living/LivingAttackEvent.html)
- [Pose JavaDocs (1.18.2)](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.18.2/net/minecraft/world/entity/Pose.html)
- [SetPose implementation - Forge Forums](https://forums.minecraftforge.net/topic/83874-setpose-implementation/)

**LOW Confidence (WebSearch-only):**
- Camera manipulation specifics (requires direct testing)
- GeckoLib 5 API changes (recent version, may have undocumented features)
