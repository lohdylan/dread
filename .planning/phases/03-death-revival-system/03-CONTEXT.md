# Phase 3: Death & Revival System - Context

**Gathered:** 2026-01-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Players experience unskippable cinematic death followed by downed state with cooperative revival mechanics. Killed players enter a 300-second downed state where teammates can revive them; if timer expires, they become permanent spectators. Solo players face permanent death stakes with no revival possible.

</domain>

<decisions>
## Implementation Decisions

### Cinematic Death Experience
- Camera locks onto Dread entity — player cannot look away during kill sequence
- Dread teleports instantly face-to-face with player (no approach animation)
- Sequence duration: 4-5 seconds — long enough to dread, short enough to not annoy
- Audio: Jumpscare sound triggers, then audio distorts/warps during the sequence

### Downed State Visuals
- Heavy blur + vignette — player can barely make out shapes, near-blind and vulnerable
- Visible countdown timer showing seconds remaining — player knows exactly when they die
- Slow crawl movement allowed — can crawl toward teammates but very slowly
- Normal chat communication — can type to teammates as usual

### Revival Interaction
- Range: 3-4 blocks (arm's reach) — close but not touching
- Duration: 3 seconds — quick, risky but doable mid-danger
- No interruption — once started, revival completes regardless of reviver taking damage or moving
- Progress UI: Circular/linear progress bar appears above downed player (visible to both)
- Trigger: Crouch near downed player to initiate

### Spectator Transition
- Instant transition — timer hits zero, immediately switch to spectator mode (no final moment)
- Free camera spectator mode — standard spectator controls, fly anywhere
- Spectators CAN see Dread when living players cannot — creates tension watching friends get stalked
- Death notice: Chat message only (e.g., "[Player] has been claimed by the Dread")

### Claude's Discretion
- Exact blur/vignette shader implementation
- Crawl movement speed (should feel desperately slow)
- Revival progress bar styling and positioning
- Spectator mode camera constraints (if any)

</decisions>

<specifics>
## Specific Ideas

- Dread teleporting face-to-face should feel like SCP-173 or similar instant horror
- Audio distortion during death should make the player feel like reality is breaking
- Spectators seeing Dread stalking their friends creates meta-tension even after death
- The 3-second uninterruptible revival creates "clutch moments" where teammates commit and succeed

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 03-death-revival-system*
*Context gathered: 2026-01-24*
