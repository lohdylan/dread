# Project Research Summary

**Project:** Dread Horror Mod v1.1 Polish & Immersion
**Domain:** Minecraft Horror Mod Enhancement (Fabric 1.21.1)
**Researched:** 2026-01-25
**Confidence:** HIGH

## Executive Summary

The Dread v1.1 enhancements add critical polish to an existing horror mod through five focused improvements: crawl pose when downed, attack prevention while downed, scary dread texture, intense cinematic kill, and real horror audio. Research reveals these features integrate cleanly with the existing v1.0 architecture through well-established Fabric API extension points, requiring **zero new runtime dependencies**. The existing stack (Minecraft 1.21.1, Fabric Loader 0.18.2, Fabric API, GeckoLib 4.7.1, Satin API 1.17.0) already provides all necessary capabilities. Development shifts from technology acquisition to asset creation workflows using Blockbench for textures and Audacity for audio.

The recommended approach prioritizes **asset creation first** (textures and audio file replacements require zero code changes), followed by **simple event handlers** (attack prevention), then **moderate complexity features** (crawl pose with client-server sync), and finally **complex cinematic enhancements** (camera effects with accessibility concerns). This sequencing enables early visual/audio wins while deferring integration complexity. Most work is additive through new event handlers rather than modifying existing systems.

Key risks center on **client-server synchronization** for crawl pose (multiplayer desync can cause position rubberbanding and immersion breaks), **sound channel saturation** when adding v1.1 audio on top of v1.0's existing system (Minecraft's ~28 channel limit), and **GeckoLib animation conflicts** between pose overrides and existing entity animations. Mitigation requires integrating with v1.0's proven client-server separation patterns, extending the existing sound priority system rather than bypassing it, and coordinating animation controllers from day one.

## Key Findings

### Recommended Stack

**No runtime dependency changes required.** All v1.1 enhancements leverage existing Fabric API capabilities: crawl pose uses Entity API's `setPose(EntityPose.SWIMMING)`, attack prevention uses `ServerLivingEntityEvents.ALLOW_DAMAGE`, texture improvements are PNG file replacements in the existing GeckoLib resource structure, camera shake can optionally use fabric-camera-shake library, and audio replacement is standard OGG Vorbis file swapping in the existing sound system.

**Core technologies (unchanged from v1.0):**
- **Minecraft 1.21.1**: Target version with stable Fabric support
- **Fabric API (latest for 1.21.1)**: Provides all necessary event callbacks and entity manipulation APIs
- **GeckoLib 4.7.1**: Existing entity animation system supports texture variants through file replacement
- **Satin API 1.17.0**: Existing shader compatibility for visual effects remains unchanged

**Development tools (new for v1.1 asset creation):**
- **Blockbench (latest 2026)**: Industry standard for Minecraft entity textures with direct 3D painting, eliminates UV misalignment — free, open-source, no alternatives needed
- **Audacity 3.x+**: Professional audio editing with native OGG Vorbis mono export for Minecraft's positional audio requirements — GPL, free
- **fabric-camera-shake (optional)**: Library for cinematic screen shake with multi-mod compatibility — evaluate during implementation if shake fits horror tone

**Asset sources:**
- Royalty-free horror audio from Pixabay (CC0) or Mixkit (no attribution), processed in Audacity for pitch shift, reverb, distortion
- Audio format requirements: OGG Vorbis, mono channel (critical for directional sound), 44100 Hz sample rate, quality 5-7 (~160-192 kbps)

### Expected Features

Research identified clear table stakes (features users expect without which v1.1 feels incomplete) versus differentiators (features that elevate from working to terrifying).

**Must have (table stakes):**
- **Prone/crawling pose during downed state**: Visual feedback for game state change — Dead by Daylight established 240-second bleed-out with slow crawl as genre standard; visual state must match mechanical state
- **Input blocking during death cinematic**: Prevents immersion break — players expect control loss during death sequence; continuing to attack during own death breaks horror
- **Non-default Dread appearance**: Visual identity distinct from vanilla mobs — baseline for custom entity, must evoke cosmic horror theme
- **Death cinematic audio**: Synchronized sound for 4.5-second sequence — silent death is immersion break, needs kill sound/scream
- **Dread presence audio**: Entity awareness cue — breathing, footsteps, or ambient sound indicating Dread is near

