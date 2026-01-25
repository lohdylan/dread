# Dread

## What This Is

A Minecraft horror mod for Fabric 1.21.x that introduces "Dread" — a Cthulhu-style cosmic horror entity that stalks and kills players with turn-around jump scares, cinematic death sequences, and cooperative revival mechanics. Features permanent death stakes for solo players and a 300-second revival window for multiplayer.

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

### Active

**v1.1 Polish & Immersion**

- [ ] Crawl pose when downed — player model in swimming/crawling position
- [ ] Attack prevention while downed — disable player attacks in downed state
- [ ] Scary dread texture — replace gray placeholder with horror skin
- [ ] Intense cinematic kill — longer grab animation with camera effects
- [ ] Real horror audio — replace silent placeholder OGGs with creepy sounds

### Out of Scope

- Sanity/hallucination system — high complexity, not core to jump scare focus
- Real-time chat integration — out of scope for horror mod
- Mobile/Bedrock support — Fabric Java Edition only
- Mod compatibility layer — focus on standalone experience first
- Multiple entity types — focusing on Dread only

## Context

**Current State:** v1.1 in progress (polish & immersion milestone)

**Codebase:**
- 2,953 lines of Java across 32 files
- Fabric 1.21.1 with GeckoLib 4.7.1 and Satin API 1.17.0
- GSON-based configuration with validation

**Tech stack:**
- Fabric API 0.116.8 for Minecraft 1.21.1
- GeckoLib for entity models and animations
- Satin API for post-processing shaders
- Custom network packets for multiplayer sync

**Known issues:**
- Placeholder audio files (5 OGG files are 54-byte silent placeholders)
- Placeholder textures functional but need proper art assets
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

---
*Last updated: 2026-01-25 after starting v1.1 milestone*
