# Phase 7: Crawl Pose - Context

**Gathered:** 2026-01-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Downed players visually crawl with synchronized animations. Players enter prone/crawling pose when downed, have visible crawling animation, pose syncs in multiplayer, and resets when revived or transitioning to spectator.

</domain>

<decisions>
## Implementation Decisions

### Crawl Animation Style
- Use Minecraft's built-in EntityPose.SWIMMING for prone position
- Limited camera pitch — player can't look straight up while crawling (reinforces being on the ground)

### Transition Effects
- Claude's discretion on enter/exit animations based on existing death cinematic flow
- Claude's discretion on transition visual effects and spectator transition handling

### Movement Constraints
- Very slow movement (25% of normal walking speed) — emphasizes vulnerability
- No jump, no sprint — completely grounded
- Subtle camera bob matching crawl movement — immersive but not disorienting
- No interactions (doors, buttons, items) — totally helpless, needs rescue

### Visual Feedback
- First-person: Visible reaching arms while crawling — reinforces the struggle
- Screen overlay: Blood vignette (red edges) — wounded feeling, urgent
- Third-person: Active crawling animation when moving — dynamic, clearly downed
- Particle effects: Blood drip particles around downed player — reinforces injury

### Claude's Discretion
- Exact crawl animation feel (desperate scramble vs injured drag vs military prone)
- Arm animation while crawling (animated vs static) — whatever works with swimming pose
- Enter/exit transition style (instant snap vs smooth blend vs collapse animation)
- Transition visual effects (flash, fade, etc.)
- Spectator transition treatment (fade to black vs direct switch)
- Camera bob intensity balancing immersion and comfort

</decisions>

<specifics>
## Specific Ideas

- Player should feel truly vulnerable and helpless when downed
- Blood vignette + blood drips + slow movement = "I'm dying and need help" feeling
- Third-person crawling animation should make it obvious to other players that someone is downed

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 07-crawl-pose*
*Context gathered: 2026-01-26*