**Should have (high-value differentiators):**
- **Emissive texture elements**: Glowing eyes/runes visible in darkness — prevents invisibility frustration while signaling otherworldly nature (cosmic horror identity)
- **Ambient soundscape**: Background drones/atmosphere — professional audio feel, transforms from "working" to "terrifying"
- **Proximity-based audio intensity**: Sounds intensify as Dread approaches — escalating tension, creates dread before visual spawn

**Defer to post-v1.1:**
- **Animated textures**: Requires shader research, high complexity for replayability benefit not critical for v1.1
- **Multiple camera angles**: Increases scope significantly, better to nail fundamentals first
- **Blood trail visuals**: Nice-to-have environmental storytelling, not essential for horror impact
- **Dynamic crawl speed based on recovery**: Mechanical complexity, minor gameplay benefit

### Architecture Approach

v1.1 enhancements integrate through well-established extension points in the existing v1.0 architecture. Most work is **additive** (new event handlers, extended client timers) rather than modification of existing systems. Only one existing file needs substantial changes: `DownedStateClientHandler.java` for pose rendering adjustments.

**Major components:**
1. **Crawl pose system** — Server-side `DownedPoseHandler.java` applies `setPose(EntityPose.SWIMMING)` while downed (auto-syncs via `SynchedEntityData`); client-side extends `DownedStateClientHandler.java` with `RenderPlayerEvent.Pre` handler for visual adjustments
2. **Attack prevention** — New `DownedPlayerProtectionHandler.java` cancels `LivingAttackEvent` with HIGHEST priority when victim is downed; server-authoritative with no client modifications needed
3. **Texture/audio replacement** — Zero code changes; drop-in PNG replacements in `assets/dread/textures/entity/` and OGG replacements in `assets/dread/sounds/` leverage existing GeckoLib texture system and Forge sound registration
4. **Cinematic enhancement** — Extend existing `DeathCinematicClientHandler.java` timer from 90 to 120-180 ticks, add camera effects (FOV transition, subtle shake) during tick handler; coordinate with existing blur/vignette effects

**Key architectural patterns:**
- **Event-driven state changes**: Use Forge event bus for all state changes (pose, protection, cinematic) to decouple features from core entity logic
- **Client-server separation**: Server manages authoritative state, client handles rendering/effects to prevent cheating and reduce network traffic
- **Resource-driven content**: Textures/audio as drop-in replacements enables resource pack support and easier iteration without recompilation

### Critical Pitfalls

**Top 5 from PITFALLS.md that directly threaten v1.1 success:**

1. **Client-Server Animation Desync (Crawl Pose)** — Forcing pose on client without packet-based synchronization causes position desync, rubberbanding, and multiplayer glitches where other players see wrong positions. **Prevention:** Use Fabric's networking API to sync pose state changes, track pose in both logical client AND server, send C2S and S2C packets, verify server acknowledgment before applying visual effects. **Test:** Dedicated server with 2+ clients, not just singleplayer integrated server.

2. **Sound Channel Saturation (v1.1 Audio Expansion)** — Adding cinematic and ambient sounds on top of v1.0 exceeds Minecraft's ~28 channel limit, causing critical entity sounds to be dropped. **Prevention:** Extend v1.0's existing sound priority system (Priority 1: jump scares, Priority 2: cinematic audio, Priority 3: entity sounds, Priority 4: ambient), stop looping sounds when switching states, implement sound pooling (max 1 cinematic, max 2 ambient loops). **Test:** Multiple Dread entities + cinematic effects + ambient sounds simultaneously.

3. **GeckoLib Animation Conflicts** — v1.0's GeckoLib entity animations break when player pose override is active, causing frozen animations, stretched limbs, or visual artifacts. **Prevention:** Check GeckoLib animation state before applying pose override, pause/adjust animations during pose-forced states using GeckoLib's event keyframes, test with v1.0 entities present while pose override active. **Test:** Trigger Dread entity spawn while in crawl pose, enable shaders (Complementary/BSL) to amplify conflicts.

