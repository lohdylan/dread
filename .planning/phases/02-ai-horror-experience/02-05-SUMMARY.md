---
phase: 02-ai-horror-experience
plan: 05
subsystem: audio-system
tags: [minecraft, audio, horror, proximity-detection, ambient-soundscape, geckolib]

# Dependency graph
requires:
  - phase: 02-03
    provides: DreadEntity with tick() infrastructure and spawn placement
  - phase: 02-04
    provides: DreadSoundManager with priority-based sound system and playProximitySound()
provides:
  - Entity-driven proximity audio triggering every 2 seconds when player within range
  - Ambient tension soundscape with randomized intervals (10-30 seconds)
  - Unnatural silence effect (volume decreases when Dread within 8 blocks)
  - Tension scaling based on mining activity and world progression
  - Complete layered audio atmosphere (ambient + proximity + fake-outs + jump scares)
affects: [02-06-human-verification, phase-03-death-mechanics]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Proximity detection using getClosestPlayer() within entity tick()"
    - "Ambient interval randomization using nextBetween() for unpredictability"
    - "Unnatural silence via volume reduction near entity (counter-intuitive horror)"

key-files:
  created: []
  modified:
    - src/main/java/com/dread/entity/DreadEntity.java
    - src/main/java/com/dread/sound/DreadSoundManager.java

key-decisions:
  - "Proximity sound cooldown 2 seconds (40 ticks) prevents audio spam while maintaining presence"
  - "Ambient interval 20s ± 10s creates unpredictable soundscape timing"
  - "Tension calculation: (blocksMined * 0.01) + (worldDay * 0.02) for balanced escalation"
  - "Unnatural silence threshold 8 blocks matches torch extinguishing range for thematic consistency"

patterns-established:
  - "Entity tick() calls multiple handler methods (handleTorchExtinguishing, handleProximitySound)"
  - "Sound manager tick() manages multiple timing systems (jumpscare protection, ambient intervals)"
  - "Tension-based probability gates for ambient sounds (50% of calculated tension value)"

# Metrics
duration: 2.3min
completed: 2026-01-24
---

# Phase 2 Plan 5: Dynamic Audio System Summary

**Proximity-triggered and ambient tension audio with unnatural silence effect creating complete layered horror atmosphere**

## Performance

- **Duration:** 2.3 min (137 seconds)
- **Started:** 2026-01-24T07:28:59Z
- **Completed:** 2026-01-24T07:31:16Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments
- Entity-driven proximity audio triggers every 2 seconds when player within 16 blocks
- Ambient tension soundscape with randomized intervals prevents predictability
- Unnatural silence effect intensifies horror when Dread is nearby (volume drops within 8 blocks)
- Tension escalation scales with player mining activity and world day progression
- Complete audio atmosphere combining ambient + proximity + fake-outs + jump scares with priority management

## Task Commits

Each task was committed atomically:

1. **Task 1: Add proximity audio triggers to DreadEntity** - `0b1b8f2` (feat)
2. **Task 2: Add ambient tension system to DreadSoundManager** - `545b3cf` (feat)

**Plan metadata:** (pending final commit)

## Files Created/Modified
- `src/main/java/com/dread/entity/DreadEntity.java` - Added handleProximitySound() method called from tick(), triggers audio every 2 seconds when player within 16 blocks
- `src/main/java/com/dread/sound/DreadSoundManager.java` - Added playAmbientTension() with randomized intervals, unnatural silence detection, tension scaling, and playDistantWhispers() for directional audio

## Decisions Made

**Proximity sound integration:**
- Cooldown set to 40 ticks (2 seconds) to maintain presence without overwhelming audio channels
- Detection range 24 blocks with audio trigger at 16 blocks for optimal horror timing
- Uses existing DreadSoundManager.playProximitySound() with unnatural silence mechanics

**Ambient tension system:**
- Base interval 400 ticks (20 seconds) with ±200 tick variance for unpredictability
- Tension calculation: `min(1.0, blocksMined * 0.01 + worldDay * 0.02)` balances escalation
- 50% probability gate (tension * 0.5) prevents constant ambient noise
- Unnatural silence: volume 0.05-0.15 when Dread within 8 blocks, 0.2-0.5 otherwise
- Checks for Dread entities within expanded bounding box (8 block radius)

**Audio layering:**
- All sounds respect jumpscare priority (3-second exclusive window)
- Ambient plays per-player based on individual tension values
- Proximity sounds triggered from entity position for proper spatial audio
- Distant whispers use random offsets for directional cues

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## Next Phase Readiness

**Complete horror audio experience ready for human verification (02-06):**
- All audio systems integrated and functional
- Proximity detection working via entity tick
- Ambient tension provides constant unease
- Unnatural silence effect amplifies nearby threat
- Sound priority management prevents channel exhaustion

**Ready for Phase 3 (Death Mechanics):**
- Audio infrastructure supports cinematic death sequence
- Jump scare audio system proven and integrated
- Entity tick() pattern established for additional mechanics

**No blockers or concerns.**

---
*Phase: 02-ai-horror-experience*
*Completed: 2026-01-24*
