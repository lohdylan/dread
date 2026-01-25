# Feature Landscape: Dread Mod v1.1 Polish Enhancements

**Domain:** Horror Game/Mod Polish & Immersion Features
**Researched:** 2026-01-25
**Confidence:** MEDIUM (WebSearch verified with horror design principles)

## Context

This research focuses on five specific enhancement features for the Dread mod v1.1:
1. **Crawl pose** (downed state visual)
2. **Attack prevention** (gameplay mechanic during death cinematic)
3. **Dread texture improvements** (visual horror design)
4. **Intense death cinematic** (camera and pacing)
5. **Real audio implementation** (atmospheric soundscape)

These enhancements build on existing v1.0 features:
- Dread entity with turn-around spawn mechanic
- 4.5-second death cinematic with camera POV switch to Dread
- Downed state (300-second timer, -90% movement speed, blur/vignette)
- Crouch-to-revive mechanic
- Sound event infrastructure (currently silent placeholders)

---

## Table Stakes

Features users expect. Missing these = features feel incomplete or broken.

### 1. Downed State: Prone Pose Visual

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Prone/crawling animation** | Visual feedback for game state change | Medium | Dead by Daylight establishes 240-second bleed-out timer with slow crawl as genre standard. Visual state must match mechanical state. |
| **Distinct from standing pose** | Clear status communication | Low | Player must instantly recognize incapacitated state. Crouch ≠ downed. |
| **Persistent during timer** | State consistency | Low | Pose active for full 300-second duration unless revived. |
| **Movement animation** | Matches -90% speed penalty | Medium | If player can move while downed, crawl animation should reflect slow speed. |

**Design principle:** "Changes in game state should be visually intuitive" - players need immediate visual confirmation that they're in downed state, not just crouching.

### 2. Death Cinematic: Attack Prevention

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Input blocking during cinematic** | Prevents immersion break | Low | Players expect control loss during death sequence. Continuing to attack Dread during own death breaks horror. |
| **Camera control lock** | Cinematic framing control | Low | Forced perspective maintains intended horror experience. |
| **Movement prevention** | Death state finality | Low | Cannot escape after death trigger. |

**Design principle:** "Forced perspective creates vulnerability" - Fatal Frame creator states forcing players to look at something scary "brings out the scariness."

### 3. Creature Texture: Basic Unsettling Elements

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Non-default appearance** | Visual identity | Low | Must not look like vanilla mob. Baseline for "custom entity." |
| **Visible in darkness** | Horror visibility balance | Medium | Too dark = invisible (frustrating). Too bright = not scary. |
| **Consistent with cosmic horror theme** | Thematic coherence | Medium | Should evoke Cthulhu mythos, not generic monster. |

**Design principle:** Cosmic horror emphasizes "the unknowable and incomprehensible more than gore." Texture should hint at otherworldliness.

### 4. Death Cinematic: Basic Camera Work

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **POV switch to Dread** | Already implemented in v1.0 | Low | Maintains existing camera behavior. |
| **4.5-second duration** | Already implemented in v1.0 | Low | Timing already established. |
| **Smooth transition** | Prevents motion sickness | Medium | Jarring camera cuts can break immersion. |

**Design principle:** "Intimate scares" from first-person perspective - Resident Evil 7/Village established first-person POV as intensely scary for modern horror.

### 5. Audio: Essential Sound Layers

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Dread presence audio** | Entity awareness cue | Medium | Breathing, footsteps, or ambient sound indicating Dread is near. |
| **Death cinematic audio** | Synchronized with 4.5s sequence | Medium | Silent death = immersion break. Needs kill sound/scream. |
| **Downed state audio** | Feedback for injured state | Low | Heavy breathing, pain sounds. Dead by Daylight standard. |
| **Ambient environment layer** | Baseline horror atmosphere | Medium | Background drones/ambience. Silence alone not sufficient. |

**Design principle:** "Silence is not merely the absence of sound but an active element" - strategic silence requires intentional ambient layers to punctuate.

---

## Differentiators

Features that set Dread apart. Not expected, but highly valued for horror impact.