4. **Attack Prevention Server-Only Execution** — Canceling attacks server-side without client feedback allows weapon swing animations and sounds to play with no effect, breaking immersion. **Prevention:** Cancel attack at packet level BEFORE damage calculation, send client notification for UI/audio feedback, suppress client-side animations/sounds when blocked, handle both melee AND projectile attacks. **Test:** Enable prevention, spam attack button, verify no sounds/animations play.

5. **Texture UV Mapping Breaks on Model Changes** — UV coordinates designed for standing state don't translate to crawling pose, causing stretched/misaligned textures during pose transitions. **Prevention:** Design UV maps for all animation states upfront (standing, crawling, downed), use consistent UV scale, stick to entity-appropriate resolutions (64x64, 128x128, 256x256 multiples of 16), test in Blockbench across all states before creating final texture. **Apply v1.0 lessons:** No gray textures, no invalid placeholders.

## Implications for Roadmap

Based on research, the optimal phase structure separates zero-code asset work from code integration, sequences by complexity, and enables parallelization where features are independent.

### Phase 1: Real Audio + Dread Texture
**Rationale:** Zero code changes required — both are drop-in file replacements. Starting here delivers immediate visual/audio upgrade and validates asset creation workflows before code complexity. Builds early momentum with tangible horror improvements.

**Delivers:**
- Improved Dread entity texture (PNG replacement in `assets/dread/textures/entity/`)
- Complete audio implementation (OGG Vorbis mono 44.1kHz files in `assets/dread/sounds/`)

**Addresses features:**
- Non-default Dread appearance (table stakes)
- Death cinematic audio (table stakes)
- Dread presence audio (table stakes)
- Ambient soundscape (differentiator)

**Avoids pitfalls:**
- Texture UV mapping breaks (design UVs for all states from start)
- Audio format incompatibility (OGG mono 44.1kHz only)
- Invalid placeholder textures (apply v1.0 lessons: valid dimensions, proper naming)

**Dependencies:** None (asset creation independent of code)

**Research flag:** Standard patterns — texture and audio format requirements are well-documented in Minecraft Wiki and Blockbench guides. Skip phase-specific research.

---

### Phase 2: Attack Prevention
**Rationale:** Simplest code change with highest gameplay value. Single event handler class, straightforward logic, critical for downed state mechanics. Proves event-driven architecture before more complex pose implementation.

**Delivers:**
- New `DownedPlayerProtectionHandler.java` with `LivingAttackEvent` cancellation
- Protection for both melee and projectile attacks
- Client feedback for blocked attacks (no ghost animations/sounds)

**Addresses features:**
- Input blocking during death cinematic (table stakes)

**Avoids pitfalls:**
- Attack prevention server-only execution (cancel at packet level with client feedback)
- Projectile edge case (handle both melee AND projectile events)

**Dependencies:** Existing `DownedPlayersState` (already present in v1.0)

**Research flag:** Standard patterns — Fabric event cancellation is core pattern, extensively documented. Skip phase-specific research.

---

### Phase 3: Crawl Pose
**Rationale:** Moderate complexity requiring both server and client changes. Dependencies on existing systems (DownedPlayersState, DownedStateClientHandler) are now validated. Builds on proven event-driven pattern from Phase 2.

**Delivers:**
- Server-side `DownedPoseHandler.java` for pose management
- Client-side `DownedStateClientHandler.java` extensions for rendering
- Fabric networking sync for multiplayer compatibility

**Addresses features:**
- Prone/crawling pose during downed state (table stakes)

**Avoids pitfalls:**
- Client-server animation desync (implement Fabric networking sync from day one)
- GeckoLib animation conflicts (coordinate with v1.0 entity rendering, test early)
- Pose animation conflicts with other mods (integrate playerAnimator API for compatibility)

**Dependencies:**
- `DownedPlayersState` (v1.0)
- `DownedStateClientHandler` (v1.0, modified)
- GeckoLib entity rendering (v1.0, must not break)

**Research flag:** **Needs phase-specific research** — playerAnimator integration patterns for broad mod compatibility (117M+ downloads, must coordinate with popular animation mods). Research focus: event keyframe coordination with GeckoLib, dedicated server sync patterns.

