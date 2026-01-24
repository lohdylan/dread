---
phase: 02-ai-horror-experience
plan: 03
type: execution
status: complete
subsystem: ai-behavior
tags: [ai-goals, spawn-mechanics, weeping-angel, jump-scare]

requires:
  - 02-01 # Sound events for jump scare
  - 02-02 # Spawn probability system

provides:
  - Weeping Angel freeze mechanic (StareStandoffGoal - from 02-04)
  - Vanish behavior after stare timeout (VanishGoal)
  - Turn-around spawn behind player
  - Jump scare sound on spawn

affects:
  - 02-04 # Turn-around detection will use these AI goals
  - 02-05 # Dynamic audio will integrate with spawn events
  - 03-* # Death mechanics will build on entity behaviors

tech-stack:
  added: []
  patterns:
    - AI Goal priority system for behavior control
    - FOV-based player detection with raycast obstruction
    - Ground-level spawn position adjustment

key-files:
  created:
    - src/main/java/com/dread/entity/ai/VanishGoal.java
  modified:
    - src/main/java/com/dread/entity/DreadEntity.java
    - src/main/java/com/dread/spawn/DreadSpawnManager.java
  note: StareStandoffGoal.java already created in plan 02-04

decisions:
  - id: ai-goal-priorities
    choice: VanishGoal (1), StareStandoffGoal (2), MeleeAttack (3)
    rationale: Vanishing must override all other behaviors; freeze takes priority over combat
  - id: stare-timeout
    choice: 600 ticks (30 seconds)
    rationale: Long enough to be unsettling but not tedious
  - id: spawn-distance
    choice: Random 3-8 blocks behind player
    rationale: Close enough for scare, far enough player can't immediately see edge of render

metrics:
  duration: 4.5 min
  completed: 2026-01-24
---

# Phase 2 Plan 3: Spawn Triggering & AI Behaviors Summary

**One-liner:** Turn-around spawn mechanic with Weeping Angel freeze behavior and vanish timeout using AI goal system

## What Was Built

### Core Spawn Mechanic
- **Turn-around spawn**: Dread spawns 3-8 blocks behind player when probability check passes
- **Ground adjustment**: Spawn position automatically finds solid ground below spawn point
- **Jump scare audio**: DREAD_JUMPSCARE sound plays at spawn location
- **Player facing**: Entity spawned facing player (yaw + 180°)

### AI Behaviors
- **VanishGoal**: Despawn with 2-second animation when entity flagged for vanishing
  - Highest priority (1) to override all other behaviors
  - Freezes entity during animation
  - Calls `discard()` after animation completes

