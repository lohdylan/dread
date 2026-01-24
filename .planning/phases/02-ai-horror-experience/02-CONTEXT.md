# Phase 2: AI & Horror Experience - Context

**Gathered:** 2026-01-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Dread intelligently stalks players with turn-around jump scares and full atmospheric soundscape. This includes the spawn mechanics, probability system, fake-out behaviors, and all audio design. The entity's visual appearance and animations were completed in Phase 1. Death mechanics and revival system are Phase 3.

</domain>

<decisions>
## Implementation Decisions

### Turn-around Spawn Mechanic
- Variable/random timing before spawn (not fixed delay) — keeps player guessing
- Random distance when spawning (sometimes close, sometimes distant) — variety in encounters
- FOV detection threshold: Claude's discretion
- Blocked view counts as "looking away" — facing a wall makes you vulnerable
- Cooldown after spawn: 30-60 second grace period
- Post-spawn behavior: Sometimes attacks, sometimes vanishes — unpredictable
- Subtle audio cue just before spawn — subconscious warning, not obvious

### Stare Mechanic (Weeping Angel style)
- Dread can't move while being watched — classic SCP-173 behavior
- Standoff resolution: Claude's discretion (vanish, forced blink, etc.)
- Looking at Dread freezes it, looking away lets it approach

### Light and Environment
- Spawns anywhere, but prefers darkness — higher chance in dark, not immune in light
- Spawns anytime (day or night equally) — no safe time of day
- All dimensions (Overworld, Nether, End) — nowhere is safe

### Combat
- Dread is damageable but very tanky — fighting back is desperate, not optimal
- Respawns after cooldown (minutes) — killing buys time, doesn't solve problem

### Multiplayer
- Targets one player at a time — spawns behind one specific player
- Visible to all players — friends can warn you ("BEHIND YOU!")
- Teamwork can save the target player

### Spawn Probability System
- Mining counter: Each block mined adds to spawn chance, resets after spawn
- Day escalation: Linear increase (Day 5 = 5x more likely than Day 1)
- Base probability: Occasional (10-20% per trigger on Day 1 with no mining)
- Danger indicator: Subtle, off-putting sound as danger rises — very slight, not obvious

### Fake-out Behaviors
- Types: All four (sounds only, distant glimpses, near-miss spawns, environmental effects)
- Ratio: More fake-outs than real (3:1) — constant paranoia, most scares are false alarms
- Escalation: Both fake-outs AND real spawns increase as days progress
- Safe zones: None — fake-outs can happen even in well-lit areas near bed

### Sound Design
- Ambient tension: Low droning/humming + distant whispers
- Jump scare audio: Sharp loud noise (classic scare chord)
- Proximity effect: Ambient sounds get quieter (unnatural silence) as Dread approaches
- Watching sound: Silent while watching — unnerving silence, must look to know

### Claude's Discretion
- Exact FOV detection threshold for "looking away"
- Variable spawn timing range (min/max seconds)
- Exact distance range for spawn positioning
- How the stare standoff resolves (vanish timer, forced blink, etc.)
- Mining counter thresholds and decay rate
- Specific probability percentages and escalation formula
- Fake-out audio/visual implementation details

</decisions>

<specifics>
## Specific Ideas

- Weeping Angel / SCP-173 mechanic — entity freezes when watched, moves when not
- The anticipation should be worse than the payoff — fake-outs build dread
- Mining should feel dangerous — the deeper you go, the more you attract it
- Friends shouting warnings is part of the multiplayer experience
- Unnatural silence before proximity creates dread better than loud cues
- Subtle audio for danger level — player shouldn't consciously notice until too late

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 02-ai-horror-experience*
*Context gathered: 2026-01-24*
