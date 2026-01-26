---
phase: 06
plan: 01
subsystem: combat-prevention
tags: [fabric-api, mixin, attack-blocking, downed-state]

dependency-graph:
  requires:
    - phase-03 (downed state infrastructure, DownedPlayersState, DownedStateClientHandler)
  provides:
    - server-side attack prevention via Fabric callbacks
    - client-side attack animation cancellation
  affects:
    - phase-07 (crawl pose - may need similar client-side mixins)
    - phase-08 (cinematic camera - may reference attack blocking pattern)

tech-stack:
  added: []
  patterns:
    - Fabric AttackEntityCallback for melee attack interception
    - Fabric UseItemCallback for projectile weapon blocking
    - Client-side mixin with @Inject(cancellable=true) at HEAD

key-files:
  created:
    - src/main/java/com/dread/death/AttackPreventionHandler.java
    - src/client/java/com/dread/mixin/ClientAttackMixin.java
  modified:
    - src/main/java/com/dread/DreadMod.java
    - src/main/resources/dread.mixins.json

decisions: []

metrics:
  duration: 3 min
  completed: 2026-01-26
---

# Phase 6 Plan 1: Attack Prevention Summary

Server and client attack blocking for downed players using Fabric API callbacks and Mixin injection.

## What Was Built

### AttackPreventionHandler (Server-Side)

Created `src/main/java/com/dread/death/AttackPreventionHandler.java` with:

- **AttackEntityCallback** - Blocks melee attacks by returning `ActionResult.FAIL` when player is downed
- **UseItemCallback** - Blocks projectile weapon use (RangedWeaponItem, TridentItem) by returning `TypedActionResult.fail(stack)`
- Both callbacks check `DownedPlayersState.isDowned(player.getUuid())` server-side
- Debug logging for blocked attacks

### ClientAttackMixin (Client-Side)

Created `src/client/java/com/dread/mixin/ClientAttackMixin.java` with:

- `@Mixin(MinecraftClient.class)` targeting the main client class
- `@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)` for early cancellation
- Checks `DownedStateClientHandler.isDownedEffectActive()` client-side flag
- Returns false and cancels to prevent arm swing animation and attack sounds

### Integration

- Updated `DreadMod.onInitialize()` to call `AttackPreventionHandler.register()` after DreadDeathManager
- Added `ClientAttackMixin` to `dread.mixins.json` client array

## Key Implementation Details

**Server-side blocking (authoritative):**
```java
AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
    if (world instanceof ServerWorld serverWorld) {
        DownedPlayersState state = DownedPlayersState.getOrCreate(serverWorld);
        if (state.isDowned(player.getUuid())) {
            return ActionResult.FAIL;
        }
    }
    return ActionResult.PASS;
});
```

**Client-side animation blocking (UX):**
```java
@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
private void dread$preventAttackWhenDowned(CallbackInfoReturnable<Boolean> cir) {
    if (DownedStateClientHandler.isDownedEffectActive()) {
        cir.setReturnValue(false);
        cir.cancel();
    }
}
```

## Deviations from Plan

None - plan executed exactly as written.

## Verification Results

- Build compiles without errors
- All 3 success criteria from phase met:
  1. Melee attacks blocked server-side (AttackEntityCallback returns FAIL)
  2. Projectile attacks blocked server-side (UseItemCallback returns fail for weapons)
  3. Attack animations blocked client-side (doAttack mixin cancels)

## Commits

| Hash | Message |
|------|---------|
| 095f8ec | feat(06-01): add server-side attack prevention handler |
| e233fe5 | feat(06-01): add client-side attack animation mixin |

## Next Phase Readiness

Phase 6 complete. Attack prevention is fully functional:
- Downed players cannot perform melee attacks
- Downed players cannot fire projectiles (bow, crossbow, trident)
- Attack inputs produce no animations or sounds when blocked

Ready for Phase 7 (crawl pose) or Phase 8 (cinematic camera) work.
