---
phase: 07-crawl-pose
plan: 01
subsystem: death
tags: [mixin, pose, swimming, crawl, datatracker, fabric]

# Dependency graph
requires:
  - phase: 03-death-revival-system
    provides: DreadDeathHandler, RevivalInteractionHandler, DreadDeathManager, DownedStateClientHandler
provides:
  - CrawlPoseHandler utility for server-side pose management
  - PlayerPoseMixin for client-side pose persistence
  - Downed players render in prone/crawling pose
affects: [07-02, 07-03, 08-cinematic-death]

# Tech tracking
tech-stack:
  added: []
  patterns: [server-pose-client-mixin, datatracker-sync]

key-files:
  created:
    - src/main/java/com/dread/death/CrawlPoseHandler.java
    - src/client/java/com/dread/mixin/PlayerPoseMixin.java
  modified:
    - src/main/java/com/dread/death/DreadDeathHandler.java
    - src/main/java/com/dread/death/RevivalInteractionHandler.java
    - src/main/java/com/dread/death/DreadDeathManager.java
    - src/main/resources/dread.mixins.json

key-decisions:
  - "Use EntityPose.SWIMMING for crawl (vanilla prone pose, no custom animation needed)"
  - "Server sets pose via DataTracker, client mixin prevents vanilla reset"

patterns-established:
  - "Pose override: Server setPose() + client mixin cancels updatePose()"
  - "State check via DownedStateClientHandler.isDownedEffectActive()"

# Metrics
duration: 2min
completed: 2026-01-26
---

# Phase 7 Plan 1: Core Crawl Pose System Summary

**Server-side CrawlPoseHandler sets SWIMMING pose on downed, client-side PlayerPoseMixin prevents vanilla reset - enabling prone rendering during downed state**

## Performance

- **Duration:** 2 min
- **Started:** 2026-01-26T03:18:04Z
- **Completed:** 2026-01-26T03:20:38Z
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments
- CrawlPoseHandler utility with enterCrawlPose/exitCrawlPose methods
- Integrated pose changes at all three critical points (downed, revived, spectator transition)
- PlayerPoseMixin prevents vanilla from resetting pose every tick
- DataTracker automatically syncs pose to all clients

## Task Commits

Each task was committed atomically:

1. **Task 1: Create CrawlPoseHandler server-side utility** - `3539bb4` (feat)
2. **Task 2: Integrate pose changes into death/revival flow** - `53cd0da` (feat)
3. **Task 3: Create PlayerPoseMixin to prevent pose reset** - `79204d7` (feat)

## Files Created/Modified
- `src/main/java/com/dread/death/CrawlPoseHandler.java` - Server-side pose enter/exit utility
- `src/client/java/com/dread/mixin/PlayerPoseMixin.java` - Client mixin preventing vanilla pose reset
- `src/main/java/com/dread/death/DreadDeathHandler.java` - Calls enterCrawlPose on downed
- `src/main/java/com/dread/death/RevivalInteractionHandler.java` - Calls exitCrawlPose on revival
- `src/main/java/com/dread/death/DreadDeathManager.java` - Calls exitCrawlPose before spectator transition
- `src/main/resources/dread.mixins.json` - Added PlayerPoseMixin to client array

## Decisions Made
- Used EntityPose.SWIMMING for prone pose (vanilla provides crawling animation when moving)
- Exit pose BEFORE changeGameMode(SPECTATOR) since pose changes don't work in spectator mode
- Client mixin cancels vanilla updatePose() entirely when downed to prevent flickering

## Deviations from Plan
None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Core pose system complete and ready for camera adjustments (07-02)
- PlayerPoseMixin pattern established for future pose-related enhancements
- DOWN-03 foundation complete (EntityPose.SWIMMING triggers crawling animation)

---
*Phase: 07-crawl-pose*
*Completed: 2026-01-26*
