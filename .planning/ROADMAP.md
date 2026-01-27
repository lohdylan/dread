# Roadmap: Dread Horror Mod

## Milestones

- ✅ **v1.0 MVP** — Phases 1-4 (shipped 2026-01-25) - See milestones/v1.0-ROADMAP.md
- ✅ **v1.1 Polish & Immersion** — Phases 5-8 (shipped 2026-01-26) - See milestones/v1.1-ROADMAP.md
- ✅ **v1.2 Quick Fixes** — Phases 9-12 (shipped 2026-01-27) - See milestones/v1.2-ROADMAP.md
- 🚧 **v2.0 Atmosphere & Dread** — Phases 13-16 (in progress)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1-4) — SHIPPED 2026-01-25</summary>

- [x] Phase 1: Foundation - Entity (4/4 plans) — completed 2026-01-25
- [x] Phase 2: AI & Horror Experience (6/6 plans) — completed 2026-01-25
- [x] Phase 3: Death & Revival System (6/6 plans) — completed 2026-01-25
- [x] Phase 4: Configuration & Release Prep (4/4 plans) — completed 2026-01-25

</details>

<details>
<summary>✅ v1.1 Polish & Immersion (Phases 5-8) — SHIPPED 2026-01-26</summary>

- [x] Phase 5: Resources (3/3 plans) — completed 2026-01-26
- [x] Phase 6: Attack Prevention (1/1 plan) — completed 2026-01-26
- [x] Phase 7: Crawl Pose (3/3 plans) — completed 2026-01-26
- [x] Phase 8: Cinematic Enhancement (3/3 plans) — completed 2026-01-26

</details>

<details>
<summary>✅ v1.2 Quick Fixes (Phases 9-12) — SHIPPED 2026-01-27</summary>

- [x] Phase 9: Cinematic Fix (1/1 plan) — completed 2026-01-26
- [x] Phase 10: State Cleanup (2/2 plans) — completed 2026-01-26
- [x] Phase 11: Single-Player Forgiveness (5/5 plans) — completed 2026-01-26
- [x] Phase 12: Audio & Testing (2/2 plans) — completed 2026-01-27

</details>

### 🚧 v2.0 Atmosphere & Dread (In Progress)

**Milestone Goal:** Transform death sequence from static event to cinematic horror experience with dynamic camera movement, animated entity textures, environmental reactions, and blood trail effects.

#### Phase 13: Cinematic Camera Control

**Goal:** Camera control transfers to cinematic system during death sequence, executing multi-stage camera path (pull back → zoom to face) with smooth interpolation and mixin coordination.

**Depends on:** Phase 12 (existing camera shake system)

**Requirements:** CAM-01, CAM-02, CAM-03, CAM-04, CAM-05, CAM-06, CAM-07

**Success Criteria** (what must be TRUE):
1. Player loses camera control when death sequence starts (cinematic system takes over)
2. Camera smoothly pulls back from player during death grab (shows vulnerability without jarring movement)
3. Camera zooms to Dread's face at climax (forced confrontation, fills screen)
4. Camera movement coordinates with existing shake system (additive effects, no conflicts)
5. Death sequence remains 4.5 seconds with no FPS drops at 144Hz

**Plans:** 3 plans

Plans:
- [x] 13-01-PLAN.md — Core camera position infrastructure (CameraMixin + DeathCinematicClientHandler)
- [x] 13-02-PLAN.md — Letterbox bars visual feedback (CinematicLetterboxRenderer)
- [x] 13-03-PLAN.md — Integration, tuning, and verification checkpoint

#### Phase 14: Animated Entity Textures

**Goal:** Dread texture changes during kill sequence with pulsing runes, writhing forms, and eye reveals synchronized to cinematic timing.

**Depends on:** Phase 13 (camera must be positioned to see texture effects)

**Requirements:** TEX-01, TEX-02, TEX-03, TEX-04, TEX-05

