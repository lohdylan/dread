# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-23)

**Core value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.
**Current focus:** Phase 2 - AI & Horror Experience (ready to plan)

## Current Position

Phase: 2 of 4 (AI & Horror Experience)
Plan: 4 of 6 in current phase
Status: In progress
Last activity: 2026-01-24 — Completed 02-04-PLAN.md

Progress: [████░░░░░░] 45%

## Performance Metrics

**Velocity:**
- Total plans completed: 8
- Average duration: 6.2 min
- Total execution time: 0.99 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-foundation-entity | 4/4 | 25.1 min | 6.3 min |
| 02-ai-horror-experience | 4/6 | 17.3 min | 4.3 min |

**Recent Trend:**
- Last 5 plans: 02-01 (3.1 min), 02-02 (4.2 min), 02-03 (4.5 min), 02-04 (5.5 min)
- Trend: Consistent fast execution (~4-5 min per plan)

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- Fabric over Forge: Modern, lightweight, 1.21.x support (Implemented - Phase 1)
- Single entity focus: Nail the horror before expanding (Implemented - Phase 1)
- Sound registration before entities: Ensures sounds available when entities spawn (Implemented - Phase 2)
- Streaming for ambient audio: Reduces memory usage for longer background audio (Implemented - Phase 2)
- Mono OGG format required: Distance attenuation only works with mono audio (Implemented - Phase 2)
- Base spawn chance 0.5% per second: Provides 10-20% chance per minute baseline (Implemented - Phase 2)
- Day escalation capped at day 20: Prevents infinite scaling, 11x multiplier max (Implemented - Phase 2)
- Mining bonus +0.1% per block: Makes mining risky, resets after spawn (Implemented - Phase 2)
- 3:1 fake-out to real spawn ratio: Maintains psychological horror unpredictability (Implemented - Phase 2)
- AI goal priorities: VanishGoal (1) > StareStandoffGoal (2) ensures vanishing overrides freeze (Implemented - Phase 2)
- Stare timeout 600 ticks: 30 seconds creates tension without tedium (Implemented - Phase 2)
- Spawn distance 3-8 blocks: Close enough for scare, far enough to avoid screen edge visibility (Implemented - Phase 2)
- Sound priority levels: JUMPSCARE=0, PROXIMITY=1, AMBIENT=2 prevents audio overlap (Implemented - Phase 2)
- Jumpscare protection window: 3 seconds exclusive audio prevents interruption (Implemented - Phase 2)
- Fake-out distribution: 40% danger rising, 30% proximity, 30% ambient for variety (Implemented - Phase 2)
- Unnatural silence effect: Proximity volume decreases as Dread gets closer (Implemented - Phase 2)
- 300 second revive window: Long enough for friends to reach, short enough for tension (Pending - Phase 3)
- Spectator mode for perm death: Player can still watch friends, stays in session (Pending - Phase 3)

### Phase 1 Completion Summary

**What was built:**
- Complete Dread entity with Cthulhu-style cosmic horror appearance
- GeckoLib animations (idle with twitching, jerky walk, attack, spawn/despawn)
- Emissive eyes and tentacle tips that glow in darkness
- Torch extinguishing within 8 blocks (one by one with smoke particles)
- Three visual variants (BASE, EVOLVED, ELDRITCH) based on world day progression
- NBT persistence for form variants across world saves

**Key implementation decisions:**
- Java 21 portable toolchain (requires JAVA_HOME export)
- Fabric API 0.116.8 for 1.21.1
- Placeholder textures (can be replaced with art assets)
- Fisher-Yates shuffle for random torch order
- 8 block extinguishing range, 1 second per torch

**Human verification:** Approved - entity meets horror standards

### Pending Todos

None yet.

### Blockers/Concerns

**Build Environment:**
- Build requires `export JAVA_HOME="X:/Vibe Coding/jdk-21.0.6+7"` before Gradle commands

**Phase 2 Planning:**
- Turn-around jump scare mechanics require FOV calculations and precise positioning — research suggests complexity

**Phase 3 Planning:**
- Death event ordering has edge cases with Totem of Undying and other death-modifying mods
- Camera control for cinematic requires network synchronization across client-server

**General:**
- Placeholder textures need proper art assets for final release
- Minecraft 1.21.11 is last Yarn-mapped version — plan migration to Mojang Mappings for future updates

## Session Continuity

Last session: 2026-01-24 07:24 UTC
Stopped at: Completed 02-04-PLAN.md
Resume file: None
Next: Execute 02-05-PLAN.md (Dynamic audio system)

## Phase 1 Results

| Wave | Plan | Objective | Status |
|------|------|-----------|--------|
| 1 | 01-01 | Fabric mod skeleton + entity registration | ✓ Complete |
| 2 | 01-02 | GeckoLib model, textures, renderer | ✓ Complete |
| 3 | 01-03 | Entity behaviors (torch, variants) | ✓ Complete |
| 4 | 01-04 | Human visual verification | ✓ Approved |

## Phase 2 Results

| Wave | Plan | Objective | Status |
|------|------|-----------|--------|
| 1 | 02-01 | Sound event registration and placeholder audio | ✓ Complete |
| 1 | 02-02 | Spawn probability system with persistent tracking | ✓ Complete |
| 2 | 02-03 | Spawn triggering and entity placement | ✓ Complete |
| 2 | 02-04 | Fake-out behaviors with priority-based sound | ✓ Complete |
| 3 | 02-05 | Dynamic audio system | Pending |
| 4 | 02-06 | Horror experience validation | Pending |
