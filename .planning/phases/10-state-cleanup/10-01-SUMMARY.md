---
phase: 10-state-cleanup
plan: 01
subsystem: state-management
tags: [lifecycle, disconnection, reconnect, penalty, fabric-api]

# Dependency graph
requires:
  - phase: 07-crawl-pose
    provides: CrawlPoseHandler for pose management, DownedPlayersState for state tracking
provides:
  - Disconnect/reconnect lifecycle handling for downed state
  - Escape tracking (transient, not persisted)
  - Reconnect penalty system (2 HP, 3s immunity, broadcast message)
  - PlayerConnectionHandler with event registration
affects: [state-management, multiplayer-behavior]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Transient state tracking pattern: escape flags cleared on server restart"
    - "Lifecycle event handling via Fabric ServerPlayConnectionEvents"
    - "Reconnect penalty using vanilla timeUntilRegen field"

key-files:
  created:
    - src/main/java/com/dread/death/PlayerConnectionHandler.java
  modified:
    - src/main/java/com/dread/death/DownedPlayersState.java
    - src/main/java/com/dread/DreadMod.java

key-decisions:
  - "Escape tracking is transient (not persisted) - intentionally clears on server restart per design"
  - "Use vanilla timeUntilRegen field for 3-second damage immunity on reconnect"
  - "Broadcast escape message to all players for narrative consistency"

patterns-established:
  - "Lifecycle event pattern: DISCONNECT clears state, JOIN applies penalties"
  - "Escape tracking pattern: mark on disconnect, check on join, clear after penalty"

# Metrics
duration: 2.25min
completed: 2026-01-26
---

# Phase 10 Plan 01: State Cleanup Summary

**Disconnect/reconnect lifecycle handling with escape tracking prevents downed state from persisting across world boundaries**

## Performance

- **Duration:** 2.25 min
- **Started:** 2026-01-27T00:39:23Z
- **Completed:** 2026-01-27T00:41:38Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Implemented escape tracking in DownedPlayersState (transient, in-memory only)
- Created PlayerConnectionHandler for disconnect/join event lifecycle management
- Disconnect clears downed state and marks player as escaped
- Reconnect applies penalty: 2 HP, 3 seconds damage immunity, broadcast message to all players
- Registered lifecycle handler in DreadMod initialization

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend DownedPlayersState with escape tracking** - `3a3d777` (feat)
2. **Task 2: Create PlayerConnectionHandler for disconnect/join events** - `5c9faf9` (feat)
3. **Task 3: Register PlayerConnectionHandler in DreadMod** - `b49d716` (feat)

## Files Created/Modified

- `src/main/java/com/dread/death/DownedPlayersState.java` - Added transient escapedPlayers HashSet and three methods (markEscapedPlayer, wasEscapedPlayer, clearEscapedPlayer) for tracking players who disconnect while downed
- `src/main/java/com/dread/death/PlayerConnectionHandler.java` - NEW: Handles ServerPlayConnectionEvents.DISCONNECT (clear state, mark escape) and ServerPlayConnectionEvents.JOIN (apply penalty, broadcast, grant immunity)
- `src/main/java/com/dread/DreadMod.java` - Added PlayerConnectionHandler.register() call in onInitialize()

## Decisions Made

**Escape tracking intentionally transient:**
- Server restart clears all escape flags (per CONTEXT.md design)
- escapedPlayers field NOT serialized in writeNbt()
- Avoids cross-restart penalty application for old disconnects

**Vanilla damage immunity field:**
- Used player.timeUntilRegen for 3-second immunity
- Standard Minecraft mechanic for post-damage invulnerability
- 60 ticks = 3 seconds at 20 ticks/second

**Broadcast message for narrative:**
- All players see "{player} narrowly escaped the Dread" on reconnect
- Maintains horror atmosphere and multiplayer awareness
- Uses server.getPlayerManager().broadcast()

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all tasks compiled and built successfully on first attempt.

## Next Phase Readiness

**Ready for 10-02 (Dimension Change Handling):**
- DownedPlayersState escape tracking foundation complete
- CrawlPoseHandler.exitCrawlPose() available for dimension transitions
- PlayerConnectionHandler pattern established for other lifecycle events

**Bug fix status:**
- ✅ Disconnecting while downed now clears downed state
- ✅ Reconnecting after escape applies 2 HP penalty
- ✅ Reconnecting after escape grants 3s damage immunity
- ✅ Reconnecting after escape broadcasts message to all players
- 🔲 Dimension changes still need handling (10-02)

**Known limitation:**
- Dimension changes (Nether/End portal) currently DO clear downed state incorrectly
- 10-02 will implement ServerPlayConnectionEvents to distinguish dimension change from true disconnect

---
*Phase: 10-state-cleanup*
*Completed: 2026-01-26*
