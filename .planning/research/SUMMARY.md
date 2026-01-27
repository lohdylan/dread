# Project Research Summary

**Project:** Dread v2.0 - Cinematic Death & Environmental Horror
**Domain:** Minecraft Fabric Mod - Horror Game Atmosphere
**Researched:** 2026-01-27
**Confidence:** HIGH

## Executive Summary

v2.0 of the Dread horror mod transforms the existing 4.5-second death sequence into a cinematic horror experience through camera control, animated entity textures, environmental effects, and blood trail particles. Research shows that **no new runtime dependencies are required** — all features can be implemented using the existing Fabric API, GeckoLib 4.7.1, and vanilla Minecraft systems.

The recommended approach uses **Camera.setPos() for direct camera positioning**, **GeckoLib .mcmeta for simple texture animations** (pulsing runes), **custom FeatureRenderer for complex animations** (writhing forms with UV offsets), **AI goals for environmental interactions** (door slams, light flickers), and **Fabric's particle system for blood trails**. The architecture centers on a single source of truth pattern: all cinematic components query `DeathCinematicClientHandler.getProgress()` for synchronized timing across camera movement, texture changes, and environmental effects.

The key risk is **render thread separation in Minecraft 1.21+**, which prevents direct entity manipulation during rendering. All render decisions must be pre-computed during `extractRenderState()` and passed via GeckoLib's DataTicket system. Secondary risks include **animated texture performance collapse** (Minecraft uploads all animated textures every tick, causing FPS drops with AMD GPUs), **camera transformation feedback loops** (improper timing causes recursive updates and crashes), and **multiplayer particle desync** (particles spawn client-side only without explicit server packets). Mitigation strategies include using GeckoLib's animation system over .mcmeta where possible, applying camera transforms at render time only via mixin order coordination, and implementing WorldServer.spawnParticle for multiplayer sync.

## Key Findings

### Recommended Stack

All v2.0 features leverage the validated v1.0 stack with zero new runtime dependencies. Camera positioning uses vanilla `Camera.setPos()` and `Camera.moveTo()` methods available since 1.17. Animated textures have three viable approaches: GeckoLib .mcmeta for simple looping animations (zero code), custom RenderLayer with tick-based UV offsets for complex effects (full control), and shader-based animation via Satin API (not recommended due to complexity). Environmental effects use standard Entity AI goals and block state manipulation. Blood trail particles use Fabric's particle registration API with `DripParticle.BloodFactory` for realistic drip behavior.

**Core technologies (unchanged from v1.0):**
- **Minecraft 1.21.1** — Camera class, Entity AI, particle system all sufficient for v2.0
- **Fabric API 0.116.8+** — Particle registration, entity events, client tick system
- **GeckoLib 4.7.1** — Entity animations, .mcmeta texture support, FeatureRenderer pattern
- **Satin API 1.17.0** — Existing post-processing shaders (vignette, blur) unaffected by v2.0

**Critical insight from stack research:** Minecraft's render thread separation in 1.21+ means texture selection cannot depend on live entity state during rendering. All render data must be pre-computed in the game thread during `extractRenderState()` and passed via DataTickets.

### Expected Features

Horror game research reveals that death sequences are about **forced confrontation** — removing player control and using camera movement to build from dread (pull back, see context) to terror (zoom to monster face). The best implementations create a narrative arc through multi-stage camera movement rather than static positioning.

**Must have (table stakes):**
- **Camera control transfer** — Player expects loss of control during death (Third Person Death mod proves this)
- **Smooth camera interpolation** — Jarring cuts break immersion (Hermite/cubic interpolation standard)
- **Camera pull-back reveal** — Shows context of what's killing player (Dead Space pattern)
- **Duration control (4-5 sec)** — Existing 4.5s timing already matches horror game standard
- **Respawn prevention during sequence** — Already implemented in v1.0
- **Audio cues during sequence** — Visual + audio = complete experience

