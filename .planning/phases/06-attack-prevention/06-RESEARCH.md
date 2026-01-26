# Phase 6: Attack Prevention - Research

**Researched:** 2026-01-25
**Domain:** Minecraft Fabric mod player action prevention (melee + projectile attacks)
**Confidence:** HIGH

## Summary

Attack prevention in Minecraft Fabric requires blocking player actions on both client and server sides. The standard approach combines Fabric API callbacks for melee attacks with mixins for projectile prevention. Fabric provides `AttackEntityCallback` for intercepting entity attacks (melee), while projectile attacks (bow, crossbow, trident) require mixins into `UseItemCallback` or item-specific `onStoppedUsing` methods.

The key challenge is maintaining client-server synchronization: the server must authoritatively block attacks while the client prevents animations/sounds from playing. Client-side blocking alone is insufficient as players could bypass it with modified clients. Server-side blocking alone creates a poor UX where animations play but nothing happens.

**Primary recommendation:** Use server-side `AttackEntityCallback` for melee + client-side mixin to cancel attack animations, combined with server-side `UseItemCallback` for projectiles. Check downed state via existing `DownedPlayersState` infrastructure.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Fabric API | 0.116.8+ (for MC 1.21) | Event system for player interactions | Official Fabric event system, provides `AttackEntityCallback` and `UseItemCallback` |
| Fabric Events Interaction | v0 module | Player interaction callbacks | Part of Fabric API, standard for blocking attacks/item use |
| Mixin | 0.8+ (bundled with Fabric Loader) | Bytecode manipulation for animation cancellation | Standard Fabric modding tool for injecting into vanilla code |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| MixinExtras | 5.0.0+ (bundled since Fabric Loader 0.17.0) | Safer mixin alternatives (`@WrapOperation` vs `@Redirect`) | When targeting methods already modified by other mods |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Fabric callbacks | Pure mixins for everything | Callbacks are more compatible (multiple mods can register), mixins risk conflicts |
| Client + server blocking | Server-only blocking | Server-only creates poor UX (animations play but nothing happens) |
| Mixins into PlayerEntity.attack | AttackEntityCallback | Callback is cleaner and more compatible with other mods |

**Installation:**
```bash
# Already in build.gradle - no additional dependencies needed
modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
```

## Architecture Patterns

### Recommended Project Structure
```
src/
├── main/java/com/dread/death/
│   └── AttackPreventionHandler.java    # Server-side callback registration
└── client/java/com/dread/mixin/
    └── ClientAttackMixin.java           # Client-side animation cancellation
```

### Pattern 1: Server-Side Attack Blocking with Fabric Callbacks
**What:** Use `AttackEntityCallback.EVENT.register()` for melee attacks, `UseItemCallback.EVENT.register()` for projectiles
**When to use:** Authoritative blocking of attacks on logical server (works in singleplayer + multiplayer)
**Example:**
```java
// Source: https://fabricmc.docs.concern.i.ng/fabric-events-interaction-v0/
public class AttackPreventionHandler {
    public static void register() {
        // Block melee attacks
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Check if player is downed (server-side state)
            if (world instanceof ServerWorld serverWorld) {
                DownedPlayersState state = DownedPlayersState.getOrCreate(serverWorld);
                if (state.isDowned(player.getUuid())) {
                    return ActionResult.FAIL; // Cancel attack, no packet sent
                }
            }
            return ActionResult.PASS; // Allow normal processing
        });

        // Block projectile attacks (bow, crossbow, trident)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world instanceof ServerWorld serverWorld) {
                DownedPlayersState state = DownedPlayersState.getOrCreate(serverWorld);
                if (state.isDowned(player.getUuid())) {
                    ItemStack stack = player.getStackInHand(hand);
                    return TypedActionResult.fail(stack); // Cancel use, no packet
                }
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        });
    }
}
```

### Pattern 2: Client-Side Animation Cancellation with Mixins
**What:** Inject into client attack methods to prevent arm swing and sound effects when downed
**When to use:** Supplement server-side blocking for smooth UX (no visual glitches)
**Example:**
```java
// Source: https://www.tabnine.com/code/java/methods/org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable/cancel
@Mixin(MinecraftClient.class)
public class ClientAttackMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void dread$preventAttackWhenDowned(CallbackInfoReturnable<Boolean> cir) {
        // Check client-side downed flag (synced via DownedStateUpdateS2C packet)
        if (DownedStateClientHandler.isDownedEffectActive()) {
            cir.setReturnValue(false); // Cancel attack
            cir.cancel();
        }
    }
}
```

