# Project Research Summary

**Project:** Dread - Minecraft Horror Mod
**Domain:** Fabric 1.21.x Minecraft Modding
**Researched:** 2026-01-23
**Confidence:** HIGH

## Executive Summary

Minecraft horror mods are a mature domain with well-established patterns for creating sustained psychological dread. The most successful mods (Cave Dweller Reimagined, From The Fog, The Obsessed) prioritize intelligent stalking AI and atmospheric tension-building over cheap jump scares. Dread's planned approach—turn-around jump scares with Cthulhu-inspired entity, mining activity triggers, and unskippable death cinematics—aligns well with proven horror mechanics while offering unique differentiators.

The recommended technical stack is modern Fabric 1.21.11 with GeckoLib for complex entity animations, Cardinal Components API for multiplayer state synchronization, and vanilla sound systems for positional audio. This is a HIGH confidence recommendation based on official Fabric documentation and verified horror mod references. Critical warning: Minecraft 1.21.11 is the last obfuscated version—Yarn mappings will not be available after this release, requiring migration to Mojang Mappings for future versions.

Key risks center on multiplayer synchronization (client-server side confusion, packet validation, state persistence) and performance (entity memory leaks, sound channel limits, animation overhead). These pitfalls have well-documented solutions but require strict adherence to Fabric architectural patterns from day one. The horror genre amplifies the impact of technical bugs—entity despawning mid-scare or audio cutting out destroys immersion instantly. Prioritize bug-free core mechanics over feature count.

## Key Findings

### Recommended Stack

Fabric 1.21.11 provides a modern, lightweight modding platform with recent API rewrites (networking payloads, data components) that streamline development. The ecosystem has matured significantly since 1.20.5, with standard patterns for all core horror mod requirements.

**Core technologies:**
- **Fabric 1.21.11 + API 0.139.4**: Modern APIs with payload-based networking (1.20.5+ rewrite), HudElementRegistry for screen effects (replacing deprecated HudRenderCallback), and superior performance vs Forge
- **GeckoLib 8.x**: Industry standard for complex 3D entity animations with concurrent animation support, sound/particle keyframes, and Blockbench integration—essential for multi-state horror creature
- **Cardinal Components API 7.3.0+**: Automatic entity/world data persistence with save/sync/tick handling—critical for multiplayer downed/revive state that vanilla data components don't support
- **Java 21**: Required for Minecraft 1.20.5+, full JDK needed for compilation and debugging
- **Blockbench**: Model and animation creation with GeckoLib export capability

**Critical version warning:** 1.21.11 is the last Yarn-mapped version. Plan migration to Mojang Mappings if targeting post-1.21.11 versions.

### Expected Features

Horror mod success hinges on balancing psychological dread with playability. Jump scares are expected but must feel earned, not cheap.

**Must have (table stakes):**
- **Unique horror entity with distinctive design** — Core threat must be recognizable and unsettling (Cthulhu-inspired for Dread)
- **Stalking AI behavior** — Entity actively hunts player, spawns out of FOV, exhibits intelligent movement patterns
- **Atmospheric sound design** — Tension music, ambient echoes, proximity-based audio intensity that signals danger without spoiling surprise
- **Darkness/light mechanics** — Horror leverages darkness, entities extinguish torches/light sources, shadows as tactical element
- **Jump scare moments** — Well-timed scares as tension release after buildup (not constant spam)
- **Spawn progression** — Threat escalates over game days to prevent overwhelming new players while building dread
- **Configuration options** — Spawn rates, damage, difficulty customizable (players have different fear tolerances)

**Should have (competitive differentiators):**
- **Turn-around jump scare mechanic** — Dread's core value prop; entity spawns when player turns around (requires precise timing)
- **Unskippable death cinematic** — Forces player to experience consequence (5-10 seconds max, must be visually compelling)
- **Multi-phase stalking system** — Unpredictable escalation prevents pattern recognition that kills fear
- **Base-centric tension** — Entity knows where you live, makes "safe spaces" feel vulnerable
- **Mining activity trigger** — Turns safe activity dangerous, creates tension during core gameplay loop
- **Multiplayer downed/revive system** — Cooperative mechanic that maintains fear even with friends

