# Dread

## What This Is

A Minecraft horror mod for Fabric 1.21.x that introduces "Dread" — a Cthulhu-style cosmic horror entity that stalks and kills players with turn-around jump scares, extended death cinematics with camera shake, and cooperative revival mechanics. Players crawl while downed with blood vignette effects. Features permanent death stakes for solo players and a 300-second revival window for multiplayer.

## Core Value

The jump scare must be genuinely terrifying — the entity appearance, cinematic kill, and audio must combine to deliver real horror.

## Requirements

### Validated

- Dread entity with Cthulhu-style cosmic horror appearance — v1.0
- GeckoLib animations for smooth, creepy movement — v1.0
- Light extinguishing (torches go out when Dread spawns) — v1.0
- Multiple Dread forms (BASE, EVOLVED, ELDRITCH) — v1.0
- Turn-around spawn mechanic (appears behind player) — v1.0
- Mining activity increases spawn chance — v1.0
- Spawn probability escalates with game days (capped at day 20) — v1.0
- Fake-out behaviors (3:1 ratio maintains tension) — v1.0
- Ambient tension soundscape — v1.0
- Jump scare audio when Dread appears — v1.0
- Death sequence audio — v1.0
- Sound intensity based on Dread proximity — v1.0
- Unskippable cinematic death sequence (4.5 seconds) — v1.0
- Downed state (300 seconds, blur/vignette vision) — v1.0
- Permanent death to spectator mode if not revived — v1.0
- Crouch-to-revive mechanic (4 blocks, 3 seconds) — v1.0
- Spawn rate configuration — v1.0
- Damage settings — v1.0
- Enable/disable mod toggle — v1.0
- Option to skip death cinematic — v1.0
- Horror textures for all 3 Dread forms with emissive glowmasks — v1.1
- 3-layer horror soundscape (ambient, proximity, jump scare) — v1.1
- Attack prevention (melee + projectile) when downed — v1.1
- Crawl pose with EntityPose.SWIMMING when downed — v1.1
- Blood vignette, drip particles, camera pitch limiting when downed — v1.1
- Extended 1.8s death grab animation with camera shake — v1.1
- Camera shake intensity config (0-100) for accessibility — v1.1
- FPS-adaptive shake with visual compensation — v1.1

### Active

**v2.0 Environmental Horror** (planned)

- [ ] Door slams and lights flicker when Dread is near
- [ ] Blood trail visual when crawling while downed
- [ ] Dynamic crawl speed based on health
- [ ] Animated texture effects (pulsing runes)
- [ ] Multiple camera angles during death cinematic

### Out of Scope

- Sanity/hallucination system — high complexity, not core to jump scare focus
- Real-time chat integration — out of scope for horror mod
- Mobile/Bedrock support — Fabric Java Edition only
- Mod compatibility layer — focus on standalone experience first
- Multiple entity types — focusing on Dread only

## Context

**Current State:** v1.1 shipped (polish & immersion milestone complete)

**Codebase:**
- 3,757 lines of Java across 43+ files
- Fabric 1.21.1 with GeckoLib 4.7.1 and Satin API 1.17.0
- GSON-based configuration with validation

**Tech stack:**
- Fabric API 0.116.8 for Minecraft 1.21.1
- GeckoLib for entity models and animations
- Satin API for post-processing shaders
- Custom network packets for multiplayer sync
- 7 server-side mixins, 4 client-side mixins

**Known issues:**
- Missing grab_impact.ogg for death grab animation sound keyframe (LOW severity)
- Multiplayer testing on dedicated server not yet performed

**Build environment:**
- Requires `export JAVA_HOME="X:/Vibe Coding/jdk-21.0.6+7"` before Gradle commands

## Constraints

- **Platform**: Fabric 1.21.x — must use Fabric API and ecosystem
- **Performance**: Jump scare and cinematic must not cause lag spikes
- **Audio**: Requires custom sound assets for full horror effect

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Fabric over Forge | Modern, lightweight, 1.21.x support | Good |
| Single entity focus | Nail the horror before expanding | Good |
| 300 second revive window | Long enough for friends to reach, short enough for tension | Good |
| Spectator mode for perm death | Player can still watch friends, stays in session | Good |
| GeckoLib for animations | Industry standard, smooth animations | Good |
| Satin API for shaders | Post-processing effects for downed vision | Good |
| Base spawn chance 0.5%/sec | Provides 10-20% chance per minute baseline | Good |
| Day escalation capped at 20 | Prevents infinite scaling, 11x max | Good |
| 3:1 fake-out ratio | Maintains psychological horror unpredictability | Good |
| GSON config with validation | Human-readable, auto-clamps invalid values | Good |
| Shader mod detection | Prevents conflicts with Iris/OptiFine | Good |
| EntityPose.SWIMMING for crawl | Vanilla provides animation, no custom needed | Good |
| Dual blocking (server + client) | Server authoritative, client smooth UX | Good |
| FPS threshold 45 for shake | Below this, shake causes judder | Good |
| Visual compensation pattern | Boost vignette when shake reduced | Good |
| Front-loaded violence timing | 0.15s lunge maximizes terror | Good |

---
*Last updated: 2026-01-26 after v1.1 milestone*