**Should have (differentiators):**
- **Monster face zoom finale** — Fatal Frame principle: force player to look at scary thing
- **Multi-stage camera movement** — Pull back → pause → zoom creates narrative arc
- **Animated monster texture during kill** — Static texture = cheap, animated = intentional performance
- **Environmental reaction to kill** — World responds: lights flicker, doors slam
- **Blood trail from crawling** — Visual consequence of downed state

**Design principles from research:**
- **Cosmic horror = wrongness, not violence** — Focus on impossible textures and uncanny movement, not gore
- **Less is more until reveal** — Keep monster partially obscured until final zoom
- **Variation prevents habituation** — Multiple texture states keep deaths from feeling identical

### Architecture Approach

v2.0 integrates with existing architecture through a **single orchestrator pattern**: `DeathCinematicClientHandler` owns the progress timer (0.0 → 1.0 over 4.5s) and all components query this static value rather than maintaining separate timers. This ensures frame-perfect synchronization across camera positioning, texture animation, and environmental triggers.

**Major components:**

1. **CinematicCameraController** (NEW) — Overrides camera position/rotation using Camera.setPos(), integrates with existing CameraMixin at higher priority (950 vs 900) so shake applies additively on top of cinematic positioning, uses timeline keyframe system for multi-stage paths

2. **DreadTextureAnimator** (NEW) — Manages texture state during kill sequence, uses GeckoLib .mcmeta for simple pulsing effects, custom FeatureRenderer with UV offsets for writhing forms, pre-computes all render data in extractRenderState() to comply with 1.21+ render thread separation

3. **ProximityEffectManager** (NEW) — Client-side distance calculation in tick event, triggers door slams via AI goals and server packets, triggers light flickers via particle effects, uses spatial partitioning to optimize chunk-wide scans

4. **CrawlBloodTrailEffect** (NEW) — Spawns particles during downed crawl, integrates with existing DownedStateClientHandler lifecycle, uses WorldServer.spawnParticle for multiplayer sync, rate-limited to 2-4 particles/second for performance

**Render pipeline integration:**
```
ClientTickEvents.END_CLIENT_TICK:
  → DeathCinematicClientHandler.tick() updates progress
  → ProximityEffectManager evaluates triggers

GameRenderer.render():
  → CameraMixin applies shake (order 900)
  → CinematicCameraController applies position override (order 950)
  → DreadEntity.extractRenderState() queries progress, pre-computes texture/UV
  → DreadRenderer renders with FeatureRenderers for animated layers
  → Particle system renders blood trail
```

**Key architectural decision:** Use mixin order coordination (explicit priority values) to prevent conflicts. Camera positioning mixin runs after shake mixin (higher priority = later application), allowing shake to remain an additive effect on top of cinematic positioning.

### Critical Pitfalls

1. **Camera transformation feedback loops** — Modifying camera during entity update phase creates circular dependency (camera affects entity which triggers camera update). Apply ALL camera transforms in render-time mixins ONLY, never modify entity position for camera effects. Detection: rapid console spam, single-digit FPS, MatrixStack overflow. **Phase 1 must establish this architecture before proceeding.**

2. **Animated texture atlas performance collapse** — Multiple animated textures cause severe FPS drops (60 to 6-10) because Minecraft uploads ALL animated textures to GPU every tick (20x/second), even when not visible. AMD GPUs particularly affected. Prevention: use GeckoLib animated models instead of .mcmeta where possible, batch animations (one 16-frame cycle for all runes, not separate animations). **Phase 2 must design texture system with performance in mind from start.**

3. **GeckoLib 5 render thread separation** — Cannot access entity fields during rendering in 1.21+. All render decisions must be pre-computed in extractRenderState() and passed via DataTickets. Prevention: always use extractRenderState() for entity → render data transfer, test thoroughly on 1.21+. **Affects Phase 2 architecture fundamentally.**

