---
phase: 11-single-player-forgiveness
plan: 04
subsystem: ui
tags: [hud, client, rendering, minecraft-ui, mercy-mode]

# Dependency graph
requires:
  - phase: 11-03
    provides: Mercy mode network synchronization from server to client
provides:
  - MERCY/NO MERCY mode indicator on HUD
  - Mode-specific timer coloring (orange for MERCY, yellow-to-red for NO MERCY)
  - Client-side mercy mode storage and live updates
affects: [11-05]

# Tech tracking
tech-stack:
  added: []
  patterns: [client-side mercy mode state tracking, mode-specific UI theming]

key-files:
  created: []
  modified:
    - src/client/java/com/dread/client/DownedStateClientHandler.java
    - src/client/java/com/dread/client/DownedHudOverlay.java
    - src/client/java/com/dread/DreadClient.java

key-decisions:
  - "MERCY mode uses solid orange color throughout (no dramatic transitions)"
  - "NO MERCY mode uses yellow-to-red gradient timer for dramatic effect"
  - "Mode indicator displayed above DOWNED label for clear visibility"
  - "Both mode updates live when mercy mode changes during gameplay"

patterns-established:
  - "Mode-specific UI theming: mercy mode = orange/amber, no mercy = red/horror"
  - "Client handler stores mode state, HUD queries via getter for rendering"

# Metrics
duration: 3min
completed: 2026-01-27
---

# Phase 11 Plan 04: Mercy Mode UI Summary

**MERCY/NO MERCY mode indicator on HUD with orange timer for singleplayer, red gradient for multiplayer**

## Performance

- **Duration:** 3 min
- **Started:** 2026-01-27T01:55:57Z
- **Completed:** 2026-01-27T01:58:31Z
- **Tasks:** 3
- **Files modified:** 3

## Accomplishments
- Client handler tracks mercy mode from server packets
- HUD displays "MERCY" in orange or "NO MERCY" in red above DOWNED label
- MERCY mode uses solid orange timer (calm, forgiving visual)
- NO MERCY mode uses yellow-to-red gradient timer (dramatic, escalating)
- Mode updates live when server sends mode changes

## Task Commits

Each task was committed atomically:

1. **Task 1: Add mercy mode storage to DownedStateClientHandler** - `d4c0dd7` (feat)
2. **Task 2: Update DreadClient to pass mercy mode to handler** - `8788a5e` (feat)
3. **Task 3: Update DownedHudOverlay to display mode indicator and colored timer** - `91ac37c` (feat)

## Files Created/Modified
- `src/client/java/com/dread/client/DownedStateClientHandler.java` - Stores isMercyMode field, getter/setter methods
- `src/client/java/com/dread/DreadClient.java` - Passes isMercyMode from network packet to handler
- `src/client/java/com/dread/client/DownedHudOverlay.java` - Renders MERCY/NO MERCY indicator and mode-specific colored timer

## Decisions Made

**Mode-specific color schemes:**
- MERCY mode: Solid orange throughout (COLOR_ORANGE = 0xFFFFAA00) - calm, forgiving
- NO MERCY mode: Red text + yellow-to-red gradient timer - dramatic, escalating threat

**UI layout:**
- Mode indicator ("MERCY" or "NO MERCY") positioned at centerY - 50
- DOWNED label at centerY - 30 (uses mode color)
- Timer at centerY - 10 (uses mode-specific coloring)

**Live updates:**
- setMercyMode() method allows mode changes during countdown
- HUD queries isMercyMode() each frame for real-time updates

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - straightforward UI implementation.

## Next Phase Readiness

Mercy mode UI complete. Ready for Phase 11 Plan 05 (final integration testing).

All visual feedback in place:
- Players see "MERCY" in orange when in singleplayer (30s timer, normal respawn)
- Players see "NO MERCY" in red when in multiplayer (300s timer, spectator death)
- Timer colors clearly distinguish the two modes

---
*Phase: 11-single-player-forgiveness*
*Completed: 2026-01-27*
