# Requirements: Dread

**Defined:** 2026-01-27
**Core Value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.

## v2.0 Requirements

Requirements for v2.0 Atmosphere & Dread milestone. Each maps to roadmap phases.

### Cinematic Camera

- [ ] **CAM-01**: Camera control transfers to cinematic system during death sequence (player loses input)
- [ ] **CAM-02**: Camera interpolates smoothly between positions (no jerky movement)
- [ ] **CAM-03**: Camera pulls back from player during initial death grab (show vulnerability)
- [ ] **CAM-04**: Camera integrates with existing render-time shake (mixin priority coordination)
- [ ] **CAM-05**: Camera follows multi-stage path (pull back → hold → zoom to face)
- [ ] **CAM-06**: Camera targets Dread's head position during face zoom (not body center)
- [ ] **CAM-07**: Camera timing defined via timeline keyframe system (declarative choreography)

### Animated Textures

- [ ] **TEX-01**: Dread texture changes during kill sequence (not static)
- [ ] **TEX-02**: Texture changes sync with cinematic timing (match camera movement)
- [ ] **TEX-03**: Emissive runes pulse during kill sequence (otherworldly glow)
- [ ] **TEX-04**: Dread form shows writhing effects (tentacles, shifting geometry)
- [ ] **TEX-05**: Eyes reveal during face zoom (eyes open as camera approaches)

### Environmental Effects

- [ ] **ENV-01**: Doors slam shut when Dread approaches (physical presence)
- [ ] **ENV-02**: Lights flicker when Dread is near (darkness follows entity)
- [ ] **ENV-03**: Effect intensity scales with Dread proximity (stronger when closer)
- [ ] **ENV-04**: Effects follow coordinated sequence (flicker → slam → spawn pattern)

### Blood Trail

- [ ] **BLD-01**: Visible blood trail when crawling while downed
- [ ] **BLD-02**: Blood trail fades over time (doesn't persist forever)
- [ ] **BLD-03**: Trail intensity based on time downed (starts heavy, lightens)
- [ ] **BLD-04**: Blood trail particle density configurable (performance option)

## Future Requirements

Deferred to v2.1 or later. Tracked but not in current roadmap.

### Gameplay Enhancements

- **PLAY-01**: Dynamic crawl speed based on health
- **PLAY-02**: Multiplayer dedicated server testing and fixes

### Visual Polish

- **VIS-01**: Shader-based pulsing effects (beyond .mcmeta animation)
- **VIS-02**: Environmental particle effects (dust, embers when Dread approaches)

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Blood spray on kill | Conflicts with cosmic horror tone (wrongness, not gore) |
| Monster vocalizations during kill | Silence is scarier than roaring |
| Slow-motion effects | Cheap trick, doesn't fit cosmic horror |
| Multiple Dread entity types | Focus on perfecting single entity first |
| Sanity/hallucination system | High complexity, not core to death cinematic |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CAM-01 | Phase 13 | Pending |
| CAM-02 | Phase 13 | Pending |
| CAM-03 | Phase 13 | Pending |
| CAM-04 | Phase 13 | Pending |
| CAM-05 | Phase 13 | Pending |
| CAM-06 | Phase 13 | Pending |
| CAM-07 | Phase 13 | Pending |
| TEX-01 | Phase 14 | Pending |
| TEX-02 | Phase 14 | Pending |
| TEX-03 | Phase 14 | Pending |
| TEX-04 | Phase 14 | Pending |
| TEX-05 | Phase 14 | Pending |
| ENV-01 | Phase 15 | Pending |
| ENV-02 | Phase 15 | Pending |
| ENV-03 | Phase 15 | Pending |
| ENV-04 | Phase 15 | Pending |
| BLD-01 | Phase 16 | Pending |
| BLD-02 | Phase 16 | Pending |
| BLD-03 | Phase 16 | Pending |
| BLD-04 | Phase 16 | Pending |

**Coverage:**
- v2.0 requirements: 20 total
- Mapped to phases: 20/20 (100%)
- Unmapped: 0

**Phase breakdown:**
- Phase 13 (Cinematic Camera Control): 7 requirements
- Phase 14 (Animated Entity Textures): 5 requirements
- Phase 15 (Environmental Effects): 4 requirements
- Phase 16 (Blood Trail Particles): 4 requirements

---
*Requirements defined: 2026-01-27*
*Last updated: 2026-01-27 after roadmap creation*