**Defer (v2+):**
- **ARG-inspired storytelling** — Environmental messages, sign manipulation, meta-horror elements (nice polish but not core scare loop)
- **Multiple entity forms/variants** — Single terrifying form sufficient for MVP; variants add complexity without proportional value
- **Advanced fake-out behaviors** — Turn-around mechanic already creates surprise; fake-outs are polish
- **Environmental manipulation** — Entity building structures or moving blocks not aligned with Dread's theme (more Herobrine-style)

### Architecture Approach

Fabric mods follow modular, event-driven architecture with strict client/server separation. For multiplayer horror with critical timing, synchronization architecture is paramount.

**Major components:**
1. **Entity System (Server)** — Custom Dread entity with AI goals (stalking, positioning, attack), pathfinding, spawn logic. Communicates spawn events to clients via automatic entity tracking
2. **Game State System (Server)** — Player state manager for downed status, revive timers, death counters, spawn ticker with probability curves. Uses Cardinal Components for automatic save/sync
3. **Networking System (Both)** — Custom packet payloads (Java Records) for state synchronization: downed state updates, revive requests, cinematic triggers. Server validates all client requests
4. **Rendering System (Client)** — Entity renderer with GeckoLib model, HUD overlays for downed state (timer, blur effect), cinematic camera control, screen effects
5. **Audio System (Both)** — Sound event registration, spatial 3D audio playback, priority management to prevent channel exhaustion during jump scares

**Data flow patterns:**
- **Spawn:** Server spawn ticker → Entity creation → Automatic tracking sync → Client rendering + audio
- **Kill:** Server attack → Game state transition (downed) → S2C packet → Client cinematic trigger + HUD + audio sync
- **Revive:** Client input → C2S packet → Server validation (proximity, state) → State update → S2C broadcast → All clients update rendering

**Critical architectural rules:**
- Server is authoritative—all game logic server-side, clients only render
- Never exchange data directly between logical sides—always use packets
- Validate all client packets server-side (range, state, permissions)
- Use Cardinal Components for persistent state with automatic NBT serialization
- Implement entity cleanup in `remove()` to prevent memory leaks

### Critical Pitfalls

Based on documented Fabric mod failures and horror-specific requirements, these pitfalls can cause catastrophic failures or rewrites.

1. **Client-Server Side Confusion** — Mixing client and server code causes `ClassNotFoundException` crashes on dedicated servers and multiplayer desync. Prevention: Strict source set separation, use packets for all communication, server is authoritative for all game logic.

2. **Missing Packet Validation** — Trusting client data allows exploits (remote revives, teleporting while downed). Prevention: Validate entity existence, range-check distances, verify player permissions on server before applying effects.

3. **NBT Data Not Marked Dirty** — Downed state vanishes on server restart if not marked dirty. Prevention: Call `markDirty()` immediately after every persistent state change, create wrapper methods that auto-mark.

4. **Entity Memory Leak on Death/Despawn** — Minecraft bug MC-260605 causes entity memories to leak, compounding with respawning stalker entities. Prevention: Manually clear brain/memories in entity's `remove()` method from day one.

5. **Sound Channel Limit Exceeded** — OpenAL has 247 channel limit; layered horror atmospherics (ambient + tension + jump scare) can exhaust channels, causing jump scare audio to fail. Prevention: Implement sound priority system, stop ambient sounds before critical scares, limit concurrent atmospheric loops to 2-3.

6. **Jump Scare Becomes "Cheap"** — Loud noise without buildup causes annoyance not fear, no replay value. Prevention: Multi-stage tension system (30-60s subtle cues, proximity + time trigger, audio layering, visual staging in peripheral vision before full reveal).

## Implications for Roadmap

Based on research findings, component dependencies, and pitfall prevention requirements, recommended phase structure:

### Phase 1: Foundation & Core Entity
**Rationale:** Entity is the centerpiece of horror experience. Must be visible, animated, and functional before building mechanics around it. Foundational systems (networking, sounds) required by all subsequent phases.

