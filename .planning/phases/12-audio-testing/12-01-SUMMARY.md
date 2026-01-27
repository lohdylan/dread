# Plan 12-01 Summary: Add grab_impact.ogg sound

## Deliverables

| Artifact | Status | Commit |
|----------|--------|--------|
| src/main/resources/assets/dread/sounds/grab_impact.ogg | Created | cccf9f3 |
| src/main/resources/assets/dread/sounds.json (grab_impact entry) | Updated | 13f8957 |

## What Was Built

- **grab_impact.ogg**: Horror impact sound for death grab animation
  - Mono OGG Vorbis format (required for directional audio in Minecraft)
  - 48kHz sample rate, ~14KB file size
  - Registered in sounds.json with category "hostile"
  - Triggered automatically by GeckoLib animation keyframe at death_grab start

## Bugs Found & Fixed

During testing, discovered and fixed 4 interconnected singleplayer death bugs:

1. **DeathScreen crash** (NullPointerException)
   - Cause: Client still thought downed when death screen appeared
   - Fix: Send RemoveDownedEffectsS2C before player.kill() (commit 9b78832)

2. **Downed state re-applied after respawn** (300s/NO MERCY wrong defaults)
   - Cause: endCinematic() called applyDownedEffects() with hardcoded defaults
   - Fix: Remove call, rely solely on server packets (commit 9b78832)

3. **Frozen/jiggling state after respawn**
   - Cause: Death cinematic (1.8s) triggered before immediate kill()
   - Fix: Skip cinematic in singleplayer death (commit 9b78832)

4. **Death loop from immediate re-kill**
   - Cause: Dread attacked respawned player immediately
   - Fix: Apply Resistance V for 5 seconds on respawn (commit 9b78832)

## Verification

- [x] grab_impact.ogg exists and plays during death grab
- [x] Sound is audible with proper volume
- [x] Sound has directional quality (mono)
- [x] Singleplayer death flow works: downed -> death -> respawn
- [x] No crash, no frozen state, no death loop

## Decisions Made

- Skip death cinematic in singleplayer (cinematic is for multiplayer spectator transition)
- Add 5-second Resistance V immunity after Dread death respawn
- Client should never independently apply downed effects - always from server packets

## Duration

~20 minutes (including bug investigation and fixes)