**Success Criteria** (what must be TRUE):
1. Dread's texture changes during death sequence (not static throughout)
2. Emissive runes pulse visibly during kill (otherworldly glow synchronized to cinematic progress)
3. Dread's form shows writhing effects (tentacles shift, geometry distorts)
4. Eyes open as camera zooms to face (reveal timed to climax)
5. Texture animations run at 60+ FPS with AMD GPUs (no performance collapse)

**Plans:** 4 plans

Plans:
- [ ] 14-01-PLAN.md — Client handler API + placeholder texture files (foundation)
- [ ] 14-02-PLAN.md — Cinematic-synchronized texture selection with pulse timing
- [ ] 14-03-PLAN.md — Tentacle writhing animation (GeckoLib bone animation)
- [ ] 14-04-PLAN.md — In-game verification checkpoint

#### Phase 15: Environmental Effects

**Goal:** Doors slam shut and lights flicker when Dread approaches, with intensity scaling by proximity and coordinated sequencing.

**Depends on:** Nothing (independent system, can parallelize with Phase 14)

**Requirements:** ENV-01, ENV-02, ENV-03, ENV-04

**Success Criteria** (what must be TRUE):
1. Doors within 8-12 blocks slam shut when Dread spawns nearby (physical presence)
2. Campfire lights flicker or extinguish when Dread approaches (darkness follows entity)
3. Effects get stronger as Dread gets closer (proximity scaling visible to player)
4. Effects follow coordinated sequence (flicker first, then slam, then spawn pattern)
5. Effects sync in multiplayer (all players see same door slams at same time)

**Plans:** TBD

Plans:
- [ ] 15-01: TBD
- [ ] 15-02: TBD

#### Phase 16: Blood Trail Particles

**Goal:** Visible blood trail when crawling while downed, with trail fade-out and intensity based on downed duration.

**Depends on:** Nothing (integrates with existing downed state)

**Requirements:** BLD-01, BLD-02, BLD-03, BLD-04

**Success Criteria** (what must be TRUE):
1. Blood trail particles spawn when crawling while downed (visible path behind player)
2. Blood trail fades over 2-3 seconds (doesn't persist forever)
3. Trail starts heavy and lightens as downed timer increases (visual feedback of time passing)
4. Blood trail appears for all players in multiplayer (not just crawling player)
5. Configurable particle density has no FPS impact when set to 50% or lower

**Plans:** TBD

Plans:
- [ ] 16-01: TBD

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Foundation - Entity | v1.0 | 4/4 | Complete | 2026-01-25 |
| 2. AI & Horror Experience | v1.0 | 6/6 | Complete | 2026-01-25 |
| 3. Death & Revival System | v1.0 | 6/6 | Complete | 2026-01-25 |
| 4. Configuration & Release Prep | v1.0 | 4/4 | Complete | 2026-01-25 |
| 5. Resources | v1.1 | 3/3 | Complete | 2026-01-26 |
| 6. Attack Prevention | v1.1 | 1/1 | Complete | 2026-01-26 |
| 7. Crawl Pose | v1.1 | 3/3 | Complete | 2026-01-26 |
| 8. Cinematic Enhancement | v1.1 | 3/3 | Complete | 2026-01-26 |
| 9. Cinematic Fix | v1.2 | 1/1 | Complete | 2026-01-26 |
| 10. State Cleanup | v1.2 | 2/2 | Complete | 2026-01-26 |
| 11. Single-Player Forgiveness | v1.2 | 5/5 | Complete | 2026-01-26 |
| 12. Audio & Testing | v1.2 | 2/2 | Complete | 2026-01-27 |
| 13. Cinematic Camera Control | v2.0 | 3/3 | Complete | 2026-01-27 |
| 14. Animated Entity Textures | v2.0 | 0/4 | Planned | - |
| 15. Environmental Effects | v2.0 | 0/? | Not started | - |
| 16. Blood Trail Particles | v2.0 | 0/? | Not started | - |

**Total:** 16 phases, 43 plans complete, 3 phases planned (v2.0), 3 milestones shipped

---
*Last updated: 2026-01-27 after Phase 13 execution*