### Pattern 3: Client-Server State Synchronization
**What:** Server sends downed state updates via S2C packet, client stores local flag
**When to use:** Always - client needs to know downed state for animations, server for authority
**Example:**
```java
// Server: Already implemented in DreadDeathManager.syncDownedStates()
DownedStateUpdateS2C packet = new DownedStateUpdateS2C(true, remainingSeconds);
ServerPlayNetworking.send(player, packet);

// Client: Already implemented in DownedStateClientHandler
public static void applyDownedEffects(int remainingTime) {
    isDownedEffectActive = true; // Client-side flag used in mixins
}
```

### Anti-Patterns to Avoid
- **Client-only blocking:** Exploitable by modified clients, always validate server-side
- **Using @Overwrite mixins:** Breaks compatibility with other mods, use @Inject with cancellable=true
- **Forgetting spectator check:** Fabric callbacks fire before spectator check, manually validate game mode
- **Blocking attacks without canceling animations:** Creates confusing UX where player swings but nothing happens

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Attack event system | Custom attack tracking via entity tick hooks | `AttackEntityCallback.EVENT` | Fabric API provides proper event phases, compatibility with other mods |
| Item use prevention | Manual tracking of player input packets | `UseItemCallback.EVENT` | Handles all item types (bow, crossbow, trident, etc.) in one callback |
| Client-server sync | Custom packet system for state flags | Existing `DownedStateUpdateS2C` packet | Already implemented in v1.0, syncs every second |
| Animation cancellation | Complex entity renderer overrides | Simple mixin with `@Inject(cancellable=true)` | Cleaner, more compatible, easier to maintain |

**Key insight:** Fabric API's event system handles the complex cases (attack timing, projectile detection, spectator mode) that look simple but have edge cases. Always prefer Fabric callbacks over custom solutions.

## Common Pitfalls

### Pitfall 1: Server-Only Blocking Creates Visual Bugs
**What goes wrong:** Attack animations and sounds play on client, but server cancels the attack effect
**Why it happens:** Client predicts attack success before server response
**How to avoid:** Block on both client (animations) and server (authority)
**Warning signs:** Players report "attacks don't work" but see swing animations

### Pitfall 2: Forgetting Integrated Server in Singleplayer
**What goes wrong:** Code assumes physical server, breaks in singleplayer
**Why it happens:** Minecraft runs a logical server even in singleplayer
**How to avoid:** Check `world.isClient()` not server instance, use `ServerWorld` casting
**Warning signs:** Mod works on dedicated server, crashes in singleplayer

### Pitfall 3: UseItemCallback Doesn't Fire for All Projectiles
**What goes wrong:** Tridents/bows still fire despite UseItemCallback blocking
**Why it happens:** Some projectiles use `onStoppedUsing` not `use` method flow
**How to avoid:** UseItemCallback fires before item use starts - it should catch bows/crossbows/tridents
**Warning signs:** Melee attacks blocked, but projectiles still work

### Pitfall 4: Mixin Injection Point Timing
**What goes wrong:** Animation plays partially before mixin cancels
**Why it happens:** Injecting at wrong point in method (TAIL vs HEAD)
**How to avoid:** Use `@At("HEAD")` for early cancellation before any logic runs
**Warning signs:** Brief flicker of animation before block

### Pitfall 5: Not Checking Spectator Mode
**What goes wrong:** Code tries to block spectator attacks, causes crashes
**Why it happens:** AttackEntityCallback fires before spectator check
**How to avoid:** Check `player.isSpectator()` or game mode before blocking
**Warning signs:** Crashes when spectators click entities

## Code Examples

Verified patterns from official sources:

### Blocking Melee Attacks (Server-Side)
```java
// Source: https://fabricmc.docs.concern.i.ng/fabric-events-interaction-v0/
AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
    // Note: Fires before spectator check, validate game mode if needed
    if (world instanceof ServerWorld serverWorld) {
        DownedPlayersState state = DownedPlayersState.getOrCreate(serverWorld);
        if (state.isDowned(player.getUuid())) {
            // FAIL = cancel without sending packet to server
            return ActionResult.FAIL;
        }
    }
    return ActionResult.PASS; // Allow normal attack
});
```

### Blocking Projectile Use (Server-Side)
```java
// Source: https://fabricmc.docs.concern.i.ng/fabric-events-interaction-v0/
UseItemCallback.EVENT.register((player, world, hand) -> {
    if (world instanceof ServerWorld serverWorld) {
        DownedPlayersState state = DownedPlayersState.getOrCreate(serverWorld);
        if (state.isDowned(player.getUuid())) {
            ItemStack stack = player.getStackInHand(hand);
            // Check if holding projectile weapon (optional - block all items or just weapons)
            if (stack.getItem() instanceof RangedWeaponItem ||
                stack.getItem() instanceof TridentItem) {
                return TypedActionResult.fail(stack);
            }
        }
    }
    return TypedActionResult.pass(player.getStackInHand(hand));
});
```