4. **Particle trail multiplayer desync** — Blood trail particles appear only for crawling player because particle spawning is client-side only by default. Use WorldServer.spawnParticle with range parameter for multiplayer sync, check !world.isRemote before spawning server-side. **Phase 4 must implement networking from start.**

5. **Death screen GUI conflicts** — Vanilla death screen forces specific camera behaviors that conflict with custom camera control, causing snaps between states. Cancel death screen rendering via mixin injection at HEAD with cancellable=true, use custom GUI overlay, trigger respawn programmatically after cinematic. **Phase 1 must handle death screen interaction before full sequence.**

## Implications for Roadmap

Based on research, recommended phase structure follows technical dependencies and risk mitigation:

### Phase 1: Cinematic Camera Control
**Rationale:** Camera override is the foundation — all other features are visible through the camera, and it has the highest technical complexity. Must establish camera positioning architecture before other visuals matter.

**Delivers:**
- Multi-stage camera path (pull back → zoom to face)
- Timeline keyframe system with smooth interpolation (Hermite curves)
- Integration with existing CameraMixin (mixin order coordination)
- Death screen conflict resolution

**Addresses:**
- Camera control transfer (table stakes)
- Smooth interpolation (table stakes)
- Pull-back reveal (table stakes)
- Monster face zoom finale (differentiator)

**Avoids:**
- Camera transformation feedback loops (Pitfall 1) by applying transforms at render time only
- Death screen conflicts (Pitfall 5) by cancelling vanilla screen during cinematic

**Technical validation needed:**
- Test camera positioning without entity manipulation (validate no feedback loops)
- Verify smooth interpolation at 144fps
- Confirm existing Satin shaders still apply correctly
- Test with doImmediateRespawn ON and OFF

### Phase 2: Animated Entity Textures
**Rationale:** Requires camera to be positioned correctly to see effects. Depends on understanding GeckoLib 5 render state system. Can be developed in parallel with Phase 3.

**Delivers:**
- GeckoLib .mcmeta implementation for pulsing runes (simple, zero-code)
- Custom FeatureRenderer for writhing forms (complex, UV offsets)
- Synchronization with DeathCinematicClientHandler.getProgress()
- Event-driven texture switching (triggered by cinematic progress)

**Uses:**
- GeckoLib 4.7.1 .mcmeta support
- FeatureRenderer pattern (like vanilla slime outer layer)
- DataTicket system for render thread safety

**Implements:**
- DreadTextureAnimator component
- extractRenderState() pre-computation pattern

**Avoids:**
- Animated texture performance collapse (Pitfall 2) by using GeckoLib over .mcmeta where possible
- GeckoLib timing conflicts (Pitfall 5 from PITFALLS.md) by using single progress query
- Render thread access violations (Pitfall 3) by pre-computing in extractRenderState()

**Technical validation needed:**
- Profile FPS with AMD GPUs (high-risk hardware for texture animation)
- Test 3+ animated Dread entities simultaneously
- Verify animation sync over 60+ seconds (detect drift)
- Confirm glowmask compatibility with animation approach

### Phase 3: Environmental Effects (Proximity-Based)
**Rationale:** Independent of camera/texture systems — can be developed in parallel with Phase 2. Uses proven patterns (AI goals, block state changes).

**Delivers:**
- ProximityEffectManager with client tick event registration
- Door slam AI goal with server packet sync
- Light flicker trigger with particle effects
- Distance calculation optimization (spatial partitioning)

**Uses:**
- Entity AI goal system (vanilla)
- Block state manipulation (DoorBlock.OPEN, CampfireBlock.LIT)
- Fabric ClientTickEvents
- Custom networking for block state sync

**Implements:**
- DreadDoorSlamGoal (extends vanilla Goal class)
- DreadLightExtinguishGoal (priority 3 in goal system)
- Proximity detection with 8-12 block radius

