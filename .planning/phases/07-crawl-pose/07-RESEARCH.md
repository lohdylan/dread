# Phase 7: Crawl Pose - Research

**Researched:** 2026-01-26
**Domain:** Minecraft Fabric entity pose manipulation, player animation, multiplayer synchronization
**Confidence:** MEDIUM

## Summary

This phase implements visual crawling mechanics for downed players using Minecraft's built-in `EntityPose.SWIMMING` pose system. The research reveals that Minecraft provides native pose management through the Entity API, but poses auto-reset every tick based on player state (in water, sneaking, etc.), requiring mixins to intercept the `updatePose()` method and maintain custom poses. The key technical challenges are preventing pose resets, synchronizing pose changes in multiplayer via DataTracker, implementing movement constraints, and adding visual feedback through HUD overlays and particle effects.

The codebase already has strong foundations: movement speed penalties via EntityAttributes, network packet infrastructure for state synchronization, client-side effect handlers, and mixin examples for blocking player actions. The swimming pose provides the correct prone visual and hitbox dimensions, and Minecraft's camera pitch is naturally limited to -90/+90 degrees.

**Primary recommendation:** Use mixin to intercept PlayerEntity.updatePose() and force EntityPose.SWIMMING when downed, leverage existing network packets to sync pose state, implement movement constraints through existing attribute system and new mixins for jump/sprint blocking, and add blood vignette via HUD rendering + custom particle effects.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.116.8+ | Entity pose manipulation, events | Industry standard for Fabric 1.21.x, provides entity manipulation APIs |
| Mixin | Bundled with Fabric Loader 0.17.0+ | Intercept updatePose() method | Required for preventing pose auto-reset, bundled with MixinExtras 5.0.0 |
| EntityPose enum | Minecraft 1.21.x | Built-in SWIMMING pose | Native Minecraft pose system, no custom implementation needed |
| DataTracker | Minecraft 1.21.x | Multiplayer pose synchronization | Native Minecraft entity data sync, pose automatically tracked |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Satin API | 1.17.0 | Post-processing shaders (already in use) | Already integrated for downed blur effects |
| Fabric Rendering API | Part of Fabric API | HUD overlay rendering | Blood vignette overlay, particle rendering |
| Fabric Events | Part of Fabric API | Client tick events, player tick events | Camera bob, particle spawning |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| EntityPose.SWIMMING | Custom pose via armor stands | Swimming pose is built-in, syncs automatically, works with vanilla animations |
| Mixin on updatePose | Packet-based fake blocks | Mixin is cleaner, doesn't cause suffocation damage, avoids client-side glitches |
| Native particles | Custom particle textures | Use vanilla DRIPPING_LAVA (red) first for speed, custom particles for polish later |

**Installation:**
```bash
# Already installed in project
# Fabric API 0.116.8+1.21.1
# Satin API 1.17.0
```

## Architecture Patterns

### Recommended Project Structure
```
src/
├── main/java/com/dread/
│   └── death/
│       ├── CrawlPoseHandler.java          # Server-side pose management
│       └── CrawlMovementRestrictions.java # Jump/sprint/interaction blocking
├── client/java/com/dread/
│   ├── client/
│   │   ├── CrawlCameraHandler.java        # Camera pitch limiting + bob
│   │   ├── CrawlVignetteRenderer.java     # Blood vignette HUD overlay
│   │   └── CrawlParticleSpawner.java      # Blood drip particles
│   └── mixin/
│       ├── PlayerPoseMixin.java           # Intercept updatePose()
│       ├── PlayerJumpMixin.java           # Disable jumping when downed
│       └── PlayerInteractionMixin.java    # Disable interactions when downed
```