---

### Phase 4: Intense Cinematic
**Rationale:** Most complex feature requiring camera manipulation with accessibility concerns. Deferred to end allows playtesting/iteration without blocking earlier features. Optionally adds camera shake library (only external dependency decision).

**Delivers:**
- Extended `DeathCinematicClientHandler.java` with camera effects
- FOV transition and subtle camera shake during 4.5-second sequence
- Config options for shake intensity and enable/disable (accessibility)
- Optional fabric-camera-shake integration (if shake fits horror tone)

**Addresses features:**
- Intense death cinematic (enhancement to existing v1.0 feature)

**Avoids pitfalls:**
- Cinematic camera motion sickness (exponential decay, config options, test at 30/60/144 FPS)
- Sound channel saturation (coordinate with Phase 1 audio, respect v1.0 priority system)

**Dependencies:**
- `DeathCinematicClientHandler` (v1.0, extended)
- Phase 1 audio (cinematic sound must not saturate channels)
- Optional: fabric-camera-shake library (evaluate during implementation)

**Research flag:** **Needs phase-specific research** — fabric-camera-shake version compatibility with Fabric 1.21.1 (repository accessible but version verification needed). Accessibility testing with motion-sensitive users (external playtesters). Research focus: camera shake config patterns from Camera Overhaul mod, exponential decay implementations.

---

### Phase 5: Integration Testing & Polish
**Rationale:** All features implemented, final phase validates combined behavior and performance. Tests interactions between features that couldn't be validated during individual phases.

**Delivers:**
- Performance profiling with all features active (5+ entities + cinematic + audio)
- Shader compatibility validation (Complementary, BSL with all features)
- Multiplayer stress testing (dedicated server, 2+ clients)
- v1.0 regression testing (ensure no existing features broke)
- Community playtesting feedback incorporation

**Addresses features:**
- All table stakes validated together
- Differentiators validated for combined horror impact

**Avoids pitfalls:**
- Performance impact from high-res textures (test with multiple entities + shaders)
- Sound channel saturation (verify Phase 1 + Phase 4 audio coexist within limits)
- GeckoLib animation conflicts (verify Phase 3 pose + v1.0 entities work together)
- Volume imbalance (normalize audio relative to vanilla and v1.0 sounds)

**Dependencies:** All prior phases complete

**Research flag:** Standard patterns — integration testing follows established QA practices. Skip phase-specific research.

---

### Phase Ordering Rationale

- **Phase 1 (Assets) first:** Enables parallel asset creation while planning code implementation, delivers early visual/audio wins, validates creation workflows with zero risk
- **Phase 2 (Attack Prevention) before Phase 3 (Crawl Pose):** Proves event-driven architecture with simplest case before tackling client-server sync complexity
- **Phase 3 (Crawl Pose) before Phase 4 (Cinematic):** Validates multiplayer sync and GeckoLib coordination before adding camera manipulation complexity
- **Phase 4 (Cinematic) last:** Most complex, requires playtesting iteration, optional library decision can be deferred, depends on Phase 1 audio being stable

