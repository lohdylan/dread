# Phase 8: Cinematic Enhancement - Context

**Gathered:** 2026-01-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Death cinematic delivers intense, terrifying experience with extended grab and camera effects. Includes camera shake during grab, configurable for accessibility, with audio synchronized to animation timing. Effects must work without causing motion sickness at 30/60/144 FPS.

</domain>

<decisions>
## Implementation Decisions

### Camera Shake Feel
- Sharp violent jolts — sudden, jarring impacts like being physically grabbed
- Big hit at start, fade out — maximum impact on initial grab, then diminishes
- Pure chaos direction — random in all directions, disorienting
- Subtle magnitude (2-3 degrees) — noticeable but controlled, immersive without extreme
- Medium decay (0.7-1s) — lingers slightly for aftershock feeling

### Grab Animation Timing
- Duration: 1.5-2 seconds total — quick and brutal for shock value
- Pacing: Front-loaded violence — explosive grab, then quick kill (jump scare pounce style)
- Entity proximity: Very close — entity fills view, can't look away from the horror

### Audio Synchronization
- Grab impact: Sudden loud sting — classic jump scare audio spike, visceral shock
- Sustained audio: Entity sounds — growling, breathing, wet sounds for creature presence
- Kill moment: Final impact sound — punctuate death with decisive audio hit
- Audio ducking: Full duck — all other sounds nearly muted, focus on the horror

### Motion Sickness Prevention
- Config: Intensity slider (0-100%) — players tune to comfort level
- Compensation: Yes — boost red vignette + screen flash when shake is reduced
- FPS handling: Auto-reduce at low FPS — detect <45 FPS and reduce intensity automatically
- Default: 100% full horror — they can reduce if needed
- No first-time warning — jump straight into horror for maximum surprise
- Mod controls only — our slider is the only control, don't tie to Minecraft accessibility options

### Claude's Discretion
- Player control during grab (complete lock vs futile struggling)
- Exact exponential decay curve implementation
- Specific audio timing offsets within the 1.5-2s window

</decisions>

<specifics>
## Specific Ideas

- Front-loaded violence pacing — like a jump scare pounce, not a slow creeping death
- Entity should fill the screen during grab — can't look away from the cosmic horror
- Full audio ducking creates isolation — world goes quiet, only you and the monster exist
- Compensation effects (vignette + flash) ensure players who reduce shake still feel intensity

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 08-cinematic-enhancement*
*Context gathered: 2026-01-25*