### Pattern 1: Pose Override with Mixin
**What:** Intercept PlayerEntity.updatePose() to force EntityPose.SWIMMING when player is downed
**When to use:** Custom pose needs to persist across ticks despite vanilla state checks
**Example:**
```java
// Pattern: Inject at HEAD with cancellable to prevent vanilla pose logic
@Mixin(PlayerEntity.class)
public class PlayerPoseMixin {

    @Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
    private void dread$forceSwimmingPoseWhenDowned(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity)(Object)this;

        // Check if player is downed (server-side check or client state)
        if (isDowned(player)) {
            player.setPose(EntityPose.SWIMMING);
            ci.cancel(); // Prevent vanilla pose logic from running
        }
    }

    private boolean isDowned(PlayerEntity player) {
        if (player.getWorld().isClient) {
            return DownedStateClientHandler.isDownedEffectActive();
        } else {
            // Server-side: Check DownedPlayersState
            return DownedPlayersState.getOrCreate((ServerWorld)player.getWorld())
                .isDowned((ServerPlayerEntity)player);
        }
    }
}
```
**Source:** [Fabric Mixin Examples](https://fabricmc.net/wiki/tutorial:mixin_examples), [Entity API](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/entity/Entity.html)

### Pattern 2: Movement Constraint Mixins
**What:** Block jump and sprint inputs at the earliest point to prevent movement
**When to use:** Need to disable player abilities while maintaining other functionality
**Example:**
```java
// Block jumping - inject at HEAD and cancel
@Mixin(LivingEntity.class)
public class PlayerJumpMixin {
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    private void dread$preventJumpWhenDowned(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayerEntity player) {
            if (DownedPlayersState.getOrCreate(player.getServerWorld()).isDowned(player)) {
                ci.cancel();
            }
        }
    }
}

// Block sprinting - similar pattern to existing ClientAttackMixin
@Mixin(PlayerEntity.class)
public class PlayerSprintMixin {
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void dread$preventSprintWhenDowned(boolean sprinting, CallbackInfo ci) {
        if (sprinting && isDowned((PlayerEntity)(Object)this)) {
            ci.cancel();
        }
    }
}
```
**Source:** [Fabric Events Documentation](https://docs.fabricmc.net/develop/events), existing ClientAttackMixin.java in codebase

### Pattern 3: HUD Overlay Rendering (Modern Fabric API)
**What:** Render blood vignette overlay using HudElementRegistry (replaces deprecated HudRenderCallback)
**When to use:** Adding custom screen overlays that need proper layering
**Example:**
```java
// Client initializer registration
@Override
public void onInitializeClient() {
    HudElementRegistry.attachElementBefore(
        VanillaHudElements.CHAT,
        Identifier.fromNamespaceAndPath("dread", "blood_vignette"),
        CrawlVignetteRenderer::render
    );
}

// Rendering method
public static void render(GuiGraphics context, DeltaTracker deltaTracker) {
    if (!DownedStateClientHandler.isDownedEffectActive()) return;

    MinecraftClient client = MinecraftClient.getInstance();
    int screenWidth = client.getWindow().getScaledWidth();
    int screenHeight = client.getWindow().getScaledHeight();

    // Render red vignette texture at screen edges
    // Use RenderSystem for blending, DrawContext for texture rendering
    RenderSystem.enableBlend();
    RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 0.5f); // Red tint
    context.drawTexture(
        VIGNETTE_TEXTURE,
        0, 0, 0, 0,
        screenWidth, screenHeight,
        screenWidth, screenHeight
    );
    RenderSystem.disableBlend();
}
```
**Source:** [Fabric HUD Rendering Documentation](https://docs.fabricmc.net/develop/rendering/hud)

### Pattern 4: Particle Spawning for Visual Effects
**What:** Spawn vanilla particles around downed player for blood drip effect
**When to use:** Adding atmospheric effects without custom particle registration
**Example:**
```java
// Server-side tick event in DreadDeathManager
private static void spawnBloodParticles(ServerWorld world, ServerPlayerEntity player) {
    if (world.getTime() % 10 == 0) { // Every 0.5 seconds
        // Spawn particles around player hitbox
        Vec3d pos = player.getPos().add(0, 0.5, 0);
        world.spawnParticles(
            ParticleTypes.DRIPPING_LAVA, // Red dripping particle
            pos.x + (world.random.nextDouble() - 0.5) * 0.5,
            pos.y,
            pos.z + (world.random.nextDouble() - 0.5) * 0.5,
            1, // count
            0, 0, 0, // velocity
            0.0 // speed
        );
    }
}
```
**Source:** Built-in ParticleTypes, [Blood Particle Mods](https://modrinth.com/mod/entity-blood-particles) for reference

### Pattern 5: Camera Pitch Limiting
**What:** Restrict camera rotation when crawling to reinforce ground-level feeling
**When to use:** Custom camera constraints for specific game states
**Example:**
```java
// Mixin on MouseInputHandler or Camera class
@Mixin(Camera.class)
public class CrawlCameraMixin {
    @Inject(method = "setRotation", at = @At("HEAD"))
    private void dread$limitPitchWhenCrawling(float yaw, float pitch, CallbackInfo ci) {
        if (DownedStateClientHandler.isDownedEffectActive()) {
            // Clamp pitch to -45 to +30 degrees (can't look straight up)
            pitch = MathHelper.clamp(pitch, -45.0f, 30.0f);
            // Note: Minecraft's normal pitch range is -90 to +90
        }
    }
}
```
**Source:** [Minecraft Camera Documentation](https://learn.microsoft.com/en-us/minecraft/creator/documents/camerasystem/camerapresetthirdperson?view=minecraft-bedrock-stable)

### Anti-Patterns to Avoid
- **Setting pose without canceling updatePose:** Pose will reset next tick, causing visual flickering
- **Client-only pose changes:** Will cause server-client desync, other players won't see crawling
- **Using @Overwrite for pose logic:** MixinExtras recommends @WrapOperation or @Inject with cancel
- **Spawning particles client-side only:** Won't be visible to other players in multiplayer
- **Blocking interactions in PlayerEntity constructor:** Too early, state not initialized

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Custom entity pose system | Armor stand manipulation, fake player entities | EntityPose.SWIMMING (built-in) | Vanilla pose syncs automatically via DataTracker, handles hitbox/eye height, no custom packets needed |
| Pose synchronization packets | Custom pose sync packet | DataTracker automatic sync | EntityPose is tracked entity data, syncs via EntityTrackerUpdateS2CPacket automatically |
| Blood vignette shader | Custom post-processing shader | HUD texture overlay with red tint | Simpler, no Satin dependency, works with shader mods disabled |
| Crawl camera bob | Custom camera transform system | Modify existing view bobbing intensity | Vanilla has view bobbing, just adjust intensity based on state |
| Custom particle types | JSON particle definition + textures | ParticleTypes.DRIPPING_LAVA (vanilla) | Red dripping particle exists, saves asset creation time |
| Movement speed reduction | Velocity manipulation every tick | EntityAttribute modifiers | Already in use (RevivalInteractionHandler), persistent, syncs automatically |
| First-person arm animation | Custom model rendering | Swimming pose arm animation (built-in) | Vanilla swimming already shows arms reaching forward |

**Key insight:** Minecraft's entity system already handles 90% of crawling mechanics through EntityPose.SWIMMING. The main work is preventing vanilla pose resets (mixin) and adding visual polish (particles, vignette, camera limits). Don't rebuild what's already there.

## Common Pitfalls

### Pitfall 1: Pose Resets Every Tick
**What goes wrong:** Setting pose with `setPose(EntityPose.SWIMMING)` works for one tick, then vanilla logic resets it based on player state (in water, sneaking, etc.)
**Why it happens:** PlayerEntity.updatePose() runs every tick and recalculates pose from scratch using vanilla conditions
**How to avoid:** Mixin into updatePose() at HEAD with cancellable=true, force pose, then cancel vanilla logic
**Warning signs:** Pose flickers between swimming and standing, player bounces between hitbox sizes

### Pitfall 2: Client-Server Pose Desync
**What goes wrong:** Setting pose only on client causes visual crawling locally but server thinks player is standing, causing collision issues
**Why it happens:** EntityPose is tracked via DataTracker and must be set on server to sync to all clients
**How to avoid:** Always set pose on server side, let DataTracker sync automatically. Client mixins should check state, not force pose.
**Warning signs:** Other players see standing player, block collisions behave wrong, "player moved wrongly" kick messages

### Pitfall 3: Camera Pitch Clamping Too Late
**What goes wrong:** Clamping pitch after camera update causes jittery camera movement
**Why it happens:** Camera rotation is applied, then clamped, causing visible correction
**How to avoid:** Clamp pitch in the rotation setter before it's applied to camera matrix
**Warning signs:** Camera stutters when trying to look up while crawling

### Pitfall 4: Particles Not Visible to Other Players
**What goes wrong:** Spawning particles client-side means only local player sees them
**Why it happens:** Particle spawning is client-local unless explicitly sent from server
**How to avoid:** Spawn particles server-side using world.spawnParticles(), which sends particle spawn packets to nearby clients
**Warning signs:** Blood drips only visible to downed player, not to rescuers

### Pitfall 5: Interaction Blocking Incomplete
**What goes wrong:** Blocking some interactions but missing edge cases (crafting table, lever, doors, etc.)
**Why it happens:** Minecraft has many interaction code paths (block interaction, entity interaction, item use)
**How to avoid:** Mixin into PlayerEntity.interact() and PlayerInteractionManager.interactBlock/interactItem at HEAD
**Warning signs:** Player can still open chests, use buttons, or activate levers while downed

### Pitfall 6: Sprint/Jump Input Ghosting
**What goes wrong:** Canceling sprint/jump at action point but input remains buffered, triggers when revived
**Why it happens:** Minecraft queues inputs for next tick processing
**How to avoid:** Cancel at input setter level (setSprinting, jump method HEAD) not at action execution
**Warning signs:** Player jumps immediately upon revival without pressing jump, sprint activates on revival

### Pitfall 7: Pose Transition Animation Jarring
**What goes wrong:** Instant snap from standing to swimming pose looks unnatural
**Why it happens:** No transition animation between poses in vanilla Minecraft
**How to avoid:** Accept limitation (context says "Claude's discretion on transitions"), or add camera fade effect during transition
**Warning signs:** Visual pop when entering/exiting downed state (acceptable per design decisions)

## Code Examples

Verified patterns from official sources and existing codebase:

### Setting Entity Pose
```java
// Server-side pose management in CrawlPoseHandler.java
public static void enterCrawlPose(ServerPlayerEntity player) {
    player.setPose(EntityPose.SWIMMING);
    // DataTracker automatically syncs to all clients
}

public static void exitCrawlPose(ServerPlayerEntity player) {
    player.setPose(EntityPose.STANDING);
}
```
**Source:** [Entity API](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/entity/Entity.html)

### Checking Pose State
```java
// Check if entity is in swimming pose
if (player.isInPose(EntityPose.SWIMMING)) {
    // Player is crawling
}

// Or use pose getter
if (player.getPose() == EntityPose.SWIMMING) {
    // Player is crawling
}
```
**Source:** [Entity API](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/entity/Entity.html)

### Blocking Player Interactions
```java
// Mixin pattern similar to existing ClientAttackMixin.java
@Mixin(PlayerInteractionManager.class)
public class PlayerInteractionMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void dread$blockInteractionsWhenDowned(
        ServerPlayerEntity player,
        ServerWorld world,
        ItemStack stack,
        Hand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<ActionResult> cir
    ) {
        if (DownedPlayersState.getOrCreate(world).isDowned(player)) {
            cir.setReturnValue(ActionResult.FAIL);
            cir.cancel();
        }
    }
}
```
**Source:** Existing ClientAttackMixin.java in codebase, [Mixin Examples](https://fabricmc.net/wiki/tutorial:mixin_examples)

### Integration with Existing Downed State
```java
// In DreadDeathHandler.java after setting downed state
state.setDowned(player);
RevivalInteractionHandler.applyMovementPenalty(player); // Already exists

// NEW: Add crawl pose
CrawlPoseHandler.enterCrawlPose(player);

// In RevivalInteractionHandler.completeRevival()
RevivalInteractionHandler.removeMovementPenalty(downedPlayer); // Already exists

// NEW: Remove crawl pose
CrawlPoseHandler.exitCrawlPose(downedPlayer);
```
**Source:** Existing codebase structure in DreadDeathHandler.java and RevivalInteractionHandler.java

### Spawning Vanilla Particles (Server-Side)
```java
// In DreadDeathManager.tick() or CrawlParticleSpawner
private static void tickBloodParticles(ServerWorld world, ServerPlayerEntity player) {
    // Only spawn particles every 10 ticks (0.5 seconds)
    if (world.getTime() % 10 != 0) return;

    Vec3d pos = player.getPos();

    // Spawn 1-2 particles around player
    for (int i = 0; i < world.random.nextInt(2) + 1; i++) {
        double offsetX = (world.random.nextDouble() - 0.5) * 0.6;
        double offsetZ = (world.random.nextDouble() - 0.5) * 0.6;

        world.spawnParticles(
            ParticleTypes.DRIPPING_LAVA, // Red dripping effect
            pos.x + offsetX,
            pos.y + 0.3, // Slightly above ground
            pos.z + offsetZ,
            1, // particle count
            0, 0, 0, // velocity spread
            0.0 // speed
        );
    }
}
```
**Source:** [ParticleTypes API](https://nekoyue.github.io/ForgeJavaDocs-NG/javadoc/1.16.5/net/minecraft/particles/ParticleTypes.html), [Entity Blood Particles mod](https://modrinth.com/mod/entity-blood-particles)

### HUD Blood Vignette Overlay
```java
// CrawlVignetteRenderer.java (client-side)
public class CrawlVignetteRenderer {
    private static final Identifier VIGNETTE_TEXTURE =
        Identifier.fromNamespaceAndPath("minecraft", "textures/misc/vignette.png");

    public static void render(GuiGraphics context, DeltaTracker deltaTracker) {
        if (!DownedStateClientHandler.isDownedEffectActive()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Red vignette overlay
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 0.2f, 0.2f, 0.7f); // Red tint, strong

        context.drawTexture(
            VIGNETTE_TEXTURE,
            0, 0, // x, y position
            0, 0, // u, v texture coords
            width, height, // render width/height
            width, height  // texture width/height
        );

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset
        RenderSystem.disableBlend();
    }
}
```
**Source:** [Fabric HUD Rendering](https://docs.fabricmc.net/develop/rendering/hud), vanilla Minecraft vignette rendering

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| HudRenderCallback event | HudElementRegistry.attachElement | Fabric API update (2024-2025) | Better layering control, deprecated callback "extremely limited" |
| @Redirect and @ModifyConstant mixins | @WrapOperation and @ModifyExpressionValue | MixinExtras 5.0.0 (bundled in Fabric Loader 0.17.0) | More maintainable, expression-based syntax |
| Custom particle JSON + textures | Use vanilla particles first | Best practice evolution | Faster iteration, fewer assets, custom particles for polish later |
| Armor stand pose manipulation | EntityPose enum (since 1.14) | Minecraft 1.14 (2019) | Native crawling support, no fake entities needed |
| Manual entity sync packets | DataTracker automatic sync | Minecraft entity system (long-standing) | Less code, automatic sync, no custom packets |

**Deprecated/outdated:**
- **HudRenderCallback:** Replaced by HudElementRegistry for better control and layering
- **@Overwrite mixins:** Avoid in favor of @WrapMethod or @Inject with cancel (better mod compatibility)
- **setSwimming() method:** Deprecated and non-functional, immediately reverted by server next tick
- **Client-side pose forcing:** Causes server-client desync, use server-side pose + DataTracker sync

## Open Questions

Things that couldn't be fully resolved:

1. **Does EntityPose.SWIMMING trigger drowning damage on land?**
   - What we know: Swimming pose is used by vanilla for crawling through 1-block gaps, likely safe
   - What's unclear: Whether any edge cases cause water-related effects when not in water
   - Recommendation: Test thoroughly, add mixin to prevent drowning damage if needed (inject into canBreatheInWater or damage calculation)

2. **How does swimming pose interact with elytra/riptide?**
   - What we know: Attack prevention already blocks projectiles (Phase 6), likely prevents riptide
   - What's unclear: Whether elytra can be activated while in forced swimming pose
   - Recommendation: Block item use interactions completely (doors, buttons, items clause in decisions), which covers elytra

3. **Camera bob intensity for crawling vs walking**
   - What we know: Vanilla has view bobbing setting, can detect movement speed
   - What's unclear: Optimal intensity that feels "immersive but not disorienting" (context says Claude's discretion)
   - Recommendation: Start with 50% of normal bobbing intensity, make configurable for player comfort

4. **Third-person crawling animation smoothness**
   - What we know: EntityPose.SWIMMING has built-in animation, shows active crawling when moving
   - What's unclear: Whether animation plays correctly when movement speed is reduced to 25%
   - Recommendation: Test in multiplayer, animation should scale with movement speed automatically

5. **Particle spawn rate for "reinforces injury" without spam**
   - What we know: Entity Blood Particles mod scales particles with damage, spawns around hitbox
   - What's unclear: Optimal balance between visible blood effect and particle performance
   - Recommendation: Start with 1-2 particles every 0.5 seconds, make configurable if players report lag

6. **Spectator transition timing with pose reset**
   - What we know: Pose should reset when "transitioning to spectator" (success criteria)
   - What's unclear: Whether to reset pose before or after spectator mode change, or if transition needs special handling
   - Recommendation: Reset pose in DreadDeathManager.transitionToSpectator() before changeGameMode(SPECTATOR), spectator mode likely handles pose automatically

## Sources

### Primary (HIGH confidence)
- [Entity API (Yarn 1.21)](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/entity/Entity.html) - EntityPose methods and usage
- [Fabric HUD Rendering Documentation](https://docs.fabricmc.net/develop/rendering/hud) - HudElementRegistry, modern overlay rendering
- [Fabric Events Documentation](https://docs.fabricmc.net/develop/events) - Event system for player tick, client tick
- [Fabric Mixin Examples](https://fabricmc.net/wiki/tutorial:mixin_examples) - Mixin patterns and best practices
- Existing codebase (DreadDeathHandler.java, RevivalInteractionHandler.java, ClientAttackMixin.java) - Integration points

### Secondary (MEDIUM confidence)
- [FabPose GitHub](https://github.com/YukkuriLaboratory/FabPose) - Reference implementation of pose commands (server-required, client-optional)
- [EntityPose enum documentation](https://maven.fabricmc.net/docs/yarn-21w05b+build.8/net/minecraft/entity/EntityPose.html) - Available pose values
- [DataTracker API](https://maven.fabricmc.net/docs/yarn-1.21.4+build.1/net/minecraft/entity/data/DataTracker.html) - Entity data synchronization
- [Minecraft Camera System Documentation](https://learn.microsoft.com/en-us/minecraft/creator/documents/camerasystem/camerapresetthirdperson?view=minecraft-bedrock-stable) - Pitch limits (-90 to +90)
- [Fabric Creating Particles](https://docs.fabricmc.net/develop/rendering/particles/creating-particles) - Particle system basics

### Tertiary (LOW confidence)
- [Crawl (Fabric/NeoForge) mod](https://www.curseforge.com/minecraft/mc-mods/crawl) - Existing crawl implementation (17.4M downloads, supports 1.21)
- [Entity Blood Particles mod](https://modrinth.com/mod/entity-blood-particles) - Reference for blood particle implementation
- [View Bobbing Options mod](https://modrinth.com/mod/viewboboptions/versions) - Camera bob customization reference
- [Spigot discussions on player pose](https://www.spigotmc.org/threads/put-player-in-swimming-pose-on-land-1-14-4.387934/) - Community knowledge on pose persistence challenges
- [Player Animation Library](https://modrinth.com/mod/player-animation-library) - Alternative to GeckoLib for player animations (noted for reference, likely not needed)

## Metadata

**Confidence breakdown:**
- Standard stack: MEDIUM - EntityPose.SWIMMING verified in official docs, mixin patterns confirmed, but specific updatePose() implementation not directly verified
- Architecture: MEDIUM - Patterns based on verified Fabric docs + existing codebase structure, but pose mixin pattern inferred from community knowledge
- Pitfalls: MEDIUM - Pose reset issue confirmed by multiple sources, other pitfalls inferred from general entity sync knowledge and mod examples

**Research date:** 2026-01-26
**Valid until:** 60 days (stable APIs - EntityPose, DataTracker, Fabric Rendering unchanged since 1.19)

**Key gaps requiring validation during implementation:**
1. Exact signature of PlayerEntity.updatePose() method in Fabric 1.21.1
2. Whether swimming pose triggers any water-related effects on land
3. Optimal particle spawn rate and camera bob intensity (requires playtesting)
4. Third-person animation smoothness with reduced movement speed
