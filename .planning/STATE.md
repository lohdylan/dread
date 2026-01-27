# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-26)

**Core value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.
**Current focus:** v1.2 Quick Fixes — Phase 12 (Audio & Testing)

## Current Position

Phase: 12 of 12 (Audio & Testing)
Plan: 02 of 02 complete
Status: Phase 12 COMPLETE - v1.2 ready for single-player release
Last activity: 2026-01-27 — Plan 12-02 complete (comprehensive testing)

Progress: [██████████████████████████████] 100% (40/40 plans)

## Performance Metrics

**Velocity:**
- Total plans completed: 40
- Average duration: 3.1 min
- Total execution time: ~3.5 hours

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

**Phase 11 (Single-Player Forgiveness) - Plans 01-05:**
- Detect mode per-downed-instance (not cached globally) - mode may change during gameplay
- Dedicated servers always MULTIPLAYER, integrated with 1 player is SINGLEPLAYER
- Default to MULTIPLAYER for backwards compatibility when reading NBT
- 30-second single-player timeout (normal respawn), 300-second multiplayer timeout (spectator)
- Dread death tracking is transient (not persisted) - server restart clears debuff penalties
- Single-player death triggers player.kill() for normal Minecraft death flow (NO cinematic)
- Multiplayer mode retains permanent spectator transition (hardcore behavior)
- Mode transitions use proportional timer scaling to maintain fairness and prevent exploits
- Respawn debuff (Weakness II 60s, Slowness I 30s) applied via AFTER_RESPAWN event with alive flag
- SP->MP transition triggers on player join, MP->SP triggers when last other player leaves

**Phase 12 (Audio & Testing) - Plans 01-02:**
- Skip death cinematic in singleplayer (cinematic is for multiplayer spectator transition only)
- Add 5-second Resistance V immunity after Dread death respawn (prevent death loop)
- Client should never independently apply downed effects - always from server packets
- Send RemoveDownedEffectsS2C before player.kill() in singleplayer death
- Deferred multiplayer testing due to lack of LAN/dedicated server setup
- Single-player validation sufficient for v1.2 conditional release
- Multiplayer features implemented but unverified - require future testing

### Pending Todos

None.

### Blockers/Concerns

**Build Environment:**
- Build requires `export JAVA_HOME="X:/Vibe Coding/jdk-21.0.6+7"` before Gradle commands

**v1.2 Release Status:**
- ✅ All single-player features validated (30/30 tests PASS)
- ✅ All known bugs fixed and verified
- ⏸️ Multiplayer features implemented but unverified (34/34 tests SKIPPED)
- Verdict: CONDITIONAL PASS - ready for single-player release

**Future Testing Required:**
- Multiplayer LAN testing (revival system, mode transitions, state sync, audio)
- Dedicated server testing (optional, but recommended for production)

## Session Continuity

Last session: 2026-01-27
Stopped at: Phase 12 complete (all 40 plans executed)
Resume file: None
Next: v1.2 release or future multiplayer testing

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
