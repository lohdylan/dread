# Phase 1: Foundation & Entity - Context

**Gathered:** 2026-01-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Dread entity exists as a visible, animated horror creature with proper Fabric mod infrastructure. This includes the entity model, GeckoLib animations, light extinguishing behavior, and multiple structural variants. AI behavior, sounds, and death mechanics are separate phases.

</domain>

<decisions>
## Implementation Decisions

### Creature Appearance
- Humanoid form — bipedal, human-like silhouette but wrong
- Slightly taller than player (2-2.5 blocks) — looms over without being massive
- Face/arm tentacles — Cthulhu-inspired appendages
- Color palette: Claude's discretion (balance visibility in darkness with horror aesthetic)

### Animation Style
- Jerky and wrong movement — stutters, moves in unsettling bursts
- Subtle twitching idle — small unsettling movements, head tilts when standing still
- Attack animation: Claude's discretion (picks for jump scare impact)
- Required animations:
  - Idle (with twitching)
  - Walk (jerky)
  - Attack
  - Spawn/appear (how it materializes)
  - Despawn/vanish (how it disappears)
  - Head tracking (head follows player even when body still)

### Multiple Forms
- 2-3 structural variants for v1
- Progress-based selection — scarier variants appear as game days increase
- Escalation through: more tentacles + more distorted proportions
- Example progression: Day 1-3 = base form, Day 4-7 = more tentacles, Day 8+ = most distorted

### Light Extinguishing
- Torches only (not lanterns, campfires, or glowstone)
- One by one timing — torches flicker out randomly over seconds (not instant)
- Range: Claude's discretion (balance gameplay with horror)
- Restoration: Claude's discretion (balance horror with not breaking player's base)

### Claude's Discretion
- Color palette for Dread
- Attack animation design
- Light extinguishing range
- Whether torches relight after Dread leaves
- Exact spawn/despawn visual effects

</decisions>

<specifics>
## Specific Ideas

- Movement should feel "wrong" — like something that doesn't belong in this world
- Head tracking creates constant sense of being watched
- Tentacles are the signature Cthulhu element — make them prominent
- Twitching idle prevents Dread from ever looking "safe" or predictable

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 01-foundation-entity*
*Context gathered: 2026-01-23*
