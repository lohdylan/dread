# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-23)

**Core value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.
**Current focus:** Phase 1 - Foundation & Entity (Planned, ready for execution)

## Current Position

Phase: 1 of 4 (Foundation & Entity)
Plan: 3 of 4 in current phase
Status: In progress
Last activity: 2026-01-24 — Completed 01-03-PLAN.md

Progress: [███░░░░░░░] 18.75%

## Performance Metrics

**Velocity:**
- Total plans completed: 3
- Average duration: 8.4 min
- Total execution time: 0.68 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-foundation-entity | 3/4 | 25.1 min | 8.4 min |

**Recent Trend:**
- Last 5 plans: 01-01 (14.5 min), 01-02 (6.6 min), 01-03 (4 min)
- Trend: Accelerating velocity

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Fabric over Forge: Modern, lightweight, 1.21.x support (Implemented - 01-01)
- Single entity focus: Nail the horror before expanding (In progress)
- 300 second revive window: Long enough for friends to reach, short enough for tension (Pending)
- Spectator mode for perm death: Player can still watch friends, stays in session (Pending)

### Phase 1 Planning Decisions

- Torch extinguishing range: 8 blocks (balanced gameplay/horror)
- Torch restoration: None (permanent removal adds stakes)
- Form variant selection: Per-entity at spawn time based on world day
- Animation style: Jerky/stutter for unsettling effect

### Phase 1 Execution Decisions

**Plan 01-01:**
- Java 21 toolchain: System has Java 25, Gradle requires Java 21 - downloaded portable JDK
- Fabric API 0.116.8: Latest available for 1.21.1 (plan specified unavailable version)
- Gradle 8.13: Required for Java 21+ support
- DuplicatesStrategy.EXCLUDE: Handle multiple fabric.mod.json files in split source sets

**Plan 01-02:**
- Placeholder texture generation: Created Node.js PNG encoder to generate functional 128x128 textures (can be replaced with art assets)
- JDK 21 portable download: Downloaded and configured portable JDK 21.0.6 for compilation (requires JAVA_HOME export)
- AutoGlowingGeoLayer: GeckoLib's automatic glowmask detection handles emissive rendering without additional code

**Plan 01-03:**
- Fisher-Yates shuffle implementation: Minecraft Random API doesn't have asJavaRandom(), implemented manual shuffle
- Torch extinguishing: 8 block range, 1 second per torch, permanent removal (adds stakes)
- Form variant progression: Days 1-3 BASE, 4-7 EVOLVED, 8+ ELDRITCH (selected at spawn, persists in NBT)

### Pending Todos

None yet.

### Blockers/Concerns

**Phase 1 Execution:**
- Build requires `export JAVA_HOME="X:/Vibe Coding/jdk-21.0.6+7"` before Gradle commands (portable JDK)
- Placeholder textures are functional but basic - need proper art assets for final release
- AutoGlowingGeoLayer intensity may need adjustment after visual testing in 01-04

**Phase 2 Planning:**
- Turn-around jump scare mechanics require FOV calculations and precise positioning — research suggests complexity
- Sound priority management needed to prevent channel exhaustion during layered audio

**Phase 3 Planning:**
- Death event ordering has edge cases with Totem of Undying and other death-modifying mods
- Camera control for cinematic requires network synchronization across client-server

**General:**
- Minecraft 1.21.11 is last Yarn-mapped version — plan migration to Mojang Mappings for future updates

## Session Continuity

Last session: 2026-01-24 06:00:55
Stopped at: Completed 01-03-PLAN.md (Entity Behaviors)
Resume file: None
Next: 01-04-PLAN.md (Animations & Models - Blockbench)

## Phase 1 Wave Structure

| Wave | Plan | Objective | Autonomous |
|------|------|-----------|------------|
| 1 | 01-01 | Fabric mod skeleton + entity registration | Yes |
| 2 | 01-02 | GeckoLib model, textures, renderer | Yes |
| 3 | 01-03 | Entity behaviors (torch, variants) | Yes |
| 4 | 01-04 | Human visual verification | No (checkpoint) |