**Parallelization opportunities:**
- Phase 1 assets (texture + audio) can be created simultaneously by separate contributors
- Phase 2 + Phase 3 code can overlap if different developers (attack prevention doesn't depend on crawl pose)
- Phase 4 camera shake library research can happen during Phase 3 implementation

**Dependency chain:**
```
Phase 1 (Assets) ──────┐
                       ├──> Phase 4 (Cinematic) ──> Phase 5 (Integration)
Phase 2 (Attack) ──┐   │
                   ├───┘
Phase 3 (Crawl) ───┘
```

### Research Flags

**Phases needing deeper research during planning:**

- **Phase 3 (Crawl Pose):** playerAnimator integration patterns for broad mod compatibility — research focus: event keyframe coordination with GeckoLib, dedicated server sync patterns, animation blend modes vs replacement modes. **Reason:** 117M+ downloads, must coordinate with popular animation mods; client-server sync is critical path.

- **Phase 4 (Intense Cinematic):** fabric-camera-shake library version compatibility verification and accessibility testing — research focus: compatible versions for Fabric 1.21.1, camera shake config patterns from Camera Overhaul mod, exponential decay implementations, motion-sensitive user testing. **Reason:** Library version compatibility needs verification, accessibility features must be designed in from start.

**Phases with standard patterns (skip research-phase):**

- **Phase 1 (Assets):** Texture and audio format requirements are well-documented in Minecraft Wiki, Blockbench guides, Audacity manual. Asset creation workflows are straightforward with established tools.

- **Phase 2 (Attack Prevention):** Fabric event cancellation is core pattern, extensively documented in Fabric API docs and community examples. Event registration patterns are proven.

- **Phase 5 (Integration Testing):** QA practices follow established integration testing patterns. No novel research required.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | **HIGH** | No changes needed to v1.0 runtime dependencies; Fabric API events verified in official docs for 1.21.5 (backwards compatible with 1.21.1); development tools (Blockbench, Audacity) have official Microsoft documentation |
| Features | **MEDIUM-HIGH** | Horror design principles verified with authoritative sources (Fatal Frame creator, Red Barrels, industry analysis); Dead by Daylight Wiki provides detailed mechanical specifications; artistic interpretation varies but patterns clear |
| Architecture | **HIGH** | v1.0 architecture already handles client-server separation, sound priority system, shader compatibility; integration points are standard Forge patterns (event handlers, resource file replacement); GeckoLib texture system confirmed to support variants |
| Pitfalls | **MEDIUM-HIGH** | Client-server sync patterns from Fabric official docs; animation conflicts from mod compatibility reports and community patterns; audio/texture issues from Minecraft Wiki and v1.0 lessons learned; motion sickness from camera mod best practices |

**Overall confidence:** **HIGH**

Research provides actionable implementation guidance with minimal gaps. The decision to use existing v1.0 stack without new dependencies significantly reduces risk. Most uncertainty centers on aesthetic choices (texture design, audio balancing) which are inherently subjective and require iteration.

### Gaps to Address

**During Phase 3 planning/implementation:**
- **playerAnimator integration specifics:** Research provides library recommendation (117M+ downloads, compatibility-focused) but implementation patterns need detailed investigation. **Handling:** Dedicate spike during Phase 3 to playerAnimator API documentation and example mods using it.
- **GeckoLib event keyframe coordination:** Research identifies need to coordinate animation controllers but specific implementation unclear. **Handling:** Test early prototype with v1.0 entities present, iterate based on actual conflicts observed.

**During Phase 4 planning/implementation:**
- **fabric-camera-shake version compatibility:** Research identifies library but version compatibility with Fabric 1.21.1 needs verification. **Handling:** Evaluate during Phase 4 start; if compatibility issues discovered, fallback to manual camera transformation (Camera Overhaul patterns).
- **Motion sickness thresholds:** Research provides best practices (exponential decay, config options) but optimal intensity levels are subjective. **Handling:** External playtesting with motion-sensitive users during Phase 4; default to moderate intensity, allow user adjustment.

**Asset quality (Phase 1):**
- **Audio quality subjective:** Royalty-free sources identified (Pixabay, Mixkit) but quality varies per file. **Handling:** Import multiple candidates, test in-game, iterate with community feedback post-release if needed.
- **Texture artistic direction:** Cosmic horror principles clear (incomprehensible forms, emissive elements, "off" not gory) but execution subjective. **Handling:** Create multiple iterations in Blockbench, test with shaders enabled, gather feedback from horror game community.

**Performance validation (Phase 5):**
- **Sound channel limits in practice:** Research identifies ~28 channel limit and priority system, but actual saturation threshold with v1.0 + v1.1 sounds needs empirical testing. **Handling:** Profile during Phase 5 integration with audio debug overlay, adjust pooling if needed.

## Sources

### Primary (HIGH confidence)

**Fabric Official Documentation:**
- [Fabric Wiki: Side Tutorial](https://wiki.fabricmc.net/tutorial:side) — Client-server separation patterns
- [Fabric Documentation: Networking](https://docs.fabricmc.net/develop/networking) — Packet-based synchronization for pose state
- [Fabric Documentation: Events](https://docs.fabricmc.net/develop/events) — Event system guide for attack prevention
- [ServerLivingEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html) — Damage prevention events (verified for 1.21.5, backwards compatible)

**Minecraft Official:**
- [Minecraft Wiki: Sounds](https://minecraft.wiki/w/Sounds) — Audio format requirements, sound categories
- [Minecraft Wiki: Textures](https://minecraft.wiki/w/Textures) — Texture format and resolution standards
- [Entity Yarn Docs](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/entity/Entity.html) — Entity pose API documentation

**Development Tools:**
- [Blockbench Official](https://www.blockbench.net/) — Download and official documentation
- [Microsoft Learn: Blockbench](https://learn.microsoft.com/en-us/minecraft/creator/documents/vibrantvisuals/useblockbenchtocreatemodelswithtextures?view=minecraft-bedrock-stable) — Official texture tutorial
- [Audacity Manual: OGG Export](https://manual.audacityteam.org/man/ogg_vorbis_export_options.html) — Export settings for Minecraft compatibility

**GeckoLib:**
- [GeckoLib GitHub](https://github.com/bernie-g/geckolib) — Animation engine documentation
- [GeckoLib Wiki: Custom Entity](https://github.com/bernie-g/geckolib/wiki/Custom-GeckoLib-Entity) — Texture variant system
- [GeckoLib 4.7.3 changelog](https://modrinth.com/mod/geckolib/version/4.7.3) — getTextureResource receives renderer parameter

### Secondary (MEDIUM confidence)

**Horror Game Design Principles:**
- [Fatal Frame creator interview](https://www.gamesradar.com/games/survival-horror/fatal-frames-iconic-camera-exists-to-force-players-to-look-straight-at-something-scary-says-series-creator-we-thought-it-would-really-bring-out-the-scariness-of-the-ghosts/) — Forced perspective creates vulnerability
- [A Lack of Fright: Examining Jump Scare Horror Game Design](https://www.gamedeveloper.com/design/a-lack-of-fright-examining-jump-scare-horror-game-design) — Effective vs cheap jump scares
- [Silence is Scary: Sound Design in Horror Games](https://www.wayline.io/blog/silence-is-scary-sound-design-horror-games) — Silence as active element, layered soundscapes

**Mod Compatibility & Best Practices:**
- [playerAnimator mod](https://modrinth.com/mod/playeranimator) — 117M+ downloads, animation compatibility library
- [Audio Engine Tweaks mod](https://modrinth.com/mod/audio-engine-tweaks) — Sound channel management patterns
- [Camera Overhaul mod](https://modrinth.com/mod/cameraoverhaul) — Cinematic camera best practices with config options
- [Entity Desync Viewer mod](https://modrinth.com/mod/entity-desync-viewer) — Debug tool for client-server position sync validation

**Game Mechanics Research:**
- [Dead by Daylight Wiki: Health States](https://deadbydaylight.fandom.com/wiki/Health_States) — Dying state crawling, 240s bleed-out, recovery mechanics (genre standard)
- [Go Down! Mod](https://www.curseforge.com/minecraft/mc-mods/go-down) — Example of crawl pose forcing in Minecraft

**Asset Sources:**
- [Pixabay Horror SFX](https://pixabay.com/sound-effects/search/horror/) — CC0 royalty-free horror sounds
- [Mixkit Horror SFX](https://mixkit.co/free-sound-effects/horror/) — 31 royalty-free effects, high quality

### Tertiary (LOW confidence, needs validation)

**Community Knowledge:**
- [Entity Texture Features performance impact analysis](https://modern.cansoft.com/time-to-retire-etf-why-entity-texture-features-has-run-its-course/) — Performance concerns with high-resolution entity textures, needs empirical testing
- [fabric-camera-shake GitHub](https://github.com/LoganDark/fabric-camera-shake) — Library source code accessible, version compatibility needs verification
- [Minecraft Forums: Texture resolution FPS impact](https://www.minecraftforum.net/forums/mapping-and-modding-java-edition/resource-packs/resource-pack-discussion/2853251-does-going-lower-than-16x-actually-improve-fps) — Community discussion, needs benchmarking

---
*Research completed: 2026-01-25*
*Ready for roadmap: yes*