### 1. Advanced Downed State Mechanics

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Dynamic crawl speed based on recovery** | Increased tension over time | High | Dead by Daylight's Tenacity perk allows crawl during recovery. Could vary speed 0-50% as recovery progresses. |
| **Blood trail visual** | Environmental storytelling | Medium | Leaves blood particles/blocks behind while crawling. Increases vulnerability (Dread can track). |
| **Proximity-based revival speed** | Cooperative gameplay depth | Medium | Allies closer to downed player = faster revive. Encourages risk/reward. |
| **Downed state sounds heard by Dread** | Strategic vulnerability | Low | Amplifies "dying survivors make sounds killers can hear" design from DBD. |

**Why this stands out:** Most Minecraft mods have binary alive/dead states. Dread's downed state already differentiates - enhancing it compounds uniqueness.

### 2. Cinematic Kill Execution

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Camera angle variation** | Replayability, reduces predictability | High | Resident Evil Requiem (2026) allows perspective switching. Multiple death camera angles prevent stale repetition. |
| **Slow zoom during 4.5s** | Building dread, not just shock | Medium | "Slowly zoom out until respawn" technique from Third Person Death mod. Creates lingering horror. |
| **Screen effects intensification** | Visceral fear escalation | Low | Vignette/blur already present - could pulse or intensify during cinematic. |
| **Final frame freeze** | Memorable horror moment | Low | Hold final frame 0.5s before respawn. Cements image in memory. |

**Why this stands out:** Jump scares are "one-time use" - making death cinematic varied and memorable ensures it stays scary on repeat encounters.

### 3. Eldritch Texture Design

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Emissive/glowing texture elements** | Otherworldly presence | Medium | Entity Texture Features mod enables emissive textures. Eyes/runes glow in darkness. |
| **Animated texture components** | Living, unsettling appearance | High | Vanilla Entity Shader Effects allows animated textures (flowing, pulsing). Non-Euclidean feel. |
| **Impossible geometry hints** | Cosmic horror incomprehensibility | Medium | Texture suggests forms that "shouldn't exist" - recursive patterns, self-similar fractals. |
| **Color outside normal spectrum** | Alien nature | Low | Use saturated purples/greens rare in Minecraft. Signals wrongness. |

**Why this stands out:** Most horror mobs use dark/creepy textures. Cosmic horror's "incomprehensible" design philosophy creates unique visual identity.

### 4. Multi-Layered Audio Atmosphere

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **3-layer soundscape** | Professional horror depth | High | Foreground (Dread sounds), midground (ambient drones), background (subtle reverb/rumbles). |
| **Proximity-based audio intensity** | Escalating tension | Medium | Dread's breathing/presence sounds intensify as it approaches. Creates dread before visual. |
| **Directional audio cues** | Spatial awareness and fear | Medium | Footsteps in water, scraping sounds from specific directions. Player can track but not see. |
| **Post-death audio continuation** | Lingering horror | Low | Ambient sounds continue 2-3s after respawn. Death doesn't "reset" atmosphere. |

**Why this stands out:** Outlast's heavy breathing and ambient density set modern standard. Layered soundscape elevates mod from "has sounds" to "has audio design."

### 5. Psychological Pacing

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Anticipation before jump scare** | Effective vs cheap scare | Medium | "What leads up to the big scream" matters more than volume. Build tension before turn-around spawn. |
| **Tension/release rhythm** | Sustainable horror | High | Jump scares as "spice, not main course." Alternate Dread encounters with ambient dread periods. |
| **Environmental audio tells** | Player-controlled tension | Medium | Distant sounds let player know "something is near" - allows hiding/preparation. Not always instant kill. |

**Why this stands out:** "Good jump scares balance tension buildup and release" - Dread's turn-around mechanic is already a strong setup, enhancing pacing makes it genuinely terrifying vs cheap.

---

## Anti-Features

Features to explicitly NOT build. Common mistakes in horror domain.

### 1. Over-Reliance on Jump Scares

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Frequent guaranteed kills** | "Jump scares used frequently become cheap gimmick that runs out very quickly" | Make some encounters escapable. Dread presence ≠ guaranteed death. Build tension that doesn't always pay off. |
| **Loud = scary** | "Doesn't take much to scare someone like that" - considered bad practice | Use silence, ambient drones, and dynamic range. Loud moments earn impact through contrast. |
| **Predictable spawn patterns** | "When you expect them, you're no longer scared" | Vary spawn conditions, timing, and locations. Unpredictability sustains fear. |

