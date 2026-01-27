# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-26)

**Core value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.
**Current focus:** v1.2 Quick Fixes — Phase 11 (Single-Player Forgiveness)

## Current Position

Phase: 11 of 12 (Single-Player Forgiveness)
Plan: 02 of 05 complete
Status: In progress
Last activity: 2026-01-27 — Completed 11-03-PLAN.md

Progress: [█████████████████████░░░░░░░░░] 83% (35/42 estimated plans)

## Performance Metrics

**Velocity:**
- Total plans completed: 35
- Average duration: 3.3 min
- Total execution time: ~3.0 hours

## Accumulated Context

### Decisions

All v1.0 + v1.1 decisions documented in PROJECT.md Key Decisions table.

**Phase 9 (Cinematic Fix):**
- Apply camera shake at render-time via Camera.setRotation injection, not entity rotation modification (eliminates feedback loop)
- Use mixin order = 900 so shake applies before CrawlCameraMixin's pitch clamping (order 1000)

**Phase 10 (State Cleanup) - Plans 01-02:**
- Escape tracking is transient (not persisted) - intentionally clears on server restart per design
- Use vanilla timeUntilRegen field for 3-second damage immunity on reconnect
- Broadcast escape message to all players for narrative consistency
- Mixin injection for gamemode change (no Fabric event exists for this API)
- Void/kill bypass: DON'T trigger cinematic (cinematic.endCinematic() re-applies downed effects causing broken state after respawn)
- Send RemoveDownedEffectsS2C packet before allowing void/kill death to clear client state
- Exit crawl pose before vanilla death to prevent visual glitches

**Phase 11 (Single-Player Forgiveness) - Plans 01-03:**
- Detect mode per-downed-instance (not cached globally) - mode may change during gameplay
- Dedicated servers always MULTIPLAYER, integrated with 1 player is SINGLEPLAYER
- Default to MULTIPLAYER for backwards compatibility when reading NBT
- 30-second single-player timeout (normal respawn), 300-second multiplayer timeout (spectator)
- isMercyMode = true means SINGLEPLAYER (MERCY), false means MULTIPLAYER (NO MERCY)
- Mercy mode sent per-sync (not cached) to support mode changes during gameplay

### Pending Todos

None.

### Blockers/Concerns

**Build Environment:**
- Build requires `export JAVA_HOME="X:/Vibe Coding/jdk-21.0.6+7"` before Gradle commands

**Known Bugs (v1.2 targets):**
- Death cinematic janky/unreadable (camera effects fighting) -> Phase 9 ✓ FIXED
- Downed state persists across worlds -> Phase 10 ✓ FIXED (disconnect/reconnect + edge cases)
- Missing grab_impact.ogg -> Phase 12
- Single-player permanent death too punishing -> Phase 11

## Session Continuity

Last session: 2026-01-27
Stopped at: Completed 11-03-PLAN.md
Resume file: None
Next: Execute 11-04-PLAN.md (Mercy Mode UI Rendering)

## Milestone History

**v1.0 MVP** — Shipped 2026-01-25
- 4 phases, 20 plans
- 2,953 lines of Java
- 2 days from start to ship

**v1.1 Polish & Immersion** — Shipped 2026-01-26
- 4 phases (5-8), 10 plans
- 873 lines added (3,757 total)
- 3 days from milestone start to ship
- All 12 requirements satisfied

See `.planning/MILESTONES.md` for full details.