**Delivers:**
- Fabric mod skeleton with proper client/server source sets
- Network payload system with registration infrastructure
- Sound event registration with placeholder audio
- Dread entity with GeckoLib model, texture, basic animations
- Entity registration with attributes, tracking range 128+
- Client renderer with shader compatibility testing

**Addresses:**
- Table stakes: Unique horror entity with distinctive design
- Stack: Fabric 1.21.11, GeckoLib 8.x, Java 21

**Avoids:**
- Pitfall #1 (client-server confusion): Proper source set separation from start
- Pitfall #4 (memory leaks): Implement `remove()` cleanup in initial entity class
- Pitfall #7 (z-fighting): Test rendering with Iris/Optifine shaders during model creation
- Pitfall #9 (tracking range): Set 128+ blocks in registration

**Research flag:** Standard patterns, skip detailed research. Use Fabric entity tutorial + GeckoLib docs.

### Phase 2: Stalking AI & Jump Scare Mechanics
**Rationale:** Core value proposition is turn-around jump scare. AI must intelligently position entity behind player based on FOV calculations and spawn timing. This is the highest-risk phase (timing makes or breaks horror experience).

**Delivers:**
- Custom AI goals: StalkPlayerGoal (out-of-FOV spawn positioning), JumpScareAttackGoal
- FOV raycasting and line-of-sight calculations
- Turn detection and precise entity placement behind player
- Mining activity trigger system (detects mining blocks → increases spawn probability)
- Multi-stage tension system: subtle audio cues (30s) → visual hint (10s) → random trigger (20-40s)
- Sound priority management system
- Jump scare audio with ambient sound stopping before critical moment

**Addresses:**
- Table stakes: Stalking AI behavior, jump scare moments
- Differentiators: Turn-around mechanic, mining activity trigger
- Features deferred: Keep to single entity form, no fake-outs (add in v2)

**Avoids:**
- Pitfall #5 (sound channels): Priority system implemented before layering atmospheric sounds
- Pitfall #15 (cheap scares): Multi-stage tension design from start, not retrofitted
- Pitfall #10 (animation performance): Keep bone count under 30 for complex stalking animations

**Research flag:** NEEDS RESEARCH. FOV calculations, raycasting for spawn positioning, and jump scare timing are complex integrations. Plan `/gsd:research-phase` for "Turn-around spawn mechanics" and "Sound priority management."

### Phase 3: Spawn Progression & Configuration
**Rationale:** Prevents overwhelming new players while maintaining long-term tension. Configuration is table stakes for horror mods (different fear tolerances). Relatively low-risk phase with well-documented patterns.

**Delivers:**
- Spawn ticker system with increasing probability over game days
- Random spawn ticker (Poisson distribution or similar for unpredictability)
- Configuration file with Fabric Config API: spawn rates, damage, health, enable/disable features, cinematic skippable option
- Day counter integration with world save data (PersistentState)
- Spawn cooldown management (prevent spam)

**Addresses:**
- Table stakes: Spawn progression, configuration options
- Features: Escalation over time to prevent adaptation

**Avoids:**
- Pitfall #3 (NBT dirty): Mark spawn ticker state dirty after every increment
- Complexity: Keep config simple—don't over-engineer early

**Research flag:** Standard patterns. Fabric Config API docs + PersistentState tutorial sufficient.

### Phase 4: Downed State & Death Mechanics
**Rationale:** Requires entity and game state systems working correctly. Downed state is core multiplayer feature and foundation for revive system. Death event handling is notoriously tricky—must get ordering correct before adding revive complexity.

**Delivers:**
- Cardinal Components API integration for player state (downed status, timer, death counter)
- Server-side downed state logic: transition from death → downed (prevent vanilla death)
- Death event handling with `ServerLivingEntityEvents.ALLOW_DEATH` (fires before totems)
- Downed timer countdown (300 seconds / 5 minutes)
- Downed → spectator mode transition after timeout
- S2C packet for downed state synchronization to all clients
- Client-side state cache for rendering decisions
- Totem of Undying compatibility testing

**Addresses:**
- Differentiators: Downed state as alternative to instant death
- Architecture: Game State System with Cardinal Components