**Design principle:** Amnesia uses "jump scares without cheap tricks, just building tension and releasing it at the right moment."

### 2. Visual Clarity Issues

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Invisible/too-dark entity** | Frustration, not fear | Dread must be visible in darkness (emissive eyes, subtle glow). Horror requires seeing the threat. |
| **Overly complex texture** | Minecraft's block aesthetic limits detail | Suggest complexity with strategic details (glowing eyes, key features) rather than photorealism. |
| **Gore-focused design** | Cosmic horror is about incomprehensibility, not gore | Focus on wrongness - impossible angles, alien forms - not blood/viscera. |

**Design principle:** "Physically, the Eldritch Abomination is only defined by seeming somehow 'off'" - subtlety > shock value.

### 3. Audio Overload

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Constant loud ambience** | Desensitization, exhaustion | Use dynamic range. "Silence as active element" - quiet moments make loud ones impactful. |
| **Every sound layer competing** | Muddy mix, no clarity | Frequency layering: low (rumbles), mid (drones), high (distant echoes). Keep layers complementary. |
| **Music during gameplay** | Reduces player tension control | Reserve music for death cinematic. Ambient soundscape during gameplay lets footsteps/breathing create dread. |

**Design principle:** "Ambient environments contain incredibly dense soundscapes with multiple layers AND randomized one-shot sounds that stick out" - density with clarity.

### 4. Immersion Breaking Mechanics

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Player control during death** | Breaks cinematic impact | Complete input lock. Death is non-negotiable. |
| **Instant respawn** | No consequence weight | Brief black screen or hold final frame. Let death sink in. |
| **Silent cinematic** | Undermines horror investment | Synchronized audio for full 4.5s sequence. Silence here is missed opportunity. |

**Design principle:** "Forced perspective creates vulnerability" - giving player control during death sequence undermines the horror of helplessness.

### 5. Generic Horror Tropes

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Generic monster design** | Could be any horror mob | Lean into cosmic horror identity - Cthulhu-style incomprehensibility, not zombie/skeleton. |
| **Standard hurt sounds** | Minecraft vanilla feel | Custom audio that matches Dread's otherworldly nature. Deep, unsettling, alien. |
| **Immediate threat visibility** | Reduces mystery | Some audio cues (breathing, footsteps) before visual. Let imagination work first. |

**Design principle:** "Lovecraftian horror emphasizes the unknowable and incomprehensible" - Dread should feel fundamentally alien, not just dangerous.

---

## Feature Dependencies

### Dependency Flow

```
CORE v1.0 FEATURES (Already Built)
    |
    ├── Downed State Mechanics
    |   ├── Crawl Pose Animation → depends on movement state
    |   ├── Blood Trail Effects → depends on crawl movement
    |   └── Downed Audio → depends on state trigger
    |
    ├── Death Cinematic (4.5s, POV switch)
    |   ├── Input Blocking → depends on cinematic trigger
    |   ├── Camera Enhancements → depends on existing POV switch
    |   ├── Screen Effects → depends on existing blur/vignette
    |   └── Cinematic Audio → depends on timing system
    |
    ├── Dread Entity
    |   ├── Texture Improvements → independent (texture swap)
    |   ├── Emissive Elements → depends on shader support
    |   └── Animated Texture → depends on ETF/shader mods
    |
    └── Sound Event System (exists, silent)
        ├── Entity Presence Audio → depends on proximity detection
        ├── Ambient Soundscape → independent layer
        ├── Death Cinematic Audio → depends on cinematic trigger
        └── Environmental Audio → depends on world state

ENHANCEMENT SEQUENCING
Phase 1 (Foundation): Audio infrastructure, attack blocking
Phase 2 (Visual Polish): Crawl pose, texture improvements
Phase 3 (Immersion): Layered soundscape, cinematic refinement
```

### Critical Dependencies

1. **Audio System** - All sound features depend on functional audio playback
2. **State Detection** - Crawl pose, audio cues depend on accurate state recognition (downed vs standing vs dead)
3. **Camera Control** - Cinematic enhancements depend on ability to manipulate camera during death sequence
4. **Texture Support** - Advanced texture features depend on mod/shader compatibility (ETF, shaders)