- **StareStandoffGoal**: Weeping Angel / SCP-173 freeze mechanic (already existed from 02-04)
  - FOV threshold 0.85 (~31° cone)
  - Raycast obstruction check (player can't see through walls)
  - 600 tick (30 second) stare timeout triggers vanishing
  - Priority 2 (after vanish, before combat)

### Entity State Management
- Added `isVanishing` boolean field to DreadEntity
- Getter/setter methods for vanishing state
- Animation controller checks vanishing before death state
- Goal priorities ensure correct behavior sequencing

## Decisions Made

### AI Goal Priority Order
**Decision:** VanishGoal (1) > StareStandoffGoal (2) > MeleeAttack (3) > Wander (4) > LookAt (5) > LookAround (6)

**Rationale:**
- Vanishing must completely override all behaviors (can't attack while disappearing)
- Freeze mechanic prevents movement but allows combat if player looks away
- Standard hostile behaviors fall back when neither vanishing nor being watched

### Stare Timeout Duration
**Decision:** 600 ticks (30 seconds) before vanish

**Rationale:**
- Long enough to create "standoff" tension
- Short enough to not become tedious in gameplay
- Encourages player to either fight or look away

**Alternatives considered:**
- 1200 ticks (60s) - too long, breaks pacing
- 300 ticks (15s) - too short, no time for horror standoff

### Spawn Distance Range
**Decision:** Random 3-8 blocks behind player

**Rationale:**
- 3 blocks minimum: Close enough for effective jump scare
- 8 blocks maximum: Far enough player can't see at screen edges
- Random variation: Prevents predictability

**Implementation:** Uses `player.getRotationVector()` inverted and multiplied by random distance

## Implementation Notes

### Spawn Position Logic
```java
// Behind player = inverse of look direction
Vec3d behindDir = lookDir.multiply(-1.0);
double distance = 3.0 + random.nextDouble() * 5.0; // 3-8 blocks
Vec3d spawnPos = player.getPos().add(behindDir.multiply(distance));

// Find ground
while (world.getBlockState(groundPos).isAir() && groundPos.getY() > bottomY) {
    groundPos = groundPos.down();
}
groundPos = groundPos.up(); // Stand on top
```

**Edge case handling:**
- Falls to world bottom if no solid ground found
- Spawns in air if bedrock ceiling above player
- Uses BlockPos center offset (0.5) for entity placement

### FOV Detection
```java
double dot = playerLook.dotProduct(toEntity);
boolean inFOV = (dot >= 0.85); // ~31° cone
```

**Why 0.85:**
- Normal Minecraft FOV is 70-110° total
- Looking "at" something = central ~60° of view
- 0.85 dot product ≈ 31° from center = 62° total cone

### Raycast Obstruction
```java
HitResult hit = world.raycast(new RaycastContext(
    player.getEyePos(),
    dread.getPos().add(0, dread.getHeight() / 2, 0),
    ShapeType.COLLIDER,
    FluidHandling.NONE,
    player
));
return hit.getType() == Type.MISS; // No obstruction
```

**Targets entity center (height / 2):**
- More forgiving than targeting feet
- Aligns with player's natural eye level view

## Deviations from Plan

### Deviation 1: StareStandoffGoal Pre-existing
**Found during:** Task 1 setup

**Issue:** Plan specified creating StareStandoffGoal, but it already existed from plan 02-04

**Action:** Used existing implementation, only created VanishGoal

**Rationale:** Avoid duplicate work; existing implementation was correct

**Files affected:**
- Only VanishGoal.java created (new)
- StareStandoffGoal.java already committed in d969ad2

### Deviation 2: Removed TODO Comments
**Found during:** Task 2 implementation

**Issue:** Plan 02 left TODO markers for spawn triggering in DreadSpawnManager

**Action:** Replaced TODO with actual spawn implementation

**Rationale:** Plan 03 objective was to implement spawn triggering (not Phase 04+)

**Files modified:**
- DreadSpawnManager.java line 76: Added `spawnDread(world, player);`

## Test Strategy

### Manual Testing Required
1. **Turn-around spawn:**
   - Mine blocks to increase spawn probability
   - Look in one direction, then quickly turn around
   - Verify Dread appears 3-8 blocks behind initial view direction

2. **Weeping Angel mechanic:**
   - When Dread spawns, look directly at it
   - Verify entity freezes (stops moving)
   - Look away briefly, verify entity can move again

3. **Stare timeout:**
   - Stare at Dread continuously for 30 seconds
   - Verify entity plays despawn animation and disappears

4. **Jump scare sound:**
   - Listen for DREAD_JUMPSCARE sound on spawn
   - Verify sound plays at entity location (not player location)

### Edge Cases to Verify
- **Spawn in caves:** Ground detection works below player
- **Spawn on cliffs:** Entity doesn't spawn mid-air
- **Glass walls:** Raycast correctly detects obstruction through transparent blocks
- **Multiple players:** Each player tracked independently for stare detection

## Integration Points

### Consumed from Previous Plans
- **ModSounds.DREAD_JUMPSCARE** (02-01): Plays on spawn
- **SpawnProbabilityState** (02-02): resetAfterSpawn() called after entity spawned
- **ModEntities.DREAD** (01-01): Entity type for spawning

### Provided to Future Plans
- **Turn-around spawn logic** (02-04): Detection will trigger this spawn
- **AI goal framework** (02-05): Dynamic audio can check goal states
- **Vanish behavior** (future): Can be triggered by other conditions beyond stare timeout

## Known Limitations

1. **No vertical spawn prevention:**
   - Entity can spawn on ceiling if player upside-down in cave
   - Future: Add angle check to ensure horizontal spawn

2. **No obstruction check for spawn:**
   - Entity might spawn inside walls if player backs against them
   - Future: Raycast from player to spawn point, retry if obstructed

3. **Single target for stare:**
   - StareStandoffGoal only tracks closest watching player
   - If multiple players look, only closest counts for timeout
   - Future: Track all watchers, vanish only if NO watchers

4. **No spawn proximity limit:**
   - Multiple Dreads could spawn near each other
   - Future: Check for nearby Dread entities before spawning

## Performance Considerations

- **Raycast per tick:** StareStandoffGoal raycasts every tick while active
  - Mitigated: Only runs when player within 16 blocks
  - Mitigated: Only one raycast per Dread entity per tick

- **Ground-finding loop:** Could iterate many blocks if spawning high in air
  - Mitigated: Breaks at world bottom
  - Mitigated: Only runs once per spawn (not per tick)

## Next Phase Readiness

### Ready for Plan 02-04 (Turn-around Jump Scare)
- ✓ Spawn mechanics functional
- ✓ AI goals integrated
- ✓ Sound triggering works
- ⚠ Turn-around detection logic still needed (that's 02-04's job)

### Dependencies Satisfied
- StareStandoffGoal available for integration
- VanishGoal handles timeout despawn
- DreadEntity has vanishing state management

### Blockers/Concerns
None - plan completed as specified.

## Files Changed

### Created (1 file)
- `src/main/java/com/dread/entity/ai/VanishGoal.java` (58 lines)

### Modified (2 files)
- `src/main/java/com/dread/entity/DreadEntity.java` (+20 lines)
  - Added isVanishing field and accessors
  - Updated AI goal priorities
  - Animation controller checks vanishing state

- `src/main/java/com/dread/spawn/DreadSpawnManager.java` (+34 lines)
  - Added spawnDread() method
  - Imports for ModEntities, ModSounds, Vec3d, BlockPos
  - Replaced TODO with actual spawn call

## Commit History

| Commit | Message | Files |
|--------|---------|-------|
| 2d092e8 | feat(02-03): create VanishGoal for despawn animation | VanishGoal.java |
| 429a443 | feat(02-03): integrate AI goals and spawn trigger | DreadEntity.java, DreadSpawnManager.java |

**Total:** 2 commits, 3 files modified, ~112 lines added
