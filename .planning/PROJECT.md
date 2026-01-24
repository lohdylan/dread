# Dread

## What This Is

A Minecraft horror mod for Fabric 1.21.x that introduces "Dread" — a Cthulhu-style cosmic horror entity that stalks and kills players with jump scares. Features a revive system for multiplayer and permanent death stakes for intense survival horror gameplay.

## Core Value

The jump scare must be genuinely terrifying — the entity appearance, cinematic kill, and audio must combine to deliver real horror.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Dread entity with Cthulhu-style cosmic horror appearance
- [ ] Jump scare trigger system (appears when player turns around)
- [ ] Random spawn ticker that increases probability as game days progress
- [ ] Mining activity increases spawn chance
- [ ] Unskippable cinematic death sequence
- [ ] Full soundscape (ambient tension, jump scare audio, death sounds)
- [ ] Downed state system (300 seconds, blurred/dark screen)
- [ ] Revive mechanic (crouch near downed player)
- [ ] Permanent death to spectator mode if not revived
- [ ] Single player support (no revive possible = maximum stakes)
- [ ] Multiplayer support (friend revives)

### Out of Scope

- Multiple entity types — focusing on Dread only for v1
- Configurable difficulty settings — ship default experience first
- Other mod integrations — standalone first

## Context

**Platform:** Fabric mod loader for Minecraft 1.21.x

**Horror Design:**
- Entity "Dread" is Cthulhu-inspired cosmic horror
- Appears behind player when they turn around
- Tension builds through ambient audio before potential spawns
- Kill sequence is cinematic and unskippable — player must experience their death

**Spawn Mechanics:**
- Base random chance that ticks periodically
- Probability increases as in-game days pass (escalating dread)
- Mining activity triggers additional checks (vulnerability in enclosed spaces)

**Revival System:**
- Death puts player in "downed" state, not immediate death
- 300 second (5 minute) window for revival
- Another player crouches near downed player to revive
- If timer expires: permanent death, player becomes spectator
- Solo players have no revival option — pure survival horror

## Constraints

- **Platform**: Fabric 1.21.x — must use Fabric API and ecosystem
- **Performance**: Jump scare and cinematic must not cause lag spikes
- **Audio**: Requires custom sound assets for full horror effect

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Fabric over Forge | Modern, lightweight, 1.21.x support | — Pending |
| Single entity focus | Nail the horror before expanding | — Pending |
| 300 second revive window | Long enough for friends to reach, short enough for tension | — Pending |
| Spectator mode for perm death | Player can still watch friends, stays in session | — Pending |

---
*Last updated: 2026-01-23 after initialization*