**Avoids:**
- Pitfall #3 (NBT dirty): markDirty() in all state change wrapper methods
- Pitfall #8 (client-only data): Sync state to clients via packets, never access PersistentState on client
- Pitfall #14 (death event order): Use ALLOW_DEATH, test with totems, verify respawn button works

**Research flag:** NEEDS RESEARCH. Death event ordering, totem interaction, and spectator mode transitions have edge cases. Plan `/gsd:research-phase` for "Death event handling & totem compatibility."

### Phase 5: Client Rendering & HUD
**Rationale:** Depends on downed state data from Phase 4. Visual feedback for downed players and observers. Relatively straightforward rendering with HudElementRegistry.

**Delivers:**
- HudElementRegistry integration (replaces deprecated HudRenderCallback)
- Downed state HUD overlay: timer display, blur shader effect
- GuiGraphics rendering for translucent overlay
- Visual feedback for nearby downed players (icon, distance indicator)
- Screen shake/distortion effects during jump scare (optional polish)

**Addresses:**
- Table stakes: Visual feedback for downed state
- Stack: Fabric HUD API (modern HudElementRegistry)

**Avoids:**
- Pitfall #8 (client data): HUD reads from client-side state cache (synced via packets)
- Complexity: Use GuiGraphics for blur (simpler than Canvas Renderer shaders)

**Research flag:** Standard patterns. HudElementRegistry docs sufficient.

### Phase 6: Multiplayer Revive System
**Rationale:** Most complex system, depends on all previous phases. Requires client input → server validation → state update → broadcast to all clients. Final phase before polish.

**Delivers:**
- Revive detection: server-side proximity check (3 block radius), crouch input detection
- C2S revive request packet with target player UUID
- Server validation: verify proximity, verify target is downed, verify reviver is alive and not in cooldown
- Revive execution: remove downed state, restore health to 50%, clear timer
- S2C broadcast to all tracking clients with state update
- Revive cooldown per player (prevent spam)
- Progress bar for reviving player (visual feedback during hold)

**Addresses:**
- Differentiators: Multiplayer cooperative mechanics
- Architecture: Full networking stack (C2S request, server validation, S2C broadcast)

**Avoids:**
- Pitfall #2 (packet validation): Validate ALL conditions server-side before revive
- Pitfall #12 (hardcoded delays): Use scheduled tasks for progress bar, not Thread.sleep()
- Pitfall #1 (desync): Server is authoritative, clients wait for confirmation packet

**Research flag:** NEEDS RESEARCH. Proximity detection with moving players, crouch input in multiplayer, and validation edge cases (what if reviver attacked mid-revive?). Plan `/gsd:research-phase` for "Multiplayer revive validation & edge cases."

### Phase 7: Unskippable Death Cinematic
**Rationale:** Differentiator feature, highest production value requirement. Requires camera control, synchronized audio, and careful performance optimization to maintain 60 FPS. Final major feature before release prep.

**Delivers:**
- Camera manipulation: pan around entity, zoom to entity face, first-person POV lock
- Synchronized audio playback (death scream, entity sounds, ominous music)
- Cinematic timeline system (server tick count as reference, client interpolates)
- 5-10 second sequence (short enough to avoid frustration)
- Optional skip via config (respects player preference)
- Performance optimization: preload assets during tension phase, target 60 FPS minimum
- Motion sickness prevention: smooth interpolation, no rapid camera movements

**Addresses:**
- Differentiators: Unskippable death cinematic (unique to Dread)
- Table stakes: Make death meaningful vs instant respawn

**Avoids:**
- Pitfall #10 (performance): Test FPS during cinematic on low-end hardware, optimize before launch
- Pitfall #15 (cheap design): Cinematic must be visually compelling, not just black screen
- Frustration: Keep SHORT, make skippable via config

**Research flag:** NEEDS RESEARCH. Camera manipulation APIs, synchronized audio/video playback across network latency, and interpolation for smooth 60 FPS. Plan `/gsd:research-phase` for "Camera control & cinematic synchronization."

### Phase 8: Polish & Release Prep
**Rationale:** Testing, compatibility, optimization before public release.