### Independent Features (Can Build in Parallel)

- Attack prevention (input blocking)
- Ambient soundscape base layer
- Basic texture improvements (non-animated)
- Downed state audio

---

## MVP Recommendation for v1.1

### Must-Have (Table Stakes)

Prioritize these to make existing features feel complete:

1. **Attack prevention** - Input blocking during death cinematic (prevents immersion break)
2. **Basic crawl pose** - Visual indication of downed state (state clarity)
3. **Death cinematic audio** - Sound during 4.5s sequence (no silent deaths)
4. **Dread presence audio** - Breathing/footsteps when near (entity awareness)

**Rationale:** These four address the biggest gaps in current implementation. Without them, existing features feel unfinished.

### Should-Have (High-Value Differentiators)

Add these if time permits - highest ROI for horror impact:

5. **Emissive texture elements** - Glowing eyes in darkness (cosmic horror identity)
6. **Ambient soundscape** - Background drones/atmosphere (professional audio feel)
7. **Proximity-based audio intensity** - Sounds intensify as Dread approaches (escalating tension)

**Rationale:** These three transform Dread from "working" to "terrifying." Emissive eyes especially signal otherworldly nature.

### Defer to Post-v1.1

Enhance these after core polish is complete:

- **Animated textures** - Requires shader research, high complexity
- **Multiple camera angles** - Increases scope significantly, replayability benefit not critical for v1.1
- **Blood trail visuals** - Nice-to-have, not essential for horror impact
- **Dynamic crawl speed** - Mechanical complexity, minor gameplay benefit

**Rationale:** These features increase production time disproportionately. Better to nail fundamentals first, then enhance.

---

## Implementation Complexity Assessment

### Low Complexity (Quick Wins)

| Feature | Estimated Effort | Notes |
|---------|-----------------|-------|
| Input blocking during cinematic | Low | Simple input override during death state |
| Basic downed state audio | Low | Play sound on state trigger |
| Basic Dread presence audio | Low | Proximity-triggered sound playback |
| Screen effect intensification | Low | Modify existing blur/vignette values |

### Medium Complexity (Core Features)

| Feature | Estimated Effort | Notes |
|---------|-----------------|-------|
| Crawl pose animation | Medium | Requires pose change on downed state |
| Texture improvements (static) | Medium | Texture art + implementation |
| Ambient soundscape (3 layers) | Medium | Audio mixing + layered playback |
| Proximity-based audio intensity | Medium | Distance calculation + volume scaling |
| Death cinematic audio sync | Medium | Timing coordination with 4.5s sequence |

### High Complexity (Stretch Goals)

| Feature | Estimated Effort | Notes |
|---------|-----------------|-------|
| Emissive/animated textures | High | Depends on shader mod compatibility research |
| Camera angle variation | High | Multiple camera paths + randomization |
| Dynamic soundscape mixing | High | Real-time audio layer management |
| Blood trail particle effects | High | Particle system + state tracking |

---

## Alignment with v1.0 Feature Set

### Building on Existing Strengths

Dread v1.0's proven horror mechanics align with research findings:

| v1.0 Feature | Research Validation | v1.1 Enhancement Opportunity |
|--------------|---------------------|------------------------------|
| **Turn-around jump scare** | Perfect timing + misdirection = effective horror | Add audio buildup before spawn |
| **4.5s death cinematic** | Unskippable death = meaningful consequence | Add synchronized audio, camera refinement |
| **Downed state (300s timer)** | DBD-style incapacitation = cooperative depth | Add crawl pose, blood trail |
| **-90% movement speed** | Player agency prevents frustration | Match visual crawl animation to speed |
| **Blur/vignette effects** | Visual state feedback = clear communication | Intensify during death cinematic |
| **Crouch-to-revive** | Cooperative mechanic = multiplayer value | Add proximity-based speed variation |

### Gaps Addressed by v1.1

| Current Gap | Player Experience Impact | v1.1 Solution |
|-------------|--------------------------|---------------|
| **Silent audio** | Immersion break, feels incomplete | Full audio implementation |
| **No crawl visual** | State unclear, looks like crouch | Prone pose animation |
| **Player can attack during death** | Breaks cinematic, undermines horror | Input blocking |
| **Generic texture** | Doesn't convey cosmic horror | Improved texture with emissive eyes |

