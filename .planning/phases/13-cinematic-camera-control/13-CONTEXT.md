# Phase 13: Cinematic Camera Control - Context

**Gathered:** 2026-01-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Camera control transfers to cinematic system during death sequence, executing multi-stage camera path (pull back → zoom to face) with smooth interpolation and mixin coordination. This phase handles camera positioning and movement only — texture animation (Phase 14) and other visual effects are separate.

</domain>

<decisions>
## Implementation Decisions

### Camera Path Choreography
- Camera starts in immediate third-person (instant jump from first-person on grab)
- Pull-back frames both player and Dread together (emphasizes size difference)
- Jump cut from pull-back to face close-up (no smooth transition — jarring effect)
- Face close-up is locked still (no drift, sway, or continued movement)

### Timing and Pacing
- Pull-back phase lasts ~1.5 seconds (medium pacing)
- Jump cut happens at peak pull-back (no pause before cut)
- Face close-up lasts ~3 seconds (remaining time)
- No camera shake during pull-back or close-up (pure frozen terror aesthetic)
- Existing v1.2 camera shake system is NOT used during this cinematic

### Face Targeting Precision
- Dread's face fills full screen (edge-to-edge, overwhelming)
- Eyes are centered in frame (direct eye contact, soul-piercing)
- Same framing for all 3 Dread forms (consistent eye-level targeting)
- Camera must always force eyes visible (adjust to ensure eyes in frame in edge cases)

### Player Control Handoff
- Control loss is instant on grab (immediate takeover)
- Complete input lockout during 4.5s sequence (no player actions accepted)
- Letterbox/cinematic bars appear as visual feedback
- Bars snap in instantly with control loss (no fade)

### Claude's Discretion
- Exact interpolation curve for pull-back movement (Hermite, linear, ease-out)
- Third-person camera offset/angle during pull-back
- Letterbox bar thickness and color
- How to calculate Dread's eye position across forms

</decisions>

<specifics>
## Specific Ideas

- Jump cut creates jarring effect — intentional horror technique
- "Pure frozen terror" during face close-up — stillness is scary
- Letterbox bars signal "cinematic mode" — player knows they've lost control
- Eye contact is the core horror moment — everything leads to those eyes

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 13-cinematic-camera-control*
*Context gathered: 2026-01-27*