**Avoids:**
- Client-server desync (Pitfall 7 from PITFALLS.md) by sending state changes via packets
- Performance issues by caching nearby doors/lights, only rescanning on chunk changes

**Technical validation needed:**
- Test in actual multiplayer (not just local server)
- Verify BlockEntity data sync for light states
- Performance test with chunk-wide scans (spatial partitioning needed)
- Test with 3+ players in same area triggering effects

### Phase 4: Blood Trail Particles
**Rationale:** Simplest component, depends on existing DownedStateClientHandler. Good final polish item with no dependencies on other phases. Can be done quickly while others work on Phases 1-3.

**Delivers:**
- CrawlBloodTrailEffect with particle spawning
- Integration with DownedStateClientHandler lifecycle
- Particle rate limiting (2-4 particles/second)
- Multiplayer sync via WorldServer.spawnParticle

**Uses:**
- Fabric particle registration API
- DripParticle.BloodFactory (vanilla blood drop behavior)
- Existing downed state detection

**Implements:**
- Particle spawn logic in DownedStateClientHandler tick
- Custom blood_drip particle type with JSON configuration

**Avoids:**
- Particle multiplayer desync (Pitfall 4) by using WorldServer.spawnParticle with range parameter
- Performance issues by spawning every 5 ticks (not every tick), setting 2-3 second lifetime

**Technical validation needed:**
- Test in multiplayer with 5+ players
- Profile particle count with multiple crawling players
- Verify particles appear for all clients at correct positions
- Test performance impact (target: no FPS drop)

### Phase Ordering Rationale

**Why camera first:** Everything else is rendered through the camera. Camera positioning must work correctly before other visuals matter. Highest technical complexity (mixin coordination, feedback loop avoidance) means getting this right establishes patterns for rest of v2.0.

**Why textures second:** Requires camera to be positioned to see effects. Builds on understanding of GeckoLib render system. Medium-high complexity warrants dedicated focus after camera foundation.

**Why proximity third (parallel with textures):** Independent system with no dependencies on camera or textures. Proven patterns (AI goals, block states) make this lower risk. Can be developed simultaneously with Phase 2 by different developer or in alternating focus.

**Why blood trail last:** Simplest feature (straightforward particle system). Good final polish item. No dependencies mean it can wait until core cinematic experience is complete. Fast implementation makes it ideal for final sprint.

**Parallelization opportunities:**
- Phases 2 and 3 are fully independent and can run simultaneously
- Phase 4 can start early if developer wants quick win (1-2 days implementation)

### Research Flags

**Phases needing deeper research during planning:**
- **Phase 1 (Camera):** HIGH — Complex mixin coordination, need to prototype timeline keyframe system and verify camera positioning math doesn't create feedback loops. Consider spike: 1-day prototyping Camera.setPos() before committing to architecture.
- **Phase 2 (Textures):** MEDIUM — UV offset application method needs validation (vertex manipulation vs shader approach). GeckoLib .mcmeta integration straightforward, but custom FeatureRenderer for UV animation requires testing approach. Consider spike: test UV offset in simple FeatureRenderer before designing full system.

**Phases with standard patterns (skip research-phase):**
- **Phase 3 (Environmental):** LOW — AI goals and block state changes well-documented, multiple reference implementations (Cave Dweller, Spooky Doors). Standard Fabric patterns apply.
- **Phase 4 (Particles):** LOW — Particle system unchanged since 1.16, official Fabric docs comprehensive, Entity Blood Particles mod provides reference implementation.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Camera.setPos() documented in Yarn mappings, GeckoLib wiki official, Fabric API stable. All v2.0 features use mature APIs (3+ year track record). No new dependencies needed. |
| Features | MEDIUM | Horror game patterns well-documented (Fatal Frame, Dead Space references), but Minecraft-specific implementation requires validation. Third Person Death mod proves camera cinematics feasible, but custom multi-stage paths need prototyping. |
| Architecture | MEDIUM | GeckoLib 5 render thread separation officially documented, but render state pre-computation pattern less documented in community. Mixin order coordination proven in Free Camera API, but priority values need testing with existing CameraMixin. |
| Pitfalls | HIGH | Camera feedback loops verified with MatrixStack documentation. Animated texture performance has official bug report (MC-132488) with benchmarks. Multiplayer particle desync documented in official bug (MC-10369). GeckoLib limitations officially documented in wiki. |

