# Phase 14: Animated Entity Textures - Context

**Gathered:** 2026-01-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Dread's texture transforms during the kill sequence with pulsing runes, writhing forms, and eye reveals synchronized to cinematic timing. The goal is making the static entity come alive in horrifying ways when the camera is locked on it during the death sequence.

</domain>

<decisions>
## Implementation Decisions

### Rune Pulsing Style
- Heartbeat rhythm — slow, organic pulse like a heartbeat
- Pulse accelerates toward climax as sequence progresses (syncs with building tension)
- Subtle glow intensity — visible but not overwhelming, horror through suggestion
- Deep red/crimson color — blood-like, visceral eldritch horror

### Form Distortion
- Subtle undulation — slow, barely perceptible shifting (you're not sure if it moved)
- Constant intensity — same gentle motion throughout (persistent wrongness)
- Tentacle appendages only — main body stays still, only appendages move
- Visible during normal gameplay at subtle level, intensifies during kill sequence

### Eye Reveal Timing
- Eyes open at face close-up start (when camera zooms to face, 3.0s remaining)
- Instant snap open — closed to open in one frame, jarring and startling
- Slit pupils — reptilian/demonic slits, predatory intelligence
- Yellow/amber emissive glow — predatory hunter eyes, distinct from crimson runes

### Animation Trigger States
- **Idle state:** Subtle tentacle writhing + steady dim rune glow + eyes closed
- **Kill sequence:** Accelerating rune pulse + intensified writhing + eyes snap open with glow
- Eyes only open during kill sequence — the reveal is special
- Dramatic transformation only activates during death sequence

### Claude's Discretion
- Exact pulse timing and BPM values
- UV animation implementation method (.mcmeta vs custom renderer)
- Texture frame count for animations
- Performance optimization approach for AMD GPUs

</decisions>

<specifics>
## Specific Ideas

- Heartbeat pulse should feel biological and oppressive — familiar rhythm made wrong
- Eye snap-open should be startling — one frame from closed to staring at you
- Yellow/amber eyes against crimson runes creates visual distinction (eyes feel special/different)
- Subtle idle animation means "it was always alive" — the kill sequence reveals what was already true

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 14-animated-entity-textures*
*Context gathered: 2026-01-27*