---

## Domain-Specific Insights: Horror Polish

### What Makes Polish Features Effective

Research reveals polish features succeed when they:

1. **Reinforce Core Horror Loop** - Audio/visuals that amplify existing scare mechanics, not distract
2. **Communicate Game State Clearly** - Visual/audio feedback prevents confusion (downed = prone pose + breathing)
3. **Respect Player Agency** - Control removed only when narratively justified (death cinematic)
4. **Layer Atmosphere** - Multiple sensory channels (visual texture + audio ambience) compound immersion

### Horror Game Audio Design Principles (Applied to Dread)

| Principle | Application to Dread v1.1 |
|-----------|---------------------------|
| **3-layer soundscape** | Foreground: Dread footsteps/breathing; Midground: ambient drones; Background: subtle rumbles |
| **Proximity intensification** | Breathing volume increases as Dread approaches turn-around spawn point |
| **Directional audio** | Footstep sounds from behind player before visual spawn |
| **Strategic silence** | Quiet ambient moments before turn-around scare amplify impact |

### Texture Design for Cosmic Horror (Applied to Dread)

| Principle | Application to Dread v1.1 |
|-----------|---------------------------|
| **Incomprehensible forms** | Texture hints at non-Euclidean geometry (recursive patterns, impossible angles) |
| **Otherworldly color** | Saturated purples/greens rare in Minecraft = alien presence |
| **Emissive elements** | Glowing eyes/runes visible in darkness = prevents invisibility frustration |
| **"Off" not gory** | Subtle wrongness (too many eyes, limbs at wrong angles) > blood/viscera |

---

## Sources

### Horror Game Design Principles

