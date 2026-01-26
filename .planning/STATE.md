# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-25)

**Core value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.
**Current focus:** Phase 5 - Resources (placeholder asset replacement)

## Current Position

Phase: 8 of 8 (Cinematic Enhancement)
Plan: 3 of 3
Status: Phase complete
Last activity: 2026-01-26 — Completed 08-03-PLAN.md

Progress: [██████████░░░░░░░░░░] 50% (v1.0 complete, 3/4 v1.1 phases done, Phase 8 complete)

## Performance Metrics

**Velocity:**
- Total plans completed: 27
- Average duration: 3.7 min
- Total execution time: 2.02 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-foundation-entity | 4/4 | 25.1 min | 6.3 min |
| 02-ai-horror-experience | 6/6 | 19.6 min | 3.3 min |
| 03-death-revival-system | 6/6 | 22.5 min | 3.8 min |
| 04-configuration-release-prep | 4/4 | 10.0 min | 2.5 min |
| 06-attack-prevention | 1/1 | 3.0 min | 3.0 min |
| 07-crawl-pose | 3/3 | 8.6 min | 2.9 min |
| 08-cinematic-enhancement | 3/3 | 10.8 min | 3.6 min |

*Updated after each plan completion*

## Accumulated Context

### Decisions

All v1.0 decisions documented in PROJECT.md Key Decisions table with outcomes marked Good.

v1.1 roadmap decisions:
- Phase 5 first: Zero-code asset replacement enables early visual/audio wins before code complexity
- Phase structure by complexity: Resources (zero code) → Attack prevention (simple) → Crawl pose (moderate) → Cinematic (complex)

Phase 6 decisions:
- Dual blocking (client + server): Server blocks authoritatively, client prevents animations for smooth UX
- Weapon-only projectile blocking: Only RangedWeaponItem and TridentItem blocked, other items allowed

Phase 7 decisions:
- EntityPose.SWIMMING for prone: Vanilla provides crawling animation, no custom animation needed
- Pose override pattern: Server setPose() + client mixin cancels updatePose() to prevent flickering
- Exit pose before spectator: Pose changes don't work in spectator mode
- Entity.setSprinting target: setSprinting defined on Entity, not PlayerEntity - use instanceof check
- Input ghosting prevention: Only block sprinting=true, allow turning sprint off

Phase 8 decisions:
- Front-loaded violence timing: 0.15s explosive lunge, 0.85s hold, 0.8s release for maximum terror
- Sound keyframe at 0.0s: Immediate impact on animation start
- State flag pattern: isPlayingDeathGrab checked first in main controller for cinematic override
- Camera shake: SHAKE_MAGNITUDE 2.5f, DECAY_SPEED 8.0f for 0.7-1s decay feel
- Intensity config: cameraShakeIntensity 0-100 (default 100, full horror), 0 disables shake only
- FPS threshold 45: Below this, shake causes judder - auto-reduce with 0.3f floor
- Compensation pattern: When reducing shake, boost vignette/flash for equivalent horror impact
- Cinematic duration 1.8s (36 ticks): Matches death_grab animation exactly

### Pending Todos

None.

### Blockers/Concerns

**Build Environment:**
- Build requires `export JAVA_HOME="X:/Vibe Coding/jdk-21.0.6+7"` before Gradle commands

**Phase 5 Notes (from research):**
- Texture UV mapping: Design for all animation states upfront (standing, crawling, downed)
- Audio format: OGG Vorbis mono 44.1kHz only (no stereo, no MP3)

**Phase 7 Notes (from research):**
- Client-server sync critical: Use Fabric networking API for pose state changes
- GeckoLib animation conflicts: Test with v1.0 entities present during pose override

**Phase 8 Notes (from research):**
- Motion sickness prevention: Exponential decay, config options, test at 30/60/144 FPS
- Sound channel saturation: Coordinate with Phase 5 audio, respect v1.0 priority system

## Session Continuity

Last session: 2026-01-26
Stopped at: Completed 08-03-PLAN.md (Phase 8 complete)
Resume file: None
Next: /gsd:discuss-phase 5 (Resources - placeholder asset replacement)

## Milestone History

**v1.0 MVP** — Shipped 2026-01-25
- 4 phases, 20 plans
- 2,953 lines of Java
- 2 days from start to ship

**v1.1 Polish & Immersion** — In progress
- 4 phases (5-8)
- Focus: Replace placeholders, enhance downed state immersion
- Phase 6 (Attack Prevention) complete
- Phase 7 (Crawl Pose) complete
- Phase 8 (Cinematic Enhancement) complete

See `.planning/MILESTONES.md` for full details.