### Canceling Attack Animation (Client-Side Mixin)
```java
// Source: https://www.tabnine.com/code/java/methods/org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable/cancel
@Mixin(MinecraftClient.class)
public class ClientAttackMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void dread$preventAttackAnimation(CallbackInfoReturnable<Boolean> cir) {
        if (DownedStateClientHandler.isDownedEffectActive()) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
```

### Registering Mixin in dread.mixins.json
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.dread.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": [
  ],
  "client": [
    "DeathScreenMixin",
    "ClientAttackMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| @Redirect mixins | @Inject with cancellable=true | Fabric API 0.14+ | Better mod compatibility, multiple mods can inject same method |
| @ModifyConstant | @ModifyExpressionValue (MixinExtras) | MixinExtras 5.0.0 (2025) | Allows chaining, no conflicts when multiple mods target same constant |
| Custom event systems | Fabric API callbacks | Fabric API inception | Standardized events, auto-compatibility between mods |
| Client-side prevention only | Client + server dual blocking | Community best practice (2023+) | Prevents exploits, improves UX |

**Deprecated/outdated:**
- **@Overwrite mixins:** Completely replaces method, breaks other mods. Use @Inject instead
- **@Redirect:** Can't chain with other mods. Use @WrapOperation (MixinExtras) instead
- **Spigot/Bukkit EntityShootBowEvent:** Not available in Fabric, use UseItemCallback

## Open Questions

1. **Do we need to block ALL item use or just weapons?**
   - What we know: UseItemCallback blocks all right-click item use (food, blocks, weapons)
   - What's unclear: Should downed players be able to eat food or place blocks?
   - Recommendation: Start with weapon-only blocking (RangedWeaponItem, TridentItem), expand if needed

2. **Should attack blocking show feedback message?**
   - What we know: Silent blocking could confuse players
   - What's unclear: Project requirements don't specify UI feedback
   - Recommendation: No message - downed state already has blur + timer, message would be redundant

3. **Does doAttack mixin catch all client attack animations?**
   - What we know: doAttack is the main client attack method in MinecraftClient
   - What's unclear: Could miss edge cases like spectator mode clicks
   - Recommendation: Test in-game, add additional mixin to ClientPlayerEntity.attack() if needed

## Sources

### Primary (HIGH confidence)
- [Fabric Events Documentation](https://docs.fabricmc.net/develop/events) - Official event system guide
- [AttackEntityCallback API](https://fabricmc.docs.concern.i.ng/fabric-events-interaction-v0/fabric-events-interaction-v0/net.fabricmc.fabric.api.event.player/-attack-entity-callback/index.html) - Official API docs
- [UseItemCallback API](https://fabricmc.docs.concern.i.ng/fabric-events-interaction-v0/fabric-events-interaction-v0/net.fabricmc.fabric.api.event.player/-use-item-callback/index.html) - Official API docs
- [Fabric Wiki: Side (Client-Server)](https://wiki.fabricmc.net/tutorial:side) - Official guide on logical sides
- [PlayerEntity API (Yarn 1.21)](https://maven.fabricmc.net/docs/yarn-1.21+build.1/net/minecraft/entity/player/PlayerEntity.html) - Official Yarn mappings

### Secondary (MEDIUM confidence)
- [Fabric Wiki: Mixin Tips](https://wiki.fabricmc.net/tutorial:mixin_tips) - Community best practices
- [Fabric Wiki: Event Index](https://fabricmc.net/wiki/tutorial:event_index) - Event catalog
- [Tabnine: CallbackInfoReturnable.cancel examples](https://www.tabnine.com/code/java/methods/org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable/cancel) - Code examples

### Tertiary (LOW confidence)
- Web search results for mixin examples - various forum posts and outdated tutorials
- GitHub search for AttackEntityCallback - limited code examples found

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Fabric API is authoritative, AttackEntityCallback is well-documented
- Architecture: HIGH - Patterns verified from official Fabric docs and existing codebase (DownedStateClientHandler)
- Pitfalls: MEDIUM - Based on Fabric Wiki and community experience, not project-specific testing

**Research date:** 2026-01-25
**Valid until:** ~30 days (Fabric API is stable, no breaking changes expected for 1.21.x)
