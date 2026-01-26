---
phase: 07-crawl-pose
plan: 02
subsystem: gameplay
tags: [mixin, movement-restriction, downed-state, server-side]

# Dependency graph
requires:
  - phase: 03-death-revival-system
    provides: DownedPlayersState with isDowned() check
provides:
  - Server-side movement restrictions for downed players
  - Jump blocking via PlayerJumpMixin
  - Sprint blocking via PlayerSprintMixin
  - Block interaction blocking via PlayerInteractionMixin
affects: [07-crawl-pose, 08-cinematic-death]

# Tech tracking
tech-stack:
  added: []
  patterns: [server-side-mixin-pattern, instanceof-pattern-in-mixin]

key-files:
  created:
    - src/main/java/com/dread/mixin/PlayerJumpMixin.java
    - src/main/java/com/dread/mixin/PlayerSprintMixin.java
    - src/main/java/com/dread/mixin/PlayerInteractionMixin.java
  modified:
    - src/main/resources/dread.mixins.json

key-decisions:
  - "Target Entity.setSprinting() instead of PlayerEntity - setSprinting defined on Entity"
  - "Only block sprinting=true, allow sprinting=false to prevent input ghosting"
  - "Return ActionResult.FAIL for block interactions - silently denies without error"

patterns-established:
  - "Server-side mixins in src/main/java/com/dread/mixin/ registered in 'mixins' array"
  - "instanceof ServerPlayerEntity pattern for server-only checks in entity mixins"

# Metrics
duration: 2.6min
completed: 2026-01-26
---

# Phase 7 Plan 2: Movement Restrictions Summary

**Server-side mixins blocking jump, sprint, and block interactions when downed via DownedPlayersState.isDowned() checks**

## Performance

- **Duration:** 2.6 min
- **Started:** 2026-01-26T03:18:00Z
- **Completed:** 2026-01-26T03:20:39Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- Downed players cannot jump (LivingEntity.jump() cancelled at HEAD)
- Downed players cannot sprint (Entity.setSprinting() cancelled for sprinting=true)
- Downed players cannot interact with blocks (ServerPlayerInteractionManager.interactBlock() returns FAIL)
- All three server-side mixins registered in dread.mixins.json

## Task Commits

Each task was committed atomically:

1. **Task 1: Create PlayerJumpMixin to block jumping** - `3ba8419` (feat)
2. **Task 2: Create PlayerSprintMixin to block sprinting** - `b9dab5a` (feat)
3. **Task 3: Create PlayerInteractionMixin and register mixins** - `a5d8dc4` (feat)

## Files Created/Modified
- `src/main/java/com/dread/mixin/PlayerJumpMixin.java` - Cancels LivingEntity.jump() when downed
- `src/main/java/com/dread/mixin/PlayerSprintMixin.java` - Cancels Entity.setSprinting(true) when downed
- `src/main/java/com/dread/mixin/PlayerInteractionMixin.java` - Returns ActionResult.FAIL for interactBlock when downed
- `src/main/resources/dread.mixins.json` - Registered all three server-side mixins

## Decisions Made
- **Entity vs PlayerEntity for sprint mixin:** setSprinting() is defined on Entity, not PlayerEntity. Targeting PlayerEntity caused mixin warning. Fixed by targeting Entity with instanceof check.
- **Input ghosting prevention:** Only block sprinting=true, allow sprinting=false to pass through. Prevents buffered sprint input from triggering on revival.
- **Silent interaction denial:** Return ActionResult.FAIL instead of throwing exception. User sees no error, interaction simply doesn't happen.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Fixed mixin target for PlayerSprintMixin**
- **Found during:** Task 2 (PlayerSprintMixin creation)
- **Issue:** Plan specified @Mixin(PlayerEntity.class) but setSprinting() is on Entity class
- **Fix:** Changed mixin target to Entity.class while keeping instanceof ServerPlayerEntity check
- **Files modified:** src/main/java/com/dread/mixin/PlayerSprintMixin.java
- **Verification:** Build compiles without mixin descriptor warnings
- **Committed in:** b9dab5a (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (blocking issue)
**Impact on plan:** Necessary fix for correct mixin targeting. No scope creep.

## Issues Encountered
None - all issues resolved via deviation rules.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Movement restrictions complete, integrates with existing DownedPlayersState
- Ready for pose animation work in 07-01 (parallel plan)
- No blockers for Phase 8 (cinematic death)

---
*Phase: 07-crawl-pose*
*Completed: 2026-01-26*
