---
phase: 14-animated-entity-textures
plan: 01
subsystem: client-rendering
tags: [texture-animation, cinematic, api, placeholder-assets]
requires:
  - 13-03-PLAN.md # Cinematic camera system with phase tracking
provides:
  - getCinematicTimer() API for texture synchronization
  - isInFaceCloseup() phase detection API
  - Placeholder texture files for animation states
affects:
  - 14-02-PLAN.md # Will use timer API for texture animation logic
  - 14-03-PLAN.md # Will reference placeholder texture files
tech-stack:
  added: []
  patterns:
    - timer-based-animation-sync
    - placeholder-asset-pattern
key-files:
  created:
    - src/main/resources/assets/dread/textures/entity/dread_base_pulse_0.png
    - src/main/resources/assets/dread/textures/entity/dread_base_pulse_0_glowmask.png
    - src/main/resources/assets/dread/textures/entity/dread_base_pulse_1.png
    - src/main/resources/assets/dread/textures/entity/dread_base_pulse_1_glowmask.png
    - src/main/resources/assets/dread/textures/entity/dread_base_pulse_2.png
    - src/main/resources/assets/dread/textures/entity/dread_base_pulse_2_glowmask.png
    - src/main/resources/assets/dread/textures/entity/dread_base_eyes_open.png
    - src/main/resources/assets/dread/textures/entity/dread_base_eyes_open_glowmask.png
  modified:
    - src/client/java/com/dread/client/DeathCinematicClientHandler.java
decisions: []
metrics:
  duration: 1.8m
  completed: 2026-01-27
---

# Phase 14 Plan 01: Cinematic Timer API and Placeholder Textures Summary

**One-liner:** Exposed cinematic timer/phase API (getCinematicTimer, isInFaceCloseup) and created 8 placeholder texture files for pulse and eyes_open animation states

## What Was Delivered

### Cinematic Timer API

Extended `DeathCinematicClientHandler` with two public static methods for texture animation synchronization:

**getCinematicTimer()**
- Returns current cinematic tick (0-90) during active cinematic
- Returns -1 when cinematic inactive
- Enables frame-accurate texture animation sync with 4.5s cinematic sequence

**isInFaceCloseup()**
- Returns true when cinematic in FACE_CLOSEUP phase (30-90 ticks)
- Returns false during THIRD_PERSON_PULLBACK or when inactive
- Triggers eye reveal texture during face close-up

### Placeholder Texture Files

Created 8 texture files (4 state + glowmask pairs) as placeholders for animation system:

**Pulse states (rune intensity during pull-back):**
- `dread_base_pulse_0.png` + glowmask (dim)
- `dread_base_pulse_1.png` + glowmask (medium)
- `dread_base_pulse_2.png` + glowmask (bright)

**Eyes open state (face close-up reveal):**
- `dread_base_eyes_open.png` + glowmask

All placeholders are copies of base texture - identical appearance until proper textures created by artist.

## Technical Foundation

**Timer synchronization pattern:**
```java
int tick = DeathCinematicClientHandler.getCinematicTimer();
if (tick >= 0) {
    // Cinematic active - select texture based on tick
}
```

**Phase detection pattern:**
```java
if (DeathCinematicClientHandler.isInFaceCloseup()) {
    // Use eyes_open texture
}
```

**Texture file structure:**
- AutoGlowingGeoLayer will find `_glowmask` files automatically
- Naming convention: `dread_base_{state}.png` + `dread_base_{state}_glowmask.png`
- Only base variant created (variant2/variant3 deferred until proper textures)

## Verification Results

✅ Build compiles successfully
✅ getCinematicTimer() and isInFaceCloseup() are public static
✅ 8 texture files present (4 states × 2 files each)
✅ All texture/glowmask pairs verified

## Tasks Completed

| Task | Description | Commit | Files |
|------|-------------|--------|-------|
| 1 | Add cinematic timer API to DeathCinematicClientHandler | 02de504 | DeathCinematicClientHandler.java |
| 2 | Create placeholder texture files for animation states | 38adfdc | 8 texture files (pulse 0/1/2, eyes_open + glowmasks) |

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

None - straightforward API extension and asset file creation.

## Known Limitations

**Placeholder textures:** All animation states currently look identical to base texture. Proper textures must be created by artist:
- Pulse 0/1/2: Rune intensity variations
- Eyes open: Visible eyes (currently closed in base texture)

**Variant coverage:** Only base variant has animation textures. Variant2 and variant3 will be added when proper textures created (out of scope for code plans).

## Next Phase Readiness

**Ready for 14-02:** Texture animation logic can now:
- Query cinematic timer for frame-accurate sync
- Detect face close-up phase for eye reveal
- Reference placeholder texture files (proper textures will slot in seamlessly)

**No blockers:** API complete, texture structure established.

## Integration Points

**Upstream dependencies:**
- Phase 13-03: Cinematic camera system provides timer (cinematicTimer field) and phase (currentPhase enum)

**Downstream consumers:**
- Phase 14-02: Will implement texture animation logic using getCinematicTimer()
- AutoGlowingGeoLayer: Will discover _glowmask files automatically

**Cross-cutting concerns:**
- Texture files follow AutoGlowingGeoLayer naming convention
- API methods are static for easy access from render thread

---
*Completed: 2026-01-27*
*Duration: 1.8 minutes*
*Commits: 02de504, 38adfdc*
