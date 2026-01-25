# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-23)

**Core value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.
**Current focus:** Phase 2 - AI & Horror Experience (ready to plan)

## Current Position

Phase: 4 of 4 (Configuration & Release Prep)
Plan: 2 of 3 in current phase
Status: In progress
Last activity: 2026-01-25 — Completed 04-02-PLAN.md (Config integration)

Progress: [█████████████████░░░] 94% (17/18 plans complete)

## Performance Metrics

**Velocity:**
- Total plans completed: 17
- Average duration: 4.1 min
- Total execution time: 1.63 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-foundation-entity | 4/4 | 25.1 min | 6.3 min |
| 02-ai-horror-experience | 6/6 | 19.6 min | 3.3 min |
| 03-death-revival-system | 6/6 | 22.5 min | 3.8 min |
| 04-configuration-release-prep | 2/3 | 10.0 min | 3.3 min |

**Recent Trend:**
- Last 5 plans: 03-06 (0 min - checkpoint), 04-01 (2.1 min), 04-02 (4.1 min), 04-03 (3.8 min)
- Trend: Phase 4 progressing, config integration complete

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
- Proximity sound cooldown 2 seconds: Prevents audio spam while maintaining entity presence (Implemented - Phase 2)
- Ambient interval 20s ± 10s: Creates unpredictable soundscape timing, prevents player conditioning (Implemented - Phase 2)
- Tension calculation formula: (blocksMined * 0.01) + (worldDay * 0.02) for balanced escalation (Implemented - Phase 2)
- 300 second revive window: Long enough for friends to reach, short enough for tension (Implemented - Phase 3)
- Spectator mode for perm death: Player can still watch friends, stays in session (Implemented - Phase 3)
- Crouch-to-revive 4 block range: Close enough for deliberate positioning, far enough for safe approach (Implemented - Phase 3)
- Uninterruptible 3-second revival: Prevents spam-clicking abuse, creates commitment stakes (Implemented - Phase 3)
- Movement penalty -90% crawl: Allows limited repositioning without trivializing downed state (Implemented - Phase 3)
- Timer pauses during revival: Prevents unfair timer expiration mid-revival (Implemented - Phase 3)
- Satin API from Ladysnake maven: org.ladysnake:satin:1.17.0 for post-processing shaders (Implemented - Phase 3)
- Custom UUID PacketCodec: Manual serialization for network packets using ByteBuf long read/write (Implemented - Phase 3)
- Placeholder audio pattern: 54-byte OGG files consistent across all sound events (Implemented - Phase 3)
- ALLOW_DEATH event pattern: Return false to cancel vanilla death, set health to 1.0f to prevent next-tick death (Implemented - Phase 3)
- 4.5 second cinematic duration: 90 ticks for jump scare impact without disrupting gameplay flow (Implemented - Phase 3)
- Face-to-face teleport calculation: Player rotation vector * 1.5 blocks for cinematic Dread positioning (Implemented - Phase 3)
- Gaussian blur radius 8.0: Heavy blur creates claustrophobic near-blind downed vision (Implemented - Phase 3)
- Vignette intensity 0.7: Darkens screen edges for tunnel vision effect during downed state (Implemented - Phase 3)
- Timer color interpolation yellow-to-red: Visual urgency increases as countdown approaches zero (Implemented - Phase 3)
- GSON with pretty printing: Human-readable config files for easier manual editing (Implemented - Phase 4)
- Validation clamps values to safe ranges: Invalid config values auto-corrected without crashing mod (Implemented - Phase 4)
- Auto-save after validation: Persists clamped values back to file so user sees corrections (Implemented - Phase 4)
- Config loads first in onInitialize(): Ensures all features have access to config values (Implemented - Phase 4)
- Iris/OptiFine detection via FabricLoader: isModLoaded("iris") and isModLoaded("optifabric") for runtime shader compatibility (Implemented - Phase 4)
- Config override precedence: disableDownedEffects=true forces post-processing off regardless of shader mod detection (Implemented - Phase 4)
- HUD vs shader separation: Countdown timer remains visible when shaders disabled for compatibility (Implemented - Phase 4)
- modEnabled check at entry points: Prevents spawn evaluation, death interception, and attack damage when mod disabled (Implemented - Phase 4)
- skipDeathCinematic preserves death sound: Audio feedback still plays even when camera lock skipped (Implemented - Phase 4)
- tryAttack() override for damage: Replaces entity attribute system with direct config-driven damage value (Implemented - Phase 4)

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

