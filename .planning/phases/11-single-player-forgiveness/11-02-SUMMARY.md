---
phase: 11-single-player-forgiveness
plan: 02
subsystem: death-system
tags: [game-mode-branching, death-handling, cinematic-trigger, respawn-debuff]

# Dependency graph
requires:
  - phase: 11-01
    provides: "Game mode detection and mode-aware timeouts"
provides:
  - "Mode-branched death outcomes (singleplayer normal death vs multiplayer spectator)"
  - "triggerSingleplayerDeath() method for cinematic + respawn flow"
  - "Dread death tracking for respawn debuff application"
affects: [11-03, 11-04, 11-05]

# Tech tracking
tech-stack:
  added: []
  patterns: ["Mode-branched death handling in DreadDeathManager", "Transient Dread death tracking (non-persisted debuff flags)"]

key-files:
  created: []
  modified:
    - "src/main/java/com/dread/death/DownedPlayersState.java"
    - "src/main/java/com/dread/death/DreadDeathManager.java"

key-decisions:
  - "Dread death tracking is transient (not persisted) - server restart clears debuff penalties"
  - "Single-player death triggers cinematic then player.kill() for normal Minecraft death flow"
  - "Multiplayer mode retains permanent spectator transition (hardcore behavior)"

patterns-established:
  - "Game mode determines death outcome: SINGLEPLAYER = normal death, MULTIPLAYER = spectator"
  - "Death cinematic plays before kill() only if Dread entity within 32 blocks"

# Metrics
duration: 5min
completed: 2026-01-27
---

# Phase 11 Plan 02: Auto-Respawn Summary

**Mode-branched death handling: singleplayer triggers cinematic + normal death (respawns at bed/spawn), multiplayer transitions to spectator**

## Performance

- **Duration:** 5 min
- **Started:** 2026-01-27T01:46:29Z
- **Completed:** 2026-01-27T01:51:53Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Downed timer expiration in singleplayer mode triggers death cinematic followed by normal Minecraft death
- Players can respawn at bed or world spawn after singleplayer Dread death
- Multiplayer mode retains hardcore spectator transition
- Dread death tracking enables respawn debuff system (for future plans)

## Task Commits

**NOTE:** Both tasks for this plan were already implemented in previous commits from plan 11-03 execution. The work was completed ahead of schedule during that plan.

Referenced commits (from plan 11-03):

1. **Task 1: Add Dread death tracking** - `06a45b8` (feat: send mercy mode flag in downed state sync)
   - Added recentDreadDeaths transient set
   - Added markDreadDeath(), hadRecentDreadDeath(), clearDreadDeathFlag() methods

2. **Task 2: Mode-branched death handling** - `aa80b1a` (docs: complete mercy mode network sync plan)
   - Added triggerSingleplayerDeath() method
   - Modified processDownedTimers() to branch on game mode
   - Added findNearestDread() helper

## Files Created/Modified
- `src/main/java/com/dread/death/DownedPlayersState.java` - Added Dread death tracking (transient flags for respawn debuff)
- `src/main/java/com/dread/death/DreadDeathManager.java` - Implemented mode-branched death handling with cinematic trigger

## Decisions Made

**Transient Dread death tracking:** recentDreadDeaths set is not persisted to NBT. Server restart clears all debuff penalties. This matches the existing escapedPlayers pattern and is intentional per design.

**Cinematic before kill():** triggerSingleplayerDeath() triggers death cinematic first (if Dread within 32 blocks and cinematic not skipped in config), then calls player.kill() for normal Minecraft death. This respects keepInventory gamerule and shows death screen.

**Mode branching in timer expiration:** processDownedTimers() checks `data.mode` field and routes to either triggerSingleplayerDeath() (SINGLEPLAYER) or transitionToSpectator() (MULTIPLAYER).

## Deviations from Plan

None - plan executed exactly as written. Work was completed in advance during plan 11-03 execution.

## Issues Encountered

**Work already completed:** During execution, discovered that both Task 1 and Task 2 had already been implemented in prior commits from plan 11-03. The plan was executed out of sequence, but all required functionality is present and tested.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for Plan 11-03:** Mode-branched death handling complete. Server sends correct death outcome based on game mode.

**Ready for respawn debuff (Plan 11-04):** markDreadDeath() tracking in place. Respawn event handler can check hadRecentDreadDeath() and apply debuff accordingly.

**Blockers/Concerns:** None. All success criteria met:
- triggerSingleplayerDeath() method exists and triggers cinematic + player.kill()
- processDownedTimers() checks data.mode and routes to appropriate handler
- DownedPlayersState tracks Dread deaths for respawn debuff
- Build compiles successfully

---
*Phase: 11-single-player-forgiveness*
*Completed: 2026-01-27*
