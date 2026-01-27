# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-27)

**Core value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.
**Current focus:** Phase 14 - Animated Entity Textures (v2.0)

## Current Position

Phase: 14 of 16 (Animated Entity Textures)
Plan: 3 of 3 (estimated)
Status: In progress
Last activity: 2026-01-27 — Completed 14-03-PLAN.md (Tentacle writhing animation)

Progress: [██████████████░░░░░░] 82% (46/56 estimated total plans)

## Performance Metrics

**Velocity:**
- Total plans completed: 46
- Average duration: 2.9 min
- Total execution time: ~3.8 hours

**By Milestone:**

| Milestone | Phases | Plans | Status |
|-----------|--------|-------|--------|
| v1.0 MVP | 4 | 20 | Shipped |
| v1.1 Polish | 4 | 10 | Shipped |
| v1.2 Fixes | 4 | 10 | Shipped |
| v2.0 Atmosphere | 4 | 6 | In progress |

**Recent Trend:**
- Consistent velocity across v1.x milestones
- v2.0 complexity higher (camera system, texture animation)
- Expect longer planning/execution cycles for Phases 13-14

## Accumulated Context

### Decisions

Recent decisions from PROJECT.md affecting v2.0:

- Render-time camera shake via mixin (eliminates feedback loops) — v1.2
- Mixin order 900 for cinematic shake, applies before crawl pitch clamping (1000) — v1.2
- FPS-adaptive shake with visual compensation pattern — v1.1

**v2.0 decisions made:**
- Camera position control via Camera.update() mixin injection — Phase 13-01 (CAM-04)
- Hardcoded tick boundaries over keyframe system for 4.5s sequence — Phase 13-01 (CAM-07)
- Mixin coordination: separate injections for position (update) and rotation (setRotation) — Phase 13-01
- Letterbox bars: 60px height, solid black (0xFF000000), instant appearance (no fade) — Phase 13-02
- HudRenderCallback pattern for cinematic overlays — Phase 13-02
- Locked camera rotation during face close-up (frozen terror aesthetic) — Phase 13-03
- Smooth yaw tracking during pull-back phase — Phase 13-03
- Heartbeat pulse zones: 3 zones with accelerating periods (20-tick → 12-tick → 6-tick) — Phase 14-02
- Pulse frame calculation: Zone-based modulo for smooth acceleration effect — Phase 14-02
- Texture path generation: Dynamic construction supports all 3 form variants — Phase 14-02
- Parallel controller pattern for tentacle animation (runs alongside main/head) — Phase 14-03 (ANIM-01)
- Subtle rotation values (2-6 degrees) for barely perceptible motion — Phase 14-03 (ANIM-02)
- 4-second loop with offset timing for organic tentacle movement — Phase 14-03 (ANIM-03)

**v2.0 decisions pending:**
- Texture animation method (.mcmeta vs custom FeatureRenderer)
- Light extinguishing scope (campfire-only vs torch support)

### Pending Todos

None.

### Blockers/Concerns

**Phase 13 (Camera):**
- COMPLETE: Cinematic camera system fully integrated and verified
- Pull-back phase: Camera behind player, smooth yaw tracking to Dread
- Face close-up phase: Camera locked on Dread's eyes, frozen terror aesthetic
- Letterbox bars: 60px solid black, instant appearance
- Timing verified: 4.5s total (1.5s pull-back + 3.0s close-up)
- Known limitation: No collision detection (camera can clip through walls)

**Phase 14 (Textures):**
- Plan 14-01 COMPLETE: Timer API and placeholder textures in place
- Plan 14-02 COMPLETE: Cinematic-synchronized texture selection with heartbeat pulse
- Plan 14-03 COMPLETE: Tentacle writhing animation added
- getCinematicTimer() returns tick (0-90) for animation sync
- isInFaceCloseup() detects face close-up phase
- 8 placeholder texture files created (pulse 0/1/2, eyes_open + glowmasks)
- DreadEntityModel.getTextureResource() dynamically selects textures based on cinematic state
- Accelerating heartbeat pulse (3 zones: 20-tick → 12-tick → 6-tick periods)
- Eyes-open texture shown during face close-up phase
- tentacle_writhe animation with 4s loop, 2-6° subtle rotations
- Parallel animation controller for continuous background motion
- MEDIUM risk: Animated texture performance collapse with AMD GPUs
- UV offset application method needs validation during planning
- GeckoLib 5 render thread separation requires extractRenderState() pattern

**Phase 15 (Environmental):**
- LOW risk: Standard patterns (AI goals, block states)
- Start with campfire-only light extinguishing (native LIT property)

**Phase 16 (Blood Trail):**
- LOW risk: Straightforward particle system
- Must use WorldServer.spawnParticle for multiplayer sync

**Build Environment:**
- Build requires `export JAVA_HOME="X:/Vibe Coding/jdk-21.0.6+7"` before Gradle commands

**Deferred Testing:**
- Multiplayer features implemented but unverified (TEST-01, TEST-02 deferred)

## Session Continuity

Last session: 2026-01-27
Stopped at: Completed 14-03-PLAN.md (Tentacle writhing animation)
Resume file: None
Next: Phase 14 in progress - ready for 14-04 (Texture animation logic)

## Milestone History

**v1.0 MVP** — Shipped 2026-01-25
- 4 phases, 20 plans
- 2,953 lines of Java
- 2 days from start to ship

**v1.1 Polish & Immersion** — Shipped 2026-01-26
- 4 phases (5-8), 10 plans
- 873 lines added (3,757 total)
- 3 days from milestone start to ship

**v1.2 Quick Fixes** — Shipped 2026-01-27
- 4 phases (9-12), 10 plans
- 1,085 lines added (4,842 total)
- 5 hours from milestone start to ship

See `.planning/MILESTONES.md` for full details.

---
*Last updated: 2026-01-27 after completing 14-03-PLAN.md*
