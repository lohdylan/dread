---
phase: 11-single-player-forgiveness
plan: 03
subsystem: networking
tags: [packets, network-sync, client-server, mercy-mode]

# Dependency graph
requires:
  - phase: 11-01
    provides: GameModeDetector and mercy mode storage in DownedPlayerData
provides:
  - DownedStateUpdateS2C packet extended with isMercyMode boolean
  - Server sends mercy mode flag based on stored game mode
  - Client receives and extracts mercy mode (ready for UI rendering)
affects: [11-04, ui, client-rendering]

# Tech tracking
tech-stack:
  added: []
  patterns: [packet-extension-pattern]

key-files:
  created: []
  modified:
    - src/main/java/com/dread/network/packets/DownedStateUpdateS2C.java
    - src/main/java/com/dread/death/DreadDeathManager.java
    - src/client/java/com/dread/DreadClient.java

key-decisions:
  - "isMercyMode = true means SINGLEPLAYER (MERCY), false means MULTIPLAYER (NO MERCY)"
  - "Mercy mode sent per-sync (not cached) to support mode changes during gameplay"

patterns-established:
  - "Packet extension: Add field to record + update CODEC tuple + update all constructors"
  - "Server derives packet fields from persistent state (DownedPlayerData.mode)"

# Metrics
duration: 3min
completed: 2026-01-27
---

# Phase 11 Plan 03: Mercy Mode Network Sync Summary

**Extended DownedStateUpdateS2C packet with isMercyMode boolean for client-side mercy mode display**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-27T01:46:36Z
- **Completed:** 2026-01-27T01:49:33Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- DownedStateUpdateS2C packet now includes mercy mode flag (isMercyMode)
- Server sync sends correct mercy mode based on player's stored DreadGameMode
- Client packet handler extracts mercy mode (ready for Plan 04 UI rendering)

## Task Commits

Each task was committed atomically:

1. **Task 1: Extend DownedStateUpdateS2C packet with mercy mode flag** - `eb500ef` (feat)
2. **Task 2: Update server sync to include mercy mode in packet** - `06a45b8` (feat)
3. **Task 3: Update client packet handler for new packet format** - `10fa782` (feat)

## Files Created/Modified
- `src/main/java/com/dread/network/packets/DownedStateUpdateS2C.java` - Added isMercyMode boolean parameter and updated CODEC
- `src/main/java/com/dread/death/DreadDeathManager.java` - Updated syncDownedStates() to send isMercyMode derived from data.mode
- `src/client/java/com/dread/DreadClient.java` - Updated DownedStateUpdateS2C handler to extract isMercyMode

## Decisions Made

**Mercy mode semantics:**
- isMercyMode = true → SINGLEPLAYER (MERCY mode, 30s respawn)
- isMercyMode = false → MULTIPLAYER (NO MERCY mode, 300s spectator)

**Packet extension pattern:**
1. Add parameter to record definition
2. Update CODEC with new PacketCodec.tuple entry
3. Update all packet constructors (server + client)

**Per-sync transmission:**
- Mercy mode sent with every downed state sync (not cached client-side)
- Supports potential future feature: mode changes during gameplay
- Derived from persistent DownedPlayerData.mode field

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

**Interdependent compilation:**
- Packet extension (Task 1) broke DreadDeathManager compilation until Task 2 completed
- Expected behavior - packet record and usage sites must be updated together
- Resolution: Completed both tasks before verification, committed separately for atomic history

## Next Phase Readiness

**Ready for Plan 04 (Mercy Mode UI Rendering):**
- Client now receives isMercyMode with every downed state update
- Flag is extracted and available (currently TODO comment)
- Plan 04 will pass isMercyMode to DownedStateClientHandler for UI color/mode indicator

**No blockers.**

---
*Phase: 11-single-player-forgiveness*
*Completed: 2026-01-27*