### Phase 2 Completion Summary

**What was built:**
- Turn-around spawn mechanic (entity appears 3-8 blocks behind player)
- Weeping Angel / SCP-173 stare mechanic (entity freezes when watched, vanishes after 30s)
- Mining-based spawn probability (increments danger with each block broken)
- Day-based escalation (1x to 11x multiplier, capped at day 20)
- Fake-out system (3:1 ratio of false alarms to real spawns)
- Priority-based sound system (jumpscare > proximity > ambient)
- Dynamic audio atmosphere (ambient tension, proximity triggers, unnatural silence)
- Complete AI goal system (VanishGoal, StareStandoffGoal, MeleeAttackGoal)

**Key implementation decisions:**
- Base spawn chance 0.5% per second for natural pacing
- Mining bonus +0.1% per block mined (resets after spawn)
- Proximity sound every 2 seconds, ambient intervals 10-30s randomized
- Unnatural silence effect (volume decreases when Dread within 8 blocks)
- FOV threshold 0.85 (~31° cone) for stare detection with raycast obstruction
- Fake-out distribution: 40% danger rising, 30% proximity, 30% ambient
- Tension calculation: (blocksMined * 0.01) + (worldDay * 0.02)

**Human verification:** Approved - all 6 tests passed, horror experience genuinely terrifying

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

Last session: 2026-01-25 (current session)
Stopped at: Completed 04-02-PLAN.md (Config integration complete)
Resume file: None
Next: Continue Phase 4 - 04-03 shader compatibility remaining

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
| 3 | 02-05 | Dynamic audio system | ✓ Complete |
| 4 | 02-06 | Horror experience validation | ✓ Approved |

**Phase 2 Status:** COMPLETE - All horror mechanics verified and approved

## Phase 3 Results

| Wave | Plan | Objective | Status |
|------|------|-----------|--------|
| 1 | 03-01 | Satin API, death sound, network packets | ✓ Complete |
| 1 | 03-02 | Persistent state management for downed players | ✓ Complete |
| 2 | 03-03 | Death event handler and transition | ✓ Complete |
| 2 | 03-04 | Downed state shaders and HUD | ✓ Complete |
| 3 | 03-05 | Revival mechanics and spectator transition | ✓ Complete |
| 4 | 03-06 | Death system validation | ✓ Approved |

**Phase 3 Status:** COMPLETE - All death/revival mechanics verified and approved

## Phase 4 Results

| Wave | Plan | Objective | Status |
|------|------|-----------|--------|
| 1 | 04-01 | GSON-based config with validation and persistence | ✓ Complete |
| 2 | 04-02 | Config integration into spawn, damage, and cinematic systems | ✓ Complete |
| 2 | 04-03 | Shader mod compatibility detection and fallback | ✓ Complete |

**Phase 4 Status:** IN PROGRESS - 2 of 3 plans complete

### Phase 3 Completion Summary

**What was built:**
- Death event interception via Fabric ALLOW_DEATH event
- 4.5-second cinematic camera lock onto Dread face-to-face
- Gaussian blur + vignette shader for near-blind downed vision
- HUD countdown timer with yellow-to-red color interpolation
- 300-second downed timer with spectator transition on expiration
- Crouch-to-revive within 4 blocks (3-second uninterruptible)
- World-space billboard progress bar above downed player
- 90% movement speed reduction during downed state
- Network packet infrastructure for multiplayer sync

**Key implementation decisions:**
- Satin API for post-processing shaders (org.ladysnake:satin:1.17.0)
- Custom UUID PacketCodec for network serialization
- Timer pauses during active revival attempt
- Solo players face permanent death stakes (no self-revival)

**Human verification:** Approved - all 5 tests passed, horror experience meets standards
