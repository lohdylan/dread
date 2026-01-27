# Phase 12: Audio & Testing - Research

**Researched:** 2026-01-26
**Domain:** Minecraft Fabric mod audio implementation (OGG sound files) and multiplayer stability testing
**Confidence:** HIGH

## Summary

Phase 12 requires adding the missing `grab_impact.ogg` audio file to complete the death cinematic audio polish, then validating multiplayer stability through comprehensive testing of all v1.0-v1.2 features. Research confirms that Fabric has well-established patterns for custom sound integration, and the project already has the infrastructure in place (ModSounds.java with GRAB_IMPACT registered, sounds.json structure, GeckoLib animation with sound keyframe at 0.0s).

The audio creation workflow is straightforward: obtain or create a 1-2 second horror sound (wet/visceral creature impact), convert to mono OGG Vorbis format using Audacity, place in `src/main/resources/assets/dread/sounds/`, and register in `sounds.json`. The existing GeckoLib animation (`death_grab`) already references `dread:grab_impact` at keyframe 0.0, so the sound will play automatically once the file exists.

For multiplayer testing, Fabric's networking model requires careful validation of state synchronization between server and clients, particularly for the downed/revive/death flow which relies on server-authoritative state (DownedPlayersState) and S2C packets. Testing should cover both LAN and dedicated server scenarios with 2+ players.

**Primary recommendation:** Use free royalty-free horror sound libraries (Pixabay, Zapsplat) for source material, process in Audacity to create wet/visceral impact sound (1-2s, mono, OGG Vorbis), add to existing sounds.json, then conduct manual multiplayer testing with documented test scenarios covering all v1.0-v1.2 features.

## Standard Stack

The established tools and libraries for this domain:

### Core

| Library/Tool | Version | Purpose | Why Standard |
|--------------|---------|---------|--------------|
| Audacity | Latest (free) | Audio editing & OGG conversion | Open-source, cross-platform, official Fabric docs recommend it for sound conversion |
| OGG Vorbis | N/A (codec) | Minecraft audio format | Required by Minecraft - all custom sounds must be OGG Vorbis format |
| Fabric API | 0.116.8+1.21.1 | Networking & registry system | Core Fabric mod dependency, provides ServerPlayNetworking |
| GeckoLib | 4.7.1 | Animation system with sound keyframes | Already in use, provides AutoPlayingSoundKeyframeHandler for animation-synced sounds |

### Supporting

| Library/Tool | Version | Purpose | When to Use |
|--------------|---------|---------|-------------|
| Minecraft dedicated server | 1.21.1 | Multiplayer testing environment | For validating server-client synchronization beyond LAN |
| PackTest | Latest | Automated testing framework | Optional - for regression test automation (Phase 12 uses manual testing) |
| Online OGG converters | N/A | Web-based audio conversion | Alternative to Audacity if needed, but less control over quality settings |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Audacity | VLC Media Player | VLC can convert to OGG but lacks audio editing capabilities (pitch, distortion, layering) |
| Free sound libraries | Custom recording/Foley | Time-intensive, requires recording equipment and expertise |
| Manual testing | GameTest framework | GameTest excellent for regression testing but overkill for one-time v1.2 validation; manual testing more appropriate |

**Audio File Requirements:**
- **Format:** OGG Vorbis (lossy compression)
- **Channels:** Mono (required for positional/directional sounds in Minecraft)
- **Quality:** Audacity quality setting 5-7 (balance between file size and quality)
- **Duration:** 1-2 seconds (per design requirements)
- **Sample Rate:** 44100 Hz (CD quality, Audacity default)

## Architecture Patterns

### Recommended Project Structure

```
src/main/resources/assets/dread/
├── sounds/
│   ├── grab_impact.ogg           # New file to add
│   ├── death_sequence.ogg        # Existing
│   ├── ambient_drone_1.ogg       # Existing
│   └── [other existing sounds]
├── sounds.json                    # Register grab_impact here
└── animations/
    └── dread_entity.animation.json  # Already has keyframe: "effect": "dread:grab_impact"
```

### Pattern 1: Custom Sound Registration (Fabric)

**What:** Three-step pattern for adding custom sounds to Fabric mods
**When to use:** Any time adding new audio to a Fabric mod
**Steps:**
1. Create OGG file in `resources/assets/[modid]/sounds/`
2. Register in `resources/assets/[modid]/sounds.json`
3. Create SoundEvent constant in Java code