- [Fatal Frame's iconic camera forces players to "look straight at something scary"](https://www.gamesradar.com/games/survival-horror/fatal-frames-iconic-camera-exists-to-force-players-to-look-straight-at-something-scary-says-series-creator-we-thought-it-would-really-bring-out-the-scariness-of-the-ghosts/) - Camera perspective and forced viewpoints in horror
- [The Cinematography of Horror Games](https://newgameplus.co.uk/2018/05/22/cinematography-of-horror-games/) - Fixed cameras, dramatic framing, visual restriction
- [A Lack of Fright: Examining Jump Scare Horror Game Design](https://www.gamedeveloper.com/design/a-lack-of-fright-examining-jump-scare-horror-game-design) - Effective vs cheap jump scares
- [What horror game creators think about jump scares](https://www.pcgamer.com/what-horror-game-creators-think-about-jump-scares/) - Red Barrels on tension buildup
- [Silence is Scary: The Power of Sound Design in Horror Games](https://www.wayline.io/blog/silence-is-scary-sound-design-horror-games) - Silence as active element
- [Resident Evil: How the Franchise Mastered Horror Through Perspective](https://bloody-disgusting.com/video-games/3626501/resident-evil-franchise-mastered-art-horror-perspective/) - First-person intimacy
- [Why Resident Evil Requiem has different camera angles](https://www.gamedeveloper.com/design/how-capcom-designed-resident-evil-requiem-with-different-camera-angles-in-mind) - Perspective switching in 2026 horror games

### Downed State Mechanics

- [Health States - Dead by Daylight Wiki](https://deadbydaylight.fandom.com/wiki/Health_States) - Dying state crawling, 240s bleed-out, recovery mechanics
- [Tenacity - Dead by Daylight Wiki](https://deadbydaylight.fandom.com/wiki/Tenacity) - Crawl during recovery mechanic
- [Third Person Death mod](https://github.com/cintlep/ThirdPersonDeath) - Bedrock-styled cinematic death camera for Java

### Audio Design

- [Drones and ambient music in horror games](https://gamemusic.net/drones-and-ambient-music-in-horror-games/) - Ambient music vs orchestral, unsettling synthesizers
- [How to Make Horror Game Music and Sound Effects](https://splice.com/blog/horror-video-games-sound-design/) - Sound design as critical element
- [Horror Sound Design's Secrets](https://lbbonline.com/news/horror-sound-designs-secrets-how-audio-experts-craft-bone-chilling-scares) - 3D audio and spatial techniques
- [Halloween Tutorial: Creating a Creepy Soundscape](https://modeaudio.com/magazine/halloween-tutorial-creating-a-creepy-soundscape) - Frequency layering (low/mid/high)
- [Crafting Immersive Soundscapes](https://karanyisounds.com/blogs/production-tips/crafting-immersive-soundscapes-for-video-games-and-motion-pictures) - Foreground/midground/background layers
- [Trapped in Terror: Sound Design with Strafekit](https://www.wayline.io/blog/foley-sound-design-for-claustrophobic-horror-with-strafekit) - Outlast's breathing and ambient density
- [Best Gaming Setup for Horror Games to Hear Every Creeping Footstep](https://steelseries.com/blog/horror-games-setup) - Audio cues in horror games
- [Sound Design in Horror Games: Crafting Audio to Induce Fear](https://horrorchronicles.com/horror-games-and-sound-design/) - Heavy breathing, distorted breathing, footsteps

### Cosmic Horror Creature Design

- [MAKING MONSTROUS: Designing Outsiders](https://www.thecloudcurio.com/post/making-monstrous-3) - Combining disparate elements, bizarre forms
- [Lovecraftian horror - Wikipedia](https://en.wikipedia.org/wiki/Lovecraftian_horror) - Unknowable and incomprehensible emphasis
- [Eldritch Abomination - TV Tropes](https://tvtropes.org/pmwiki/pmwiki.php/Main/EldritchAbomination) - Amorphous, formless, non-Euclidean geometry
- [How to Write Cosmic Horror Stories](https://www.masterclass.com/articles/how-to-write-cosmic-horror-stories) - Incomprehensible greater beings
- [Cosmicism - Wikipedia](https://en.wikipedia.org/wiki/Cosmicism) - Cosmic horror philosophy

### Minecraft Technical Implementation

- [Entity Texture Features mod](https://www.curseforge.com/minecraft/mc-mods/entity-texture-features-fabric) - Random, emissive, blinking textures
- [Vanilla Entity Shader Effects](https://modrinth.com/resourcepack/vanilla-entity-shader-effects) - Animated shader effects, glowing/flowing textures
- [Entity Texture Animation - Bedrock Wiki](https://wiki.bedrock.dev/visuals/animated-entity-texture) - Texture animation techniques
- [Shader – Minecraft Wiki](https://minecraft.wiki/w/Shader) - Core and post-processing shaders

### Horror Atmosphere & Soundscape Layering

- [Sound of Terror! Technicality behind horror movie soundscapes](https://www.letsfame.com/blog/sound-of-terror-technicality-behind-horror-movie-soundscapes) - Layering structure and processing
- [How to add ambience sound effects to a horror video](https://krotos.studio/guide/ambience-horror-scene-davinci-resolve-tutorial) - Ambient sound implementation
- [Creating a Thrilling Audio Experience](https://epicstockmedia.com/designing-horror-sound-effects/) - Horror sound effect design

---

## Confidence Assessment

| Area | Confidence | Reasoning |
|------|-----------|-----------|
| Horror Design Principles | **HIGH** | Multiple authoritative sources (Fatal Frame creator, Red Barrels, industry analysis) agree on core principles |
| Downed State Mechanics | **HIGH** | Dead by Daylight Wiki provides detailed mechanical specifications (240s timer, 95% recovery, crawl speed) |
| Audio Design Best Practices | **MEDIUM** | Strong industry sources but WebSearch-based, not Context7 verified |
| Cosmic Horror Aesthetics | **MEDIUM** | Well-documented design philosophy but artistic interpretation varies |
| Minecraft Implementation | **MEDIUM** | Mods exist (ETF, shader effects) but specific compatibility with Dread mod needs verification |

## Open Questions for Phase-Specific Research

1. **Shader Compatibility** - Does current Dread mod support Entity Texture Features / shader-based effects? Needs technical investigation.
2. **Audio System Limitations** - What audio playback capabilities exist in current implementation? Can it handle layered/proximity-based audio?
3. **Animation System** - What pose/animation control exists for players in downed state? Custom animations possible?
4. **Camera Control Scope** - Can camera manipulation during death cinematic be extended (zoom, angle variation)?

These questions should be addressed during requirements gathering and technical spike phases.
