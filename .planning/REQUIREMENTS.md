# Requirements: Dread v1.2

**Defined:** 2026-01-26
**Core Value:** The jump scare must be genuinely terrifying — entity appearance, cinematic kill, and audio must combine to deliver real horror.

## v1.2 Requirements

Requirements for quick fixes milestone. Focus on bug fixes, gameplay polish, and testing.

### Bug Fixes

- [x] **FIX-01**: Death cinematic is smooth and readable — camera effects coordinated, player can clearly see Dread grab animation
- [x] **FIX-02**: Downed state clears properly when leaving world or creating new world
- [ ] **FIX-03**: grab_impact.ogg audio file exists and plays during death grab animation

### Gameplay

- [ ] **GAME-01**: Single-player downed state duration is brief (~30-60 seconds) before death
- [ ] **GAME-02**: Single-player death triggers normal Minecraft death (respawn at bed/spawn, not permanent spectator)
- [ ] **GAME-03**: Full cinematic still plays before single-player death
- [ ] **GAME-04**: Multiplayer retains current behavior (300s downed, revive or permanent spectator)

### Testing

- [ ] **TEST-01**: Multiplayer dedicated server testing completed
- [ ] **TEST-02**: Any multiplayer sync issues found during testing are fixed

## Future Requirements

Deferred to v2.0 Environmental Horror.

### Environmental Horror
- **ENV-01**: Dread manipulates environment (door slams, lights flicker)
- **ENV-02**: Blood trail visual when crawling while downed
- **ENV-03**: Dynamic crawl speed based on health

### Advanced Animation
- **ANIM-01**: Animated texture effects (pulsing runes)
- **ANIM-02**: Multiple camera angles during death cinematic

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Sanity/hallucination system | High complexity, not core to jump scare focus |
| Multiple entity types | Focusing on Dread only |
| VR support | Requires separate input/camera systems |
| Mod compatibility layer | Focus on standalone experience |
| New horror features | Save for v2.0, focus on fixing existing |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| FIX-01 | Phase 9 | Complete |
| FIX-02 | Phase 10 | Complete |
| FIX-03 | Phase 12 | Pending |
| GAME-01 | Phase 11 | Pending |
| GAME-02 | Phase 11 | Pending |
| GAME-03 | Phase 11 | Pending |
| GAME-04 | Phase 11 | Pending |
| TEST-01 | Phase 12 | Pending |
| TEST-02 | Phase 12 | Pending |

**Coverage:**
- v1.2 requirements: 9 total
- Mapped to phases: 9
- Unmapped: 0

---
*Requirements defined: 2026-01-26*
*Traceability updated: 2026-01-26*
