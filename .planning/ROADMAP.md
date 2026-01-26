# Roadmap: Dread Horror Mod

## Milestones

- ✅ **v1.0 MVP** - Phases 1-4 (shipped 2026-01-25)
- 🚧 **v1.1 Polish & Immersion** - Phases 5-8 (in progress)

## Phases

<details>
<summary>✅ v1.0 MVP (Phases 1-4) - SHIPPED 2026-01-25</summary>

### Phase 1: Foundation - Entity
**Goal**: Dread entity exists with cosmic horror appearance
**Plans**: 4 plans
**Status**: Complete

Plans:
- [x] 01-01: Entity creation
- [x] 01-02: GeckoLib integration
- [x] 01-03: Texture setup
- [x] 01-04: Basic spawning

### Phase 2: AI & Horror Experience
**Goal**: Turn-around spawn mechanic with tension and jump scares
**Plans**: 6 plans
**Status**: Complete

Plans:
- [x] 02-01: Turn-around spawn
- [x] 02-02: Mining activity tracking
- [x] 02-03: Spawn probability escalation
- [x] 02-04: Fake-out behaviors
- [x] 02-05: Light extinguishing
- [x] 02-06: Audio system

### Phase 3: Death & Revival System
**Goal**: Cinematic death with revival mechanics
**Plans**: 6 plans
**Status**: Complete

Plans:
- [x] 03-01: Cinematic death sequence
- [x] 03-02: Downed state
- [x] 03-03: Permanent death handling
- [x] 03-04: Crouch-to-revive mechanic
- [x] 03-05: Vision effects
- [x] 03-06: Multiplayer sync

### Phase 4: Configuration & Release Prep
**Goal**: Configurable mod ready for distribution
**Plans**: 4 plans
**Status**: Complete

Plans:
- [x] 04-01: GSON configuration system
- [x] 04-02: Config validation
- [x] 04-03: Shader compatibility detection
- [x] 04-04: Release prep

</details>

## 🚧 v1.1 Polish & Immersion (In Progress)

**Milestone Goal:** Replace placeholder assets and enhance downed state immersion to deliver genuinely terrifying horror experience.

### Phase 5: Resources
**Goal**: Dread entity has terrifying appearance and horror audio that replaces placeholders
**Depends on**: v1.0 foundation
**Requirements**: TEX-01, TEX-02, TEX-03, AUDIO-01, AUDIO-02, AUDIO-03
**Success Criteria** (what must be TRUE):
  1. Dread entity has scary cosmic horror texture replacing gray placeholder
  2. Dread texture includes emissive glowmask visible in darkness
  3. All 3 form variants (BASE, EVOLVED, ELDRITCH) have visually distinct textures
  4. Core horror sounds are audible (jump scare, death, ambient presence)
  5. Audio uses 3-layer soundscape (ambient drone, proximity intensification, jump scare)
**Plans**: 3 plans
**Status**: Complete

Plans:
- [x] 05-01-PLAN.md - Create horror textures for all 3 Dread form variants with glowmasks
- [x] 05-02-PLAN.md - Create horror audio with variations for 3-layer soundscape
- [x] 05-03-PLAN.md - Update sounds.json with new audio file references and weights

### Phase 6: Attack Prevention
**Goal**: Players cannot attack while in downed state
**Depends on**: Phase 5
**Requirements**: DOWN-02
**Success Criteria** (what must be TRUE):
  1. Player cannot perform melee attacks while downed
  2. Player cannot fire projectiles (bow, crossbow, trident) while downed
  3. Attack inputs produce no animations or sounds when blocked
**Plans**: 1 plan
**Status**: Complete

Plans:
- [x] 06-01-PLAN.md - Server-side attack callbacks + client-side animation mixin

### Phase 7: Crawl Pose
**Goal**: Downed players visually crawl with synchronized animations
**Depends on**: Phase 6
**Requirements**: DOWN-01, DOWN-03
**Success Criteria** (what must be TRUE):
  1. Player enters prone/crawling pose (EntityPose.SWIMMING) when entering downed state
  2. Player has visible crawling animation while in downed state
  3. Crawl pose syncs correctly in multiplayer (other players see downed player crawling)
  4. Pose resets to standing when revived or transitioning to spectator
**Plans**: 3 plans
**Status**: Complete

Plans:
- [x] 07-01-PLAN.md - Core pose override (server-side pose management + client-side mixin)
- [x] 07-02-PLAN.md - Movement restrictions (no jump, no sprint, no interactions)
- [x] 07-03-PLAN.md - Visual effects (blood vignette, particles, camera pitch limiting)

### Phase 8: Cinematic Enhancement
**Goal**: Death cinematic delivers intense, terrifying experience with extended grab and camera effects
**Depends on**: Phase 7
**Requirements**: CINE-01, CINE-02, CINE-03
**Success Criteria** (what must be TRUE):
  1. Death cinematic has extended, intense grab animation (longer than current 0.8s)
  2. Death cinematic includes camera shake effect during grab
  3. Camera shake can be disabled via config for accessibility
  4. Death sequence audio is synchronized to animation timing
  5. Cinematic effects work without causing motion sickness at 30/60/144 FPS
**Plans**: 3 plans
**Status**: Complete

Plans:
- [x] 08-01-PLAN.md - Extended death grab animation with GeckoLib sound keyframes
- [x] 08-02-PLAN.md - Camera shake system with exponential decay and config option
- [x] 08-03-PLAN.md - Compensation effects, FPS auto-reduction, and cinematic integration

## Progress

**Execution Order:**
Phases execute in numeric order: 5 → 6 → 7 → 8

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

---
*Last updated: 2026-01-26 after Phase 5 completion*