**Overall confidence:** HIGH

Research quality is strong with official documentation for technical stack (Fabric API, GeckoLib wiki, Yarn mappings), horror game design patterns validated across multiple AAA titles, and pitfalls backed by official bug reports and mod implementations. Medium areas (feature specifics, architecture patterns) reflect Minecraft modding's sparse documentation rather than uncertainty — proven via reference mods (Third Person Death, Cave Dweller, Entity Blood Particles).

### Gaps to Address

**UV offset application method:** Stack research identifies two approaches (vertex manipulation vs shader-based), but neither has definitive implementation guide. **Resolution:** Prototype both approaches in Phase 2 spike (1 day), select based on performance and compatibility with existing Satin shaders.

**Camera timeline keyframe system:** Architecture describes pattern but no existing Minecraft implementation found (Aperture API is closest reference). **Resolution:** Design custom timeline system in Phase 1, use Hermite interpolation formula from CMDCam as reference. Consider extracting to reusable utility if successful.

**Light extinguishing scope:** Pitfalls note campfires have native LIT property, but torches/lanterns require creative solution (block replacement or custom unlit block). **Resolution:** Phase 3 starts with campfire-only support (zero risk), evaluate torch support as stretch goal after core system working.

**Texture animation count:** Pitfall 3 notes Minecraft designed for ~21 animated textures total. **Resolution:** Inventory all animated textures across mod during Phase 2 planning (target: <10 total), prioritize GeckoLib animated models over .mcmeta where performance testing shows issues.

## Sources

### Primary (HIGH confidence)
- **Fabric API Documentation** (official) — ClientTickEvents, particle system, entity events, mixin injection patterns
- **GeckoLib Wiki** (official) — GeckoLib 5 render thread separation, animated textures .mcmeta support, FeatureRenderer pattern, DataTicket system
- **Minecraft Wiki** (official) — Camera mechanics, particle system, death screen behavior, block state properties
- **Yarn Mappings** (official) — Camera.setPos(), Camera.moveTo(), Entity AI goals, particle APIs
- **Mojang Bug Tracker** (official) — MC-132488 (animated texture performance), MC-10369 (particle multiplayer sync)

### Secondary (MEDIUM confidence)
- **Third Person Death mod** (Modrinth) — Proves cinematic death camera feasible in Minecraft, reference for camera manipulation during death
- **CMDCam mod** (CurseForge) — Hermite interpolation reference, smooth camera path implementation
- **Cave Dweller mod** (CurseForge) — Light extinguishing on proximity, environmental effects reference
- **Entity Blood Particles mod** (Modrinth) — Blood trail particle reference implementation
- **Free Camera API** (mod analysis) — Camera modifier priority system pattern
- **Horror game research** — Fatal Frame forced confrontation principle, Dead Space death spectacle pattern, SOMA proximity distortion

### Tertiary (LOW confidence — needs validation)
- **Aperture API** — Keyframe system reference (not directly applicable, different use case)
- **Entity Texture Features mod** — Texture animation proof of concept (different approach than GeckoLib)
- **Spooklementary shaders** — Emissive pulsing reference (shader-based, not entity texture)

---

**Research completed:** 2026-01-27
**Ready for roadmap:** Yes

**Next step:** Roadmap creation should use the four-phase structure outlined above, with Phase 1 (Camera) as highest priority foundation. Consider 1-day spikes for camera positioning math and UV offset approach before committing to detailed implementation plans.