**Delivers:**
- Full multiplayer testing (2-4 players, dedicated server)
- Shader compatibility verification (Iris, Optifine)
- Memory leak testing (1+ hour continuous play)
- Sound channel stress testing (multiple players, layered audio)
- Fabric API version compatibility range testing
- Configuration validation (all options functional)
- Documentation: README, CurseForge/Modrinth descriptions, feature showcase video

**Addresses:**
- All pitfalls: Final validation before release
- Table stakes: Mod must work reliably with popular mod combinations

### Phase Ordering Rationale

**Dependency-driven order:**
- Foundation must precede all features (networking, entity)
- AI/jump scare requires entity to exist but can develop before state systems
- Downed state blocks revive system (can't revive without downed state)
- HUD requires downed state data to display
- Cinematic is last major feature (depends on entity, state, rendering)

**Risk-based prioritization:**
- Phase 2 (AI/jump scare) is highest-risk—core value prop, complex timing. Tackle early while budget/motivation high
- Phase 6 (revive) is most complex networking—defer until patterns proven in simpler systems
- Phase 7 (cinematic) is highest production value—save for when core loop proven fun

**Pitfall prevention:**
- Client-server separation (Phase 1) prevents rewrites later
- Memory leak cleanup (Phase 1) must be in initial entity code
- Sound priority (Phase 2) must be designed before layering audio
- Death event order (Phase 4) must be correct before adding revive complexity

**Horror-specific considerations:**
- Entity appearance (Phase 1) is first impression—must be high quality
- Jump scare timing (Phase 2) is make-or-break—can't ship if this fails
- Configuration (Phase 3) respects different fear tolerances—table stakes for accessibility
- Bugs that break immersion (entity despawning, sound cutting out) are 10x worse for horror—prioritize stability over features

### Research Flags

**Phases needing deeper research during planning:**
- **Phase 2 (Jump Scare Mechanics):** Complex integration of FOV raycasting, spawn positioning, and timing. Research topics: "Turn-around spawn mechanics," "Sound priority management"
- **Phase 4 (Downed State):** Death event ordering has edge cases with totems, other mods. Research topic: "Death event handling & totem compatibility"
- **Phase 6 (Multiplayer Revive):** Validation edge cases in networked environment. Research topic: "Multiplayer revive validation & edge cases"
- **Phase 7 (Death Cinematic):** Camera APIs and network synchronization for smooth playback. Research topic: "Camera control & cinematic synchronization"

**Phases with standard patterns (skip research-phase):**
- **Phase 1 (Foundation):** Well-documented Fabric tutorials for entity creation, networking, sounds
- **Phase 3 (Spawn Progression):** Standard config + PersistentState patterns
- **Phase 5 (Client Rendering):** HudElementRegistry has official docs and examples
- **Phase 8 (Polish):** Testing methodology, no novel technical patterns

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | **HIGH** | Verified from official Fabric docs, example mod build.gradle, API changelogs. GeckoLib confirmed as industry standard for complex entity animations. Cardinal Components verified as recommended solution for entity data. |
| Features | **MEDIUM** | Based on WebSearch analysis of popular horror mods (Cave Dweller, From The Fog, The Obsessed) and horror game design articles. Cross-referenced across multiple sources but not authoritative domain expertise. Table stakes vs differentiators validated by community consensus. |
| Architecture | **MEDIUM** | Official Fabric documentation for networking, entity system, HUD rendering patterns (HIGH confidence). Specific horror mod architecture patterns inferred from ecosystem survey and community mods (MEDIUM confidence). Client/server separation and synchronization patterns verified in official docs. |
| Pitfalls | **HIGH** | Client-server confusion, packet validation, NBT persistence verified in official Fabric docs and wiki. Entity memory leak documented in Minecraft bug MC-260605 and MemoryLeakFix mod. Sound channel limits documented in OpenAL specifications. Horror-specific design pitfalls from game design research (MEDIUM confidence but cross-validated). |

**Overall confidence:** **HIGH** for technical implementation, **MEDIUM** for horror game design specifics.

### Gaps to Address

**Version migration path:**
- Minecraft 1.21.11 is last obfuscated version; Yarn mappings will not be available after this release
- How to handle: Complete initial development on 1.21.11, monitor for 26.1 release timeline, plan migration to Mojang Mappings when next version releases. Not urgent for Phase 1-8 but flag for future roadmap.

**Jump scare timing calibration:**
- Research provides principles (buildup, multi-stage, timing unpredictability) but exact timing values (how long before entity spawns? how long for tension phase?) require playtesting
- How to handle: Implement configurable timing parameters in Phase 2, conduct internal playtesting to calibrate values, gather external playtester feedback before Phase 8 release

**Multiplayer fear dynamics:**
- Research indicates horror works differently in groups (1-3 players: amplified fear, 4+ players: becomes comedy)
- How to handle: Design scares for 1-3 players (Phase 2-7). If targeting larger groups, shift emphasis from jump scares to atmosphere in post-launch updates. Flag for Phase 6 multiplayer testing.

**Shader compatibility edge cases:**
- Research identifies z-fighting risk with Iris/Optifine but specific entity rendering mode for Dread model unknown until model created
- How to handle: Test with shaders during Phase 1 entity creation, iterate rendering mode if issues found. Budget extra time in Phase 1 for shader troubleshooting.

**Performance baseline for cinematic:**
- Target is 60 FPS minimum on low-end hardware, but "low-end" spec unknown (what GPU/CPU?)
- How to handle: Define performance baseline during Phase 7 planning (e.g., "Minecraft runs at 60 FPS with vanilla at normal render distance" as minimum spec). Test cinematic on baseline hardware before Phase 8.

**Death event mod compatibility:**
- Research identifies totem interaction as known pitfall, but compatibility with other death-modifying mods (corpse mods, gravestone mods) unknown
- How to handle: Test with popular death mods during Phase 4. If conflicts found, document incompatibilities or add compatibility layer based on popularity.

## Sources

### Primary (HIGH confidence)
- [Fabric Development Portal](https://fabricmc.net/develop/) — Modern API documentation, networking, HUD rendering
- [Fabric Wiki](https://wiki.fabricmc.net/) — Entity creation, persistent states, mixin tutorials, side separation
- [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod) — build.gradle, gradle.properties, mod structure
- [Cardinal Components API - GitHub](https://github.com/Ladysnake/Cardinal-Components-API) — Entity/world data persistence
- [GeckoLib Wiki](https://github.com/bernie-g/geckolib/wiki) — Entity animations (GeckoLib4)
- [Fabric Documentation - Custom Sounds](https://docs.fabricmc.net/develop/sounds/custom) — OGG Vorbis format, sound registration
- [Fabric Documentation - Networking](https://docs.fabricmc.net/develop/networking) — 1.20.5+ payload system

### Secondary (MEDIUM confidence)
- [Top Minecraft Horror Mods - CurseForge](https://blog.curseforge.com/top-minecraft-horror-mods/) — Feature landscape research
- [Cave Dweller Reimagined - CurseForge](https://www.curseforge.com/minecraft/mc-mods/cave-dweller-reimagined) — Stalking AI patterns
- [From The Fog Official Site](https://lunareclipse.studio/creations/from-the-fog) — Environmental manipulation, ARG elements
- [The Obsessed Mod - CurseForge](https://www.curseforge.com/minecraft/mc-mods/obsessed) — Multi-phase stalking system
- [Horror Game Design - Game Developer](https://www.gamedeveloper.com/design/creating-horror-through-level-design-tension-jump-scares-and-chase-sequences) — Jump scare theory
- [Jump Scare Design - Game Developer](https://www.gamedeveloper.com/design/a-lack-of-fright-examining-jump-scare-horror-game-design) — What makes scares cheap vs effective
- [MemoryLeakFix - GitHub](https://github.com/FxMorin/MemoryLeakFix) — Entity memory leak bug MC-260605
- [Audio Engine Tweaks - Modrinth](https://modrinth.com/mod/audio-engine-tweaks) — Sound channel limits, priority system

### Tertiary (LOW confidence, needs validation)
- Community horror mod surveys — Ecosystem patterns, not verified technical implementations
- Horror game design articles — General principles, not Minecraft-specific
- Forge Forums — Rendering z-fighting discussions (Forge not Fabric, patterns may differ)

---
*Research completed: 2026-01-23*
*Ready for roadmap: yes*
