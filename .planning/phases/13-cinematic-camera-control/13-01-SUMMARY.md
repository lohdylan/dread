---
phase: 13
plan: 01
subsystem: camera-control
completed: 2026-01-27
duration: 5.1 min
tags: [camera, mixin, cinematic, v2.0]

requires:
  - 12-02  # Audio testing infrastructure
  - 11-01  # Camera shake system (reuses CameraMixin coordination)

provides:
  - Position-based camera control during death cinematic
  - Third-person pull-back phase (1.5s)
  - Face close-up phase (3.0s) with eye-level targeting
  - Mixin coordination for position and rotation control

affects:
  - 13-02  # Letterbox bars (will add visual feedback)
  - 14-01  # Texture animation (coordinates with face close-up timing)

tech-stack:
  added: []
  patterns:
    - "Mixin coordination with order priority (order=900)"
    - "Phase-based position calculation (hardcoded tick boundaries)"
    - "Camera position injection via Camera.update() mixin"

key-files:
  created: []
  modified:
    - src/client/java/com/dread/mixin/CameraMixin.java
    - src/client/java/com/dread/client/DeathCinematicClientHandler.java

decisions:
  - id: CAM-07
    choice: "Hardcoded tick boundaries over keyframe system"
    rationale: "4.5s sequence doesn't need spline curves, matches existing v1.2 phase pattern"
  - id: CAM-04
    choice: "Separate injections for position (update) and rotation (setRotation)"
    rationale: "Operate on different Camera fields, no conflicts, clear separation of concerns"
---

# Phase 13 Plan 01: Cinematic Camera Position Control Summary

**One-liner:** Position-based camera control with third-person pull-back and face close-up phases using Camera.update() mixin injection.

## What Was Built

Implemented v2.0 cinematic camera system that transfers control from player to cinematic choreography during death sequence. Camera executes a two-phase path:

1. **THIRD_PERSON_PULLBACK (0-30 ticks / 1.5s)**: Camera positioned behind and above player (5 blocks back, 2 blocks up), framing both player and Dread together
2. **FACE_CLOSEUP (30-90 ticks / 3.0s)**: Camera jump-cuts to Dread's face, positioned 0.4 blocks from eyes to avoid model clipping

Total cinematic duration: 4.5 seconds (90 ticks).

### Technical Implementation

**CameraMixin extensions:**
- Added `@Shadow` for `Vec3d pos` field access
- Added `@Inject` into `Camera.update()` at `@At("TAIL")` with `order = 900`
- Position injection applies offsets from `DeathCinematicClientHandler.getCameraPositionOffset()`
- Coordinates with existing `setRotation()` injection (rotation) without conflicts

**DeathCinematicClientHandler refactor:**
- Replaced 5-phase v1.2 system (IMPACT/LIFT/HOLD/RELEASE/SETTLE) with 2-phase v2.0 system
- New `CinematicPhase` enum: `THIRD_PERSON_PULLBACK`, `FACE_CLOSEUP`
- `getCameraPositionOffset()` returns `Vec3d` based on current phase
- `calculatePullbackPosition()`: Calculates offset behind player using yaw rotation
- `calculateFaceCloseupPosition()`: Positions camera between Dread's eyes and player
- Updated timing constants: `PULLBACK_END_TICKS=30`, `CINEMATIC_DURATION_TICKS=90`
- Removed old phase motion methods (impact dip, lift, wobble)
- Camera shake disabled during cinematic (returns 0.0f per CONTEXT.md "pure frozen terror" aesthetic)

## Task Breakdown

| Task | Name | Commit | Files Modified |
|------|------|--------|----------------|
| 1 | Extend CameraMixin with position injection | 5b99a32 | CameraMixin.java |
| 2 | Refactor DeathCinematicClientHandler with v2.0 phase system | c9ab878 | DeathCinematicClientHandler.java |
| 3 | Verify integration, mixin coordination, and build | 22314f8 | (verification only) |

## Design Patterns

**Mixin Coordination Pattern:**
- Two injections at same priority (`order=900`) but different methods
- `setRotation()` injection → modifies `this.yaw` and `this.pitch` (rotation fields)
- `update()` injection → modifies `this.pos` (position field)
- No shared mutable state, clear separation of concerns
- Both check `isCinematicActive()` before applying changes

**Phase-Based Position Calculation:**
- Hardcoded tick boundaries (CAM-07 decision)
- Position calculated per-frame based on phase
- Pull-back uses player yaw for behind-camera positioning
- Face close-up uses Dread eye position and direction to player

## Integration Points

**Called by:**
- `CameraMixin.dread$applyCinematicPosition()` → calls `getCameraPositionOffset()` every frame

**Calls to:**
- `MinecraftClient.getInstance().player` → for position and yaw
- `Entity.getEyePos()` → for Dread's eye position
- `Vec3d` math → for offset calculations

**Coordinates with:**
- Existing `setRotation()` injection (rotation shake system)
- `DeathCinematicClientHandler.tick()` (phase transitions)

## Deviations from Plan

None - plan executed exactly as written.

## Testing Notes

**Build verification:**
- Gradle build successful with no mixin application errors
- Both `@Inject` annotations registered at `order=900`
- Method signatures match between mixin and handler

**Runtime testing deferred:**
- In-game verification needed to confirm camera positions
- Face close-up targeting accuracy needs visual validation
- Jump cut timing (instant phase transition) needs feel testing

## Known Limitations

**Camera positioning:**
- Pull-back distance/height are hardcoded constants (5 blocks, 2 blocks)
- Face distance fixed at 0.4 blocks (may need adjustment per Dread form)
- No collision detection (camera can clip through walls/blocks)

**Phase transitions:**
- Jump cut is instant (no interpolation between phases)
- No easing on pull-back movement (linear position change)

**Integration gaps:**
- No letterbox bars yet (Phase 13 Plan 02)
- No texture animation coordination (Phase 14)
- Player control not locked (should disable input during cinematic)

## Next Phase Readiness

**Phase 13 Plan 02 (Letterbox Bars):**
- Ready to implement
- Add HUD overlay during `isCinematicActive()`
- Black bars at top/bottom of screen

**Phase 14 (Texture Animation):**
- Ready to coordinate
- Face close-up duration is 3.0s (60 ticks)
- Eye animation can sync with face close-up phase

**Concerns:**
- Camera collision with environment not handled (may produce jarring clips through walls)
- Face targeting assumes Dread eye position is consistent across forms (needs validation)
- No smooth interpolation on pull-back (may feel abrupt depending on player orientation)

## Metrics

- **Lines added:** ~100 (CameraMixin extension + position calculation methods)
- **Lines removed:** ~228 (old phase motion system removed)
- **Net change:** -128 lines (simplified from 5-phase to 2-phase system)
- **Build time:** ~2 minutes (full Gradle build)
- **Execution time:** 5.1 minutes (3 tasks)

---

*Executed: 2026-01-27*
*Autonomous: Yes*
*Commits: 5b99a32, c9ab878, 22314f8*
