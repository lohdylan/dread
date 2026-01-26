# Roadmap: Dread Horror Mod

## Milestones

- v1.0 MVP - Phases 1-4 (shipped 2026-01-25) - See milestones/v1.0-ROADMAP.md
- v1.1 Polish & Immersion - Phases 5-8 (shipped 2026-01-26) - See milestones/v1.1-ROADMAP.md
- v1.2 Quick Fixes - Phases 9-12 (in progress)
- v2.0 Environmental Horror (planned)

## v1.2 Quick Fixes

**Milestone Goal:** Fix bugs, improve single-player experience, validate multiplayer stability.

### Phase 9: Cinematic Fix
**Goal**: Death cinematic is smooth, readable, and genuinely horrifying
**Depends on**: Phase 8 (previous milestone)
**Requirements**: FIX-01
**Success Criteria** (what must be TRUE):
  1. Camera effects during death cinematic are coordinated (no fighting/flickering)
  2. Player can clearly see Dread's grab animation during death sequence
  3. Cinematic timing allows player to process what's happening before death
**Plans**: TBD

Plans:
- [ ] 09-01: TBD

### Phase 10: State Cleanup
**Goal**: Downed state lifecycle is properly managed across world boundaries
**Depends on**: Phase 9
**Requirements**: FIX-02
**Success Criteria** (what must be TRUE):
  1. Leaving a world clears downed state completely
  2. Creating a new world starts with fresh state (not downed)
  3. Re-entering a world where player was downed restores correct state
**Plans**: TBD

Plans:
- [ ] 10-01: TBD

### Phase 11: Single-Player Forgiveness
**Goal**: Single-player death is punishing but not permanent, while multiplayer retains hardcore behavior
**Depends on**: Phase 10
**Requirements**: GAME-01, GAME-02, GAME-03, GAME-04
**Success Criteria** (what must be TRUE):
  1. Single-player downed state lasts 30-60 seconds before automatic death
  2. Single-player death triggers normal Minecraft respawn (at bed or world spawn)
  3. Full death cinematic plays before single-player death (not skipped)
  4. Multiplayer downed state still lasts 300 seconds
  5. Multiplayer death without revive still results in permanent spectator mode
**Plans**: TBD

Plans:
- [ ] 11-01: TBD

### Phase 12: Audio & Testing
**Goal**: Audio polish complete, multiplayer stability validated
**Depends on**: Phase 11
**Requirements**: FIX-03, TEST-01, TEST-02
**Success Criteria** (what must be TRUE):
  1. grab_impact.ogg plays during death grab animation
  2. Multiplayer dedicated server tested with 2+ players
  3. No desync issues during downed/revive/death flow in multiplayer
  4. All v1.2 requirements verified working in both single-player and multiplayer
**Plans**: TBD

Plans:
- [ ] 12-01: TBD

## Progress

**Execution Order:**
Phases 1-8 complete. v1.2 starts at Phase 9.

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
| 9. Cinematic Fix | v1.2 | 0/? | Not started | - |
| 10. State Cleanup | v1.2 | 0/? | Not started | - |
| 11. Single-Player Forgiveness | v1.2 | 0/? | Not started | - |
| 12. Audio & Testing | v1.2 | 0/? | Not started | - |

**Total:** 12 phases, 30 plans complete, 2 milestones shipped, 1 in progress

---
*Last updated: 2026-01-26 after v1.2 roadmap creation*
