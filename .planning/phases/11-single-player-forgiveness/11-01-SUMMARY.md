---
phase: 11-single-player-forgiveness
plan: 01
subsystem: gameplay
tags: [game-mode-detection, downed-state, timeout-config, single-player, multiplayer]

# Dependency graph
requires:
  - phase: 03-downed-revival
    provides: DownedPlayerData and DownedPlayersState for downed state management
  - phase: 04-configuration-release-prep
    provides: DreadConfig and DreadConfigLoader for config system
provides:
  - Game mode detection utility (SP vs MP detection)
  - Mode storage in downed player data with NBT persistence
  - Separate timeout configurations for single-player (30s) and multiplayer (300s)
  - Mode-aware downed state initialization
affects: [11-02-auto-respawn, 11-single-player-forgiveness]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Game mode detection at downed entry time (not globally cached)"
    - "Mode-specific timeout configuration via separate config fields"

key-files:
  created:
    - src/main/java/com/dread/death/GameModeDetector.java
  modified:
    - src/main/java/com/dread/death/DownedPlayerData.java
    - src/main/java/com/dread/death/DownedPlayersState.java
    - src/main/java/com/dread/config/DreadConfig.java

key-decisions:
  - "Detect mode per-downed-instance (not cached globally) - mode may change during gameplay"
  - "Dedicated servers always MULTIPLAYER, integrated with 1 player is SINGLEPLAYER"
  - "Default to MULTIPLAYER for backwards compatibility when reading NBT"
  - "30-second single-player timeout (normal respawn), 300-second multiplayer timeout (spectator)"

patterns-established:
  - "GameModeDetector.detectMode() called at setDowned() time to capture mode"
  - "DreadGameMode enum stored in DownedPlayerData for mode-aware behavior"
  - "Config-driven timeouts via singleplayerDownedTimeout/multiplayerDownedTimeout fields"

# Metrics
duration: 4min
completed: 2026-01-27
---

# Phase 11 Plan 01: Game Mode Detection Infrastructure Summary

**Game mode detection (SP vs MP) at downed entry with mode-specific timeout configuration (30s SP, 300s MP)**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-27T01:39:37Z
- **Completed:** 2026-01-27T01:43:13Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- GameModeDetector utility with DreadGameMode enum for SP/MP classification
- Extended DownedPlayerData with mode field and NBT persistence
- Added separate config options (singleplayerDownedTimeout: 30s, multiplayerDownedTimeout: 300s)
- Mode-aware setDowned() that detects game mode and applies appropriate timeout

## Task Commits

Each task was committed atomically:

1. **Task 1: Create GameModeDetector utility and DreadGameMode enum** - `d552b44` (feat)
2. **Task 2: Extend DownedPlayerData with mode field and NBT serialization** - `4fac295` (feat)
3. **Task 3: Add config options and update DownedPlayersState.setDowned()** - `d6fa218` (feat)

## Files Created/Modified
- `src/main/java/com/dread/death/GameModeDetector.java` - Utility for detecting SP vs MP mode based on server type and player count
- `src/main/java/com/dread/death/DownedPlayerData.java` - Added mode field with NBT persistence and backwards compatibility
- `src/main/java/com/dread/death/DownedPlayersState.java` - Updated setDowned() to detect mode and use mode-based timeout
- `src/main/java/com/dread/config/DreadConfig.java` - Added singleplayerDownedTimeout and multiplayerDownedTimeout config fields

## Decisions Made

**1. Detect mode per-downed-instance, not globally**
- Rationale: Players may join/leave during gameplay (LAN server), mode must be captured at downed entry time

**2. Dedicated servers always MULTIPLAYER**
- Rationale: server.isSingleplayer() returns false for dedicated servers, which are inherently multiplayer contexts

**3. NBT backwards compatibility defaults to MULTIPLAYER**
- Rationale: Existing saves don't have mode field - default to safer MULTIPLAYER behavior (longer timeout)

**4. 30s single-player timeout vs 300s multiplayer**
- Rationale: Single-player has no rescue mechanics (solo), shorter timeout allows faster respawn. Multiplayer enables rescue, needs longer window.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all tasks completed without obstacles.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for next plan (11-02: Auto-Respawn):**
- Mode detection infrastructure complete
- DownedPlayerData stores mode for mode-aware behavior
- Config options available for user customization
- setDowned() correctly detects and stores mode

**Foundation established for:**
- Plan 11-02: Normal respawn in single-player (vs spectator in multiplayer)
- Future mode-aware behavior (different mechanics based on SP/MP context)

**No blockers.**

---
*Phase: 11-single-player-forgiveness*
*Completed: 2026-01-27*
