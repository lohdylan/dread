# Requirements: Dread v1.1

**Defined:** 2026-01-25
**Core Value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.

## v1.1 Requirements

Requirements for polish & immersion milestone. Each maps to roadmap phases.

### Downed State

- [x] **DOWN-01**: Player enters crawl pose (EntityPose.SWIMMING) when downed
- [x] **DOWN-02**: Player cannot attack while in downed state (melee + projectile blocked)
- [x] **DOWN-03**: Downed player has visual crawling animation (not just pose flag)

### Texture

- [ ] **TEX-01**: Dread entity has scary horror texture replacing gray placeholder
- [ ] **TEX-02**: Dread texture includes emissive glowmask for eyes and details
- [ ] **TEX-03**: All 3 form variants (BASE, EVOLVED, ELDRITCH) have unique textures

### Cinematic

- [x] **CINE-01**: Death cinematic has extended, intense grab animation (longer than 0.8s)
- [x] **CINE-02**: Death cinematic includes camera shake effect with config disable option
- [x] **CINE-03**: Death sequence audio is synchronized to animation timing

### Audio

- [ ] **AUDIO-01**: Core horror sounds are audible (jump scare, death, ambient presence)
- [ ] **AUDIO-02**: Audio uses 3-layer soundscape (ambient drone + proximity + jump scare)
- [ ] **AUDIO-03**: Each sound event has multiple OGG variations for variety

## Future Requirements

Deferred to later milestones.

### Environmental Horror (v2.0)
- **ENV-01**: Dread manipulates environment (door slams, lights flicker)
- **ENV-02**: Blood trail visual when crawling while downed
- **ENV-03**: Dynamic crawl speed based on health

### Advanced Animation (v2.0)
- **ANIM-01**: Animated texture effects (pulsing runes)
- **ANIM-02**: Multiple camera angles during death cinematic
- **ANIM-03**: Procedural tentacle movement

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Sanity/hallucination system | High complexity, not core to jump scare focus |
| Multiple entity types | Focusing on Dread only for v1.1 |
| VR support | Requires separate input/camera systems |
| Mod compatibility layer | Focus on standalone experience |
| Real-time voice integration | Out of scope for horror mod |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| TEX-01 | Phase 5 | Pending |
| TEX-02 | Phase 5 | Pending |
| TEX-03 | Phase 5 | Pending |
| AUDIO-01 | Phase 5 | Pending |
| AUDIO-02 | Phase 5 | Pending |
| AUDIO-03 | Phase 5 | Pending |
| DOWN-02 | Phase 6 | Complete |
| DOWN-01 | Phase 7 | Complete |
| DOWN-03 | Phase 7 | Complete |
| CINE-01 | Phase 8 | Complete |
| CINE-02 | Phase 8 | Complete |
| CINE-03 | Phase 8 | Complete |

**Coverage:**
- v1.1 requirements: 12 total
- Mapped to phases: 12
- Unmapped: 0

---
*Requirements defined: 2026-01-25*
*Last updated: 2026-01-26 after Phase 8 completion*
