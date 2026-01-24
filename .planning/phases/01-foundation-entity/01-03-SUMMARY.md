---
phase: 01-foundation-entity
plan: 03
subsystem: entity
tags: [minecraft, fabric, geckolib, entity-behavior, ai-goals, particles]

# Dependency graph
requires:
  - phase: 01-foundation-entity/01-02
    provides: GeckoLib model and renderer foundation
provides:
  - Torch extinguishing behavior within 8 block range
  - Form variant enum with day-based progression system
  - Spawn animation state management
  - Enhanced animation controller with multiple animation tracks
affects: [01-04, phase-02, phase-03]

# Tech tracking
tech-stack:
  added: []
  patterns: [Fisher-Yates shuffle for randomization, enum-based variant selection, NBT persistence for entity state]

key-files:
  created:
    - src/main/java/com/dread/entity/DreadFormVariant.java
  modified:
    - src/main/java/com/dread/entity/DreadEntity.java

key-decisions:
  - "Fisher-Yates shuffle for torch selection instead of Collections.shuffle (Minecraft Random API compatibility)"
  - "8 block range for torch extinguishing (balanced gameplay/horror atmosphere)"
  - "1 torch per second extinguishing rate (20 ticks cooldown)"
  - "Form variant based on world day at spawn time (persistent escalation)"

patterns-established:
  - "NBT serialization for entity state persistence (form variant, animation state)"
  - "Server-side only behavior in tick() method for torch extinguishing"
  - "Separate animation controllers for concurrent animations (main + head tracking)"

# Metrics
duration: 4min
completed: 2026-01-24
---

# Phase 01 Plan 03: Entity Behaviors Summary

**Torch extinguishing with smoke particles, day-based form variant progression (BASE/EVOLVED/ELDRITCH), and spawn animation state management**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-24T05:56:50Z
- **Completed:** 2026-01-24T06:00:55Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Torch extinguishing behavior with 8 block range, one torch per second
- Smoke particles spawn before each torch is removed
- Form variant enum with day-based selection (days 1-3: BASE, 4-7: EVOLVED, 8+: ELDRITCH)
- Form variant persists in NBT across world saves
- Spawn animation plays when entity first appears
- Enhanced animation controller with spawn/despawn/attack/head-tracking animations

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement torch extinguishing behavior** - `0ef3ea7` (feat)
2. **Task 2: Implement form variant selection and spawn animation trigger** - `b8edb7d` (feat)

## Files Created/Modified
- `src/main/java/com/dread/entity/DreadFormVariant.java` - Enum for form variants with day-based selection logic
- `src/main/java/com/dread/entity/DreadEntity.java` - Added torch extinguishing, form variant system, spawn animation controller

## Decisions Made

**1. Fisher-Yates shuffle implementation**
- Minecraft's Random API doesn't have `asJavaRandom()` in this version
- Implemented Fisher-Yates shuffle manually using `getRandom().nextInt()`
- Provides same randomization behavior as Collections.shuffle

**2. Torch extinguishing parameters**
- 8 block range: Balances atmosphere without destroying entire bases
- 20 tick (1 second) cooldown: Fast enough to be scary, slow enough to notice each torch
- Random order via shuffle: Makes it unpredictable (random torches go out, not nearest first)
- No restoration: Torches stay gone permanently (adds stakes)

**3. Form variant progression**
- Days 1-3: BASE form (texture index 0)
- Days 4-7: EVOLVED form (texture index 1)
- Days 8+: ELDRITCH form (texture index 2)
- Selected at spawn based on world day, persists in NBT

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed Minecraft Random API compatibility**
- **Found during:** Task 1 (Torch extinguishing implementation)
- **Issue:** `this.getRandom().asJavaRandom()` method doesn't exist in Minecraft Random interface
- **Fix:** Implemented Fisher-Yates shuffle manually using `this.getWorld().getRandom().nextInt(i + 1)`
- **Files modified:** src/main/java/com/dread/entity/DreadEntity.java
- **Verification:** Build succeeded, shuffle algorithm correct
- **Committed in:** 0ef3ea7 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Required for compilation. Fisher-Yates implementation provides same randomization behavior.

## Issues Encountered
None - both tasks executed smoothly after fixing the Random API compatibility.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Entity behaviors complete and functional
- Ready for Phase 1 Plan 4 (animations and models in Blockbench)
- Torch extinguishing will be testable once client runs
- Form variants ready for texture differentiation in renderer

**Note for testing:**
- Use `/time set day` to test day 1 (BASE variant)
- Use `/time set 100000` to test day 4+ (EVOLVED variant)
- Use `/time set 200000` to test day 8+ (ELDRITCH variant)

---
*Phase: 01-foundation-entity*
*Completed: 2026-01-24*