**Example:**
```json
// sounds.json
{
  "grab_impact": {
    "category": "hostile",
    "sounds": [
      {
        "name": "dread:grab_impact",
        "stream": false
      }
    ]
  }
}
```

```java
// ModSounds.java (already exists in project)
public static final SoundEvent GRAB_IMPACT = registerSound("grab_impact");

private static SoundEvent registerSound(String id) {
    Identifier identifier = Identifier.of(DreadMod.MOD_ID, id);
    return Registry.register(
        Registries.SOUND_EVENT,
        identifier,
        SoundEvent.of(identifier)
    );
}
```

Source: [Fabric Documentation - Creating Custom Sounds](https://docs.fabricmc.net/develop/sounds/custom)

### Pattern 2: GeckoLib Sound Keyframe Integration

**What:** Animation-synchronized sound playback using GeckoLib's AutoPlayingSoundKeyframeHandler
**When to use:** When sounds must play at specific moments during entity animations
**How it works:**
- GeckoLib reads animation JSON sound_effects entries
- AutoPlayingSoundKeyframeHandler triggers sounds at specified keyframe times
- Supports format: `namespace:soundid` or `namespace:soundid|volume|pitch`

**Example:**
```json
// dread_entity.animation.json (already configured in project)
"death_grab": {
  "loop": false,
  "animation_length": 1.8,
  "sound_effects": {
    "0.0": {
      "effect": "dread:grab_impact"
    }
  }
}
```

**Project status:** Death grab animation already has sound keyframe configured. Once OGG file exists and is registered in sounds.json, GeckoLib will automatically play it at animation start (0.0s).

Source: [GeckoLib Wiki - Keyframe Triggers (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Keyframe-Triggers-(Geckolib4))

### Pattern 3: Multiplayer State Synchronization Testing

**What:** Validation that server-authoritative state correctly propagates to all clients
**When to use:** Any Fabric mod with custom entity states, player states, or multiplayer-specific features
**Key principles:**
- **Server is authority:** State changes originate server-side (DownedPlayersState, RevivalProgress)
- **Packets synchronize:** S2C packets notify clients of state changes
- **Validation matters:** Server must validate all C2S packets (distance checks, entity existence)
- **Thread safety:** Packet handlers run on network threads - use `getTaskQueue().execute()` for main thread operations

**Test scenario pattern:**
```
Setup → Action → Validate Client → Validate Server → Validate Other Clients
```

Example for downed/revive flow:
1. **Setup:** Player A and Player B on server, Dread entity spawned
2. **Action:** Dread kills Player A (triggers downed state)
3. **Validate Client A:** Sees downed overlay, crawl pose, camera effects
4. **Validate Server:** DownedPlayersState contains Player A, revival timer active
5. **Validate Client B:** Sees Player A in crawl pose, can interact for revival
6. **Action:** Player B interacts with Player A
7. **Validate both clients:** Revival progress bar renders correctly
8. **Validate Server:** RevivalProgress tracks interaction
9. **Action:** Revival completes
10. **Validate all:** Player A restored to normal, downed state cleared

Source: [Fabric Documentation - Networking](https://docs.fabricmc.net/develop/networking)

### Anti-Patterns to Avoid

- **Stereo sounds for positional audio:** Minecraft requires mono OGG files for directional sounds (like grab_impact played at entity position). Stereo files work but lose directionality.
- **Client-only validation:** Always validate packets server-side. Clients can be modded/manipulated - server is authority.
- **Assuming single-player == no networking:** Fabric networking operates even in single-player (integrated server). All S2C/C2S patterns apply.
- **High-quality OGG for short SFX:** Don't use quality 10 for 1-2 second sound effects. Quality 5-7 is sufficient and reduces mod file size.
- **Testing only LAN:** LAN and dedicated servers have different networking characteristics. Test both if possible.

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Audio format conversion | Custom converter script | Audacity (free, GUI) or FFmpeg (CLI) | Audio codecs are complex; battle-tested tools handle edge cases (sample rates, bit depths, metadata) |
| Animation-synced sound playback | Custom timing system in tick() | GeckoLib AutoPlayingSoundKeyframeHandler | Already integrated, handles network sync, respects animation speed modifiers |
| Multiplayer test automation | Custom test framework | Manual testing with documented scenarios (for one-time validation) | GameTest/PackTest excellent for ongoing regression but overkill for Phase 12's single validation pass |
| OGG quality optimization | Manual bit rate tuning | Audacity quality slider (0-10) | Variable bit rate encoding handles complexity; slider provides good defaults |

**Key insight:** Sound integration has many gotchas (mono vs stereo, streaming vs buffered, volume balancing, format requirements). Use established tools (Audacity for editing, Fabric docs patterns for integration, GeckoLib for animation sync) rather than reinventing any part of the pipeline.

## Common Pitfalls

### Pitfall 1: Forgetting sounds.json Registration

**What goes wrong:** Sound files exist in `sounds/` folder but don't play in-game. No error messages, just silence.

**Why it happens:** Minecraft/Fabric requires explicit registration in `sounds.json`. Files in the sounds folder are not auto-discovered.

**How to avoid:**
- Always update `sounds.json` when adding new OGG files
- Use consistent naming: file `grab_impact.ogg` → sounds.json entry `"grab_impact"` → Java constant `GRAB_IMPACT`
- Rebuild/reload resources after changing sounds.json

**Warning signs:**
- Sound plays in Blockbench animation preview but not in-game
- No errors in logs but sound doesn't trigger
- Other sounds work but new sound is silent

### Pitfall 2: Stereo Audio for Positional Sounds

**What goes wrong:** Grab impact sound doesn't have proper directional audio - sounds equally loud from all directions.

**Why it happens:** Minecraft's positional audio system requires mono (single channel) files. Stereo files play but lose spatial positioning.

**How to avoid:**
- In Audacity: Tracks → Mix → Mix Stereo Down to Mono (before export)
- Export settings: Select "Mono" in channel dropdown
- Verify: Audacity waveform should show single channel, not L/R pair

**Warning signs:**
- Sound plays at same volume regardless of player orientation
- Sound doesn't fade with distance as expected
- Audio feels "flat" compared to other positional sounds

### Pitfall 3: Multiplayer Desync - Client State Assumptions

**What goes wrong:** Feature works perfectly in single-player but desyncs in multiplayer. Examples:
- Player appears downed on their client but standing on other clients
- Revival progress bar shows different values for different players
- Death cinematic plays for one client but not others

**Why it happens:**
- Forgetting to send S2C packet after state change
- Modifying client-side state without server confirmation
- Race conditions between packet arrival and state updates

**How to avoid:**
- **Always use server as source of truth:** State changes happen server-side first (DownedPlayersState)
- **Send packets immediately:** After updating server state, send S2C to all relevant clients (use PlayerLookup.tracking())
- **Never trust client state:** When receiving C2S packets, validate everything (entity exists, distance valid, state allows action)
- **Test with latency:** LAN has ~0ms latency; real servers have 20-100ms. Timing bugs emerge under latency.

**Warning signs:**
- Works in single-player but not multiplayer
- "Player is in an invalid state" errors in logs
- Different clients see different game states
- Entity positions/rotations jump or rubberband

### Pitfall 4: Audio Clipping and Volume Imbalance

**What goes wrong:** New grab_impact sound is too quiet compared to existing sounds, or distorts/clips.

**Why it happens:**
- No reference to existing sound volumes during creation
- Audio waveform exceeds 0 dB (digital clipping)
- Inconsistent mixing between sound files

**How to avoid:**
- **Reference existing sounds:** Listen to `death_sequence.ogg` and other Dread sounds to match intensity
- **Use Audacity normalization:** Effect → Normalize to bring peak to -1.0 dB (prevents clipping)
- **Check waveform:** Should use dynamic range but not exceed +/-1.0
- **Volume in sounds.json:** Can adjust playback volume in sounds.json or animation keyframe (e.g., `dread:grab_impact|0.8|1.0` for 80% volume)

**Warning signs:**
- Sound is barely audible while others are loud
- Crackling/distortion during loud moments
- Waveform shows flat-topped peaks (clipping)

### Pitfall 5: Missing Dedicated Server Testing

**What goes wrong:** Mod works on LAN but crashes or desyncs on dedicated servers.

**Why it happens:**
- LAN uses integrated server (client and server in same JVM)
- Dedicated server is separate process with different class loading
- Client-only classes (rendering, UI) may be accidentally referenced from server code

**How to avoid:**
- **Test on actual dedicated server:** Download Minecraft 1.21.1 server.jar, install Fabric loader and mod
- **Check @Environment annotations:** Server code shouldn't reference client-only classes
- **Review networking:** All UI updates must come from S2C packets, never direct calls
- **Monitor server logs:** Watch for ClassNotFoundException or NoClassDefFoundError

**Warning signs:**
- "Server thread crashed" on dedicated server but not LAN
- ClassNotFoundException in server logs
- Features work until specific actions trigger crashes

## Code Examples

Verified patterns from official sources:

### Creating Horror Sound in Audacity

```
Source: Tutorial - How to Create Monster and Creature Sounds in Audacity
https://hasithappensyet.com/articles/tech/sfx-monsters-and-creatures.html

Recipe for Wet Impact Sound (visceral creature grab):

1. Generate Base Tone
   - Generate → Tone
   - Waveform: Sawtooth (harsh, organic)
   - Frequency: 70-90 Hz (deep, gut-level)
   - Duration: 1.2 seconds

2. Add Distortion (flesh-tearing quality)
   - Effect → Distortion
   - Type: Hard Clipping or Leveller
   - Amount: 30-40%

3. Layer Organic Noise (wet visceral texture)
   - Generate → Noise
   - Type: Brown Noise (deeper, meatier than white)
   - Duration: 1.0 seconds
   - Effect → Low-pass Filter at 800 Hz
   - Mix under tone (about 60% volume of tone)

4. Add Movement (impact + sustain)
   - Effect → Sliding Stretch
   - Initial pitch shift: +3 semitones (impact spike)
   - Final pitch shift: -5 semitones (horror sustain)

5. Shape Envelope
   - Quick fade in (0.05s) for impact
   - Fade out over final 0.5s for lingering horror

6. Normalize and Export
   - Effect → Normalize → Peak to -1.0 dB
   - Tracks → Mix → Mix Stereo Down to Mono
   - File → Export → Export Audio
   - Format: Ogg Vorbis
   - Quality: 5-7
   - Channels: Mono
```

### Professional Horror Layering Technique

```
Source: The Ultimate Horror Sound Guide 2025
https://www.asoundeffect.com/ultimate-guide-horror-sound/

Layering for visceral impact:
1. Start with authentic source (animal vocalization, meat sounds, bone snap Foley)
2. Process beyond recognition (pitch shift, time stretch, distortion)
3. Layer contrasting textures:
   - Organic base (wet meat, flesh sounds)
   - Synthetic layer (synthesized sub-bass rumble)
   - Mechanical artifacts (servo motor noises pitched down)
4. Use strategic silence before/after impact to amplify psychological effect
5. Frequency manipulation in 80-250 Hz range triggers physiological discomfort

Key takeaway: "Authentic source material, then processing it beyond recognition"
```

### Audacity Export Settings for Minecraft

```
Source: Fabric Documentation - Creating Custom Sounds
https://docs.fabricmc.net/develop/sounds/custom

Required settings:
- Format: Ogg Vorbis
- Channels: MONO (critical for positional audio)
- Sample Rate: 44100 Hz (Audacity default)
- Quality: 5 (balance) or 7 (higher quality)

In Audacity:
1. File → Export → Export Audio
2. Format dropdown → "Ogg Vorbis Files"
3. Channels dropdown → "Mono"
4. Quality slider → 5 to 7
5. Save to: src/main/resources/assets/dread/sounds/grab_impact.ogg

Note: "Compression during export helps minimize mod file sizes"
```

### Fabric sounds.json Format

```json
// Source: Fabric Documentation - Creating Custom Sounds
// Location: src/main/resources/assets/dread/sounds.json

{
  "grab_impact": {
    "category": "hostile",
    "sounds": [
      {
        "name": "dread:grab_impact",
        "stream": false
      }
    ]
  }
}

// Key points:
// - "category": "hostile" → Controlled by Hostile Creatures volume slider
// - "stream": false → Sound is buffered entirely (correct for short sounds <5s)
// - Can add "volume" and "pitch" fields for per-file adjustments
// - Supports multiple variants: "sounds": ["dread:grab_impact_1", "dread:grab_impact_2"]
```

### GeckoLib Sound Keyframe Format

```json
// Source: GeckoLib Wiki - Keyframe Triggers (GeckoLib4)
// Already implemented in project at:
// src/main/resources/assets/dread/animations/dread_entity.animation.json

"death_grab": {
  "loop": false,
  "animation_length": 1.8,
  "sound_effects": {
    "0.0": {
      "effect": "dread:grab_impact"
    }
  }
}

// Format options:
// Simple: "effect": "dread:grab_impact"
// With volume/pitch: "effect": "dread:grab_impact|0.8|1.0"
//   |volume|pitch → 0.8 = 80% volume, 1.0 = normal pitch

// AutoPlayingSoundKeyframeHandler automatically parses and plays these
```

### Multiplayer Testing - Server Setup

```bash
# Source: Minecraft Wiki - Tutorial: Setting up a Java Edition server
# MCVersions.net - Minecraft 1.21.1 Server Download
# https://mcversions.net/download/1.21.1

# Download Minecraft 1.21.1 server
wget https://piston-data.mojang.com/v1/objects/59353fb40c36d304f2035d51e7d6e6baa98dc05c/server.jar

# Install Fabric Server Loader
# Download from https://fabricmc.net/use/server/
# Run installer: java -jar fabric-installer-1.0.1.jar server -mcversion 1.21.1

# Add mods to server/mods/ folder:
# - fabric-api-0.116.8+1.21.1.jar
# - geckolib-fabric-4.7.1-1.21.1.jar
# - dread-0.1.0.jar (your mod)

# Start server
java -Xmx2G -jar fabric-server-launch.jar nogui

# Accept EULA (edit eula.txt: eula=true)

# Players connect via Direct Connect: <server-ip>:25565
```

### Multiplayer Testing Checklist

```
Source: Fabric Documentation - Networking
https://docs.fabricmc.net/develop/networking

Manual testing scenarios for Phase 12:

✓ SINGLE-PLAYER REGRESSION (Baseline)
  □ Dread spawns and attacks player
  □ Death grab animation plays with all sounds
  □ Downed state: crawl pose, camera effects, overlay render
  □ Revival timer counts down correctly
  □ Death occurs after timer expires
  □ Death screen displays (no respawn button in hardcore)

✓ MULTIPLAYER - 2 PLAYERS LAN
  □ Both players see Dread entity
  □ Player A gets downed → Player B sees crawl pose
  □ Player B interacts → Both see revival progress bar
  □ Revival completes → Player A restored
  □ Player A left alone → Death occurs for both clients
  □ Death grab cinematic plays for downed player
  □ grab_impact.ogg audible to both players

✓ MULTIPLAYER - 2 PLAYERS DEDICATED SERVER
  □ Repeat all LAN tests
  □ Check server logs for errors
  □ Monitor for desyncs (client sees different state than server)
  □ Test player disconnect/reconnect during downed state
  □ Verify DownedPlayersState persists correctly

✓ MULTIPLAYER - EDGE CASES
  □ Downed player disconnects → State cleaned up on reconnect
  □ Reviving player disconnects mid-revival → Progress cancelled
  □ Both players downed simultaneously → Both enter downed state
  □ Player changes gamemode while downed → State cleaned up

✓ V1.0-V1.2 FEATURE REGRESSION
  □ Spawn probability system works in multiplayer
  □ Configuration file respected (server-side config)
  □ All sounds play correctly (ambient, jumpscare, proximity, death, grab_impact)
  □ Single-player forgiveness system works
  □ Cinematic can be skipped via config

PASS CRITERIA:
- No desync issues (all clients see same state)
- No data loss (state survives disconnect/reconnect)
- No visual glitches (animations, overlays render correctly)
- All audio plays at correct times with correct spatialization
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual SoundEvent playback in code | GeckoLib sound keyframes in animation JSON | GeckoLib 4.x | Animation-synced sounds no longer require manual timing code; handled declaratively |
| Sounds.json with simple string arrays | Rich sound definitions with category, streaming, volume, pitch | Minecraft 1.13+ | Better control over sound behavior and mixing |
| Forge-only GameTest | Fabric client game tests + PackTest | Fabric API recent | Client-side features (rendering, UI) can now be automated tested |
| Registry.register() static calls | Fabric Data Generation for registries | Fabric recent (optional) | Auto-generates registry code, but manual registration still standard for small mods |

**Deprecated/outdated:**
- **Fabric Networking v0 API**: Superseded by Networking v1 API (more ergonomic, more features). Use `ServerPlayNetworking` not legacy `ServerSidePacketRegistry`.
- **MP3 for Minecraft sounds**: Never supported. Only OGG Vorbis works.
- **sounds.json "subtitle" field for non-subtitle mods**: Optional if you're not implementing accessibility subtitles. Dread mod doesn't use subtitles.

## Open Questions

Things that couldn't be fully resolved:

1. **Optimal volume level for grab_impact relative to other Dread sounds**
   - What we know: death_sequence.ogg exists and plays during cinematic
   - What's unclear: Exact volume level (dB) of existing sounds to match intensity
   - Recommendation: Import death_sequence.ogg into Audacity alongside new grab_impact.ogg, compare waveform peak levels, adjust grab_impact to similar or slightly higher peak (it's the climax moment). Test in-game and iterate if needed.

2. **Whether to duck other sounds during grab impact**
   - What we know: DreadSoundManager has priority system (jumpscare blocks other sounds)
   - What's unclear: Should grab_impact trigger same blocking behavior as jumpscare?
   - Recommendation: Start without ducking (grab_impact plays, ambient continues). The cinematic is already visually intense; additional audio ducking may be unnecessary. Test and evaluate - can add ducking in Phase 13 if needed.

3. **Dedicated server hosting for testing**
   - What we know: Testing requires dedicated server validation, not just LAN
   - What's unclear: Whether to self-host on local machine or use remote server
   - Recommendation: Self-host on localhost for initial testing (sufficient to catch server/client separation issues). Can use free trial cloud hosting (Aternos, etc.) if testing with geographically remote players, but not required for Phase 12 validation.

4. **Sound category: Hostile Creatures vs Master**
   - What we know: Context decision marked this as "Claude's discretion"
   - What's unclear: Gameplay impact of category choice
   - Recommendation: Use `"category": "hostile"` (same as dread_jumpscare and dread_death). Consistent with other Dread sounds, controlled by Hostile Creatures slider. Master category is for global sounds; Dread sounds are entity-specific.

## Sources

### Primary (HIGH confidence)

- [Fabric Documentation - Creating Custom Sounds](https://docs.fabricmc.net/develop/sounds/custom) - Official Fabric docs for sound integration
- [Fabric Documentation - Networking](https://docs.fabricmc.net/develop/networking) - Official networking patterns and state sync best practices
- [Audacity Manual - Ogg Vorbis Export Options](https://manual.audacityteam.org/man/ogg_vorbis_export_options.html) - Official Audacity documentation for OGG export
- [GeckoLib Wiki - Keyframe Triggers (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Keyframe-Triggers-(Geckolib4)) - Official GeckoLib documentation for sound keyframes
- [Minecraft Wiki - Tutorial: Setting up a Java Edition server](https://minecraft.wiki/w/Tutorial:Setting_up_a_Java_Edition_server) - Official Minecraft server setup guide

### Secondary (MEDIUM confidence)

- [MCreator - How to convert MP3 or WAV to OGG for Minecraft](https://mcreator.net/wiki/how-convert-mp3-or-wav-file-ogg-minecraft-sounds) - Community wiki, verified against official docs
- [How to Create Monster and Creature Sounds in Audacity](https://hasithappensyet.com/articles/tech/sfx-monsters-and-creatures.html) - Tutorial verified through hands-on examples
- [The Ultimate Horror Sound Guide 2025](https://www.asoundeffect.com/ultimate-guide-horror-sound/) - Professional sound designer insights (Chase Steele, Joe Dzuban)
- [Fabric Documentation - Automated Testing](https://docs.fabricmc.net/develop/automatic-testing) - Official Fabric testing guide
- [MCVersions.net - Minecraft 1.21.1 Server Download](https://mcversions.net/download/1.21.1) - Verified download source for Minecraft servers

### Tertiary (LOW confidence - supplementary context)

- [Pixabay - Free Monster Sound Effects](https://pixabay.com/sound-effects/search/monster/) - Royalty-free sound library, licensing verified
- [Zapsplat - Monster and Creature Sounds](https://www.zapsplat.com/sound-effect-category/monsters-and-creatures/) - Royalty-free sound library (requires attribution for free tier)
- [PackTest - Fabric Mod for Testing](https://modrinth.com/mod/packtest) - Automated testing tool (mentioned but not recommended for Phase 12's manual approach)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Official Fabric docs verified, Audacity widely documented, existing project already uses GeckoLib 4.7.1
- Architecture: HIGH - Patterns extracted from official Fabric/GeckoLib docs, verified against existing project code structure
- Pitfalls: MEDIUM-HIGH - Based on official docs + community reports + general Minecraft modding knowledge; specific desync scenarios not exhaustively tested

**Research date:** 2026-01-26
**Valid until:** ~60 days (Fabric ecosystem stable; audio tools don't change rapidly)

**Project-specific notes:**
- Project already has ModSounds.GRAB_IMPACT registered (line 27, ModSounds.java)
- Project already has death_grab animation with sound keyframe at 0.0s (line 690, dread_entity.animation.json)
- Project uses GeckoLib 4.7.1 (supports AutoPlayingSoundKeyframeHandler)
- Missing piece: actual grab_impact.ogg file in sounds/ folder and sounds.json entry
- Minecraft version: 1.21.1 (per gradle.properties, though context mentions 1.21.4 - verify target version)
