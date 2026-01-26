---
phase: 07-crawl-pose
plan: 03
subsystem: client, rendering
tags: [fabric-api, mixin, hud, particles, camera, vignette]

# Dependency graph
requires:
  - phase: 07-01
    provides: Crawl pose system and state management
  - phase: 07-02
    provides: Movement restrictions preventing jump/sprint/attack
provides:
  - Blood vignette HUD overlay for downed state visual feedback
  - Server-side blood drip particles visible to all players
  - Camera pitch limiting mixin preventing looking straight up when downed
affects: [08-cinematic-polish, visual-feedback, player-feedback]

# Tech tracking
tech-stack:
  added: []
  patterns: [HudRenderCallback for overlays, Camera mixin for pitch limiting]

key-files:
  created:
    - src/client/java/com/dread/client/CrawlVignetteRenderer.java
    - src/client/java/com/dread/client/CrawlCameraHandler.java
    - src/client/java/com/dread/mixin/CrawlCameraMixin.java
  modified:
    - src/client/java/com/dread/DreadClient.java
    - src/main/java/com/dread/death/DreadDeathManager.java
    - src/main/resources/dread.mixins.json

key-decisions:
  - "Vanilla vignette.png with red tint - no custom asset needed"
  - "DRIPPING_LAVA particles for blood effect - existing particle type"
  - "MIN_PITCH -30 degrees - allows looking above horizon but not straight up"

patterns-established:
  - "HudRenderCallback for overlay effects with RenderSystem blending"
  - "Server-side particle spawning for multiplayer visibility"
  - "Camera mixin at setRotation TAIL for pitch clamping"

# Metrics
duration: 4min
completed: 2026-01-26
---

# Phase 7 Plan 3: Visual Feedback Summary

**Blood vignette overlay, dripping particles, and camera pitch limiting reinforce wounded/helpless feeling when downed**

## Performance

- **Duration:** 4 min
- **Started:** 2026-01-26T03:24:33Z
- **Completed:** 2026-01-26T03:28:33Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments
- Blood vignette overlay creates visual urgency with red-tinted screen edges
- Server-side blood drip particles signal injury state to all nearby players
- Camera pitch limited to -30 degrees prevents looking straight up while crawling
- All effects activate on downed state, disappear on revival

## Task Commits

Each task was committed atomically:

1. **Task 1: Create CrawlVignetteRenderer for blood vignette overlay** - `a67f908` (feat)
2. **Task 2: Add blood drip particle spawning in DreadDeathManager** - `995c15c` (feat)
3. **Task 3: Create camera pitch limiting mixin** - `3df45ae` (feat)

## Files Created/Modified
- `src/client/java/com/dread/client/CrawlVignetteRenderer.java` - HUD overlay rendering blood vignette when downed
- `src/client/java/com/dread/client/CrawlCameraHandler.java` - Camera pitch clamping logic
- `src/client/java/com/dread/mixin/CrawlCameraMixin.java` - Mixin intercepting Camera.setRotation
- `src/client/java/com/dread/DreadClient.java` - Added CrawlVignetteRenderer registration
- `src/main/java/com/dread/death/DreadDeathManager.java` - Added blood particle spawning in tick loop
- `src/main/resources/dread.mixins.json` - Added CrawlCameraMixin to client mixins

## Decisions Made
- Used vanilla vignette.png texture with red shader color (1.0, 0.15, 0.15) - no custom asset needed
- ParticleTypes.DRIPPING_LAVA for blood effect - existing red dripping particle
- Camera MIN_PITCH of -30 degrees - allows looking slightly above horizon but not straight up
- Particle spawning every 10 ticks (0.5s) with 1-2 particles - avoids particle spam while maintaining visibility

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None - all tasks completed successfully on first attempt.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 7 (Crawl Pose) complete with all 3 plans
- Downed state now has full visual/audio/movement immersion
- Ready for Phase 8 (Cinematic Polish) when scheduled

---
*Phase: 07-crawl-pose*
*Completed: 2026-01-26*
