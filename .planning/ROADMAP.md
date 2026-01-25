# Roadmap: Dread

## Overview

Dread is a Minecraft horror mod delivering genuine terror through a cosmic horror entity with turn-around jump scares, cinematic death sequences, and cooperative revival mechanics. This roadmap structures delivery across four phases: foundational entity creation, AI-driven stalking and sound design, death/revival systems, and configuration polish for release.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundation & Entity** - Fabric mod skeleton with Dread entity model and animations
- [x] **Phase 2: AI & Horror Experience** - Turn-around jump scares, stalking AI, and complete soundscape
- [x] **Phase 3: Death & Revival System** - Downed state, cinematic death, and multiplayer revival
- [x] **Phase 4: Configuration & Release Prep** - User customization and final polish

## Phase Details

### Phase 1: Foundation & Entity
**Goal**: Dread entity exists as a visible, animated horror creature with proper Fabric mod infrastructure
**Depends on**: Nothing (first phase)
**Requirements**: ENTITY-01, ENTITY-02, ENTITY-03, ENTITY-04
**Success Criteria** (what must be TRUE):
  1. Dread entity spawns in-game with Cthulhu-style cosmic horror appearance
  2. Entity animations play smoothly (idle, walking, attacking) via GeckoLib
  3. Nearby torches extinguish when Dread spawns creating darkness
  4. Multiple Dread forms/appearances are implemented and randomly selected
**Plans**: 4 plans in 4 waves

Plans:
- [x] 01-01-PLAN.md — Fabric mod skeleton with entity registration
- [x] 01-02-PLAN.md — GeckoLib model, textures, and renderer
- [x] 01-03-PLAN.md — Entity behaviors (torch extinguishing, form variants)
- [x] 01-04-PLAN.md — Human visual verification checkpoint

### Phase 2: AI & Horror Experience
**Goal**: Dread intelligently stalks players with turn-around jump scares and full atmospheric soundscape
**Depends on**: Phase 1
**Requirements**: AI-01, AI-02, AI-03, AI-04, SOUND-01, SOUND-02, SOUND-04
**Success Criteria** (what must be TRUE):
  1. Player experiences turn-around jump scare (entity appears when looking away then back)
  2. Mining activity noticeably increases Dread spawn frequency
  3. Spawn probability escalates as game days progress creating long-term tension
  4. Fake-out tension moments occur without attacks keeping player on edge
  5. Ambient tension soundscape plays building dread before possible spawns
  6. Jump scare audio triggers precisely when Dread appears
  7. Sound intensity increases when Dread is nearby even when not visible
**Plans**: 6 plans in 4 waves

Plans:
- [x] 02-01-PLAN.md — Sound event registration and placeholder audio files
- [x] 02-02-PLAN.md — Spawn probability system with persistent state
- [x] 02-03-PLAN.md — AI goals (turn-around spawn, stare-to-freeze, vanish)
- [x] 02-04-PLAN.md — Fake-out behaviors with varied tension sounds
- [x] 02-05-PLAN.md — Proximity audio and ambient tension integration
- [x] 02-06-PLAN.md — Human verification checkpoint

### Phase 3: Death & Revival System
**Goal**: Players experience unskippable cinematic death followed by downed state with cooperative revival mechanics
**Depends on**: Phase 2
**Requirements**: DEATH-01, DEATH-02, DEATH-03, DEATH-04, SOUND-03
**Success Criteria** (what must be TRUE):
  1. Player death triggers unskippable cinematic sequence showing their demise
  2. Killed player enters downed state with 300 second timer and blurred/dark screen
  3. Downed player becomes permanent spectator if timer expires without revival
  4. Nearby player can crouch to revive downed teammate restoring them to play
  5. Solo players experience permanent death stakes (no revival possible)
**Plans**: 6 plans in 4 waves

Plans:
- [x] 03-01-PLAN.md — Satin API dependency, death sound, network packets
- [x] 03-02-PLAN.md — DownedPlayersState server-side persistence
- [x] 03-03-PLAN.md — Death event interception and cinematic controller
- [x] 03-04-PLAN.md — Downed state shaders and HUD timer
- [x] 03-05-PLAN.md — Revival mechanics and spectator transition
- [x] 03-06-PLAN.md — Human verification checkpoint

### Phase 4: Configuration & Release Prep
**Goal**: Players can customize mod behavior and mod is release-ready with full documentation
**Depends on**: Phase 3
**Requirements**: CONFIG-01, CONFIG-02, CONFIG-03, CONFIG-04
**Success Criteria** (what must be TRUE):
  1. Spawn rate is configurable via config file
  2. Damage settings are adjustable for different difficulty preferences
  3. Mod can be fully disabled via toggle without removing from mod folder
  4. Death cinematic can be skipped via configuration option
  5. Mod works reliably in multiplayer with 2-4 players on dedicated server
  6. Mod is compatible with Iris and Optifine shaders
**Plans**: 4 plans in 3 waves

Plans:
- [x] 04-01-PLAN.md — GSON config system with validation (DreadConfig, DreadConfigLoader)
- [x] 04-02-PLAN.md — Config integration into spawn, damage, and cinematic systems
- [x] 04-03-PLAN.md — Shader compatibility detection (Iris/OptiFine) with fallback
- [x] 04-04-PLAN.md — Human verification checkpoint

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Entity | 4/4 | Complete | 2026-01-24 |
| 2. AI & Horror Experience | 6/6 | Complete | 2026-01-24 |
| 3. Death & Revival System | 6/6 | Complete | 2026-01-24 |
| 4. Configuration & Release Prep | 4/4 | Complete | 2026-01-25 |

---
*Roadmap created: 2026-01-23*
*Phase 1 planned: 2026-01-24*
*Phase 1 complete: 2026-01-24*
*Phase 2 planned: 2026-01-24*
*Phase 2 complete: 2026-01-24*
*Phase 3 planned: 2026-01-24*
*Phase 3 complete: 2026-01-24*
*Phase 4 planned: 2026-01-24*
*Phase 4 complete: 2026-01-25*
*Depth: quick (4 phases)*
*Coverage: 20/20 v1 requirements mapped*
*Milestone complete: 2026-01-25*
