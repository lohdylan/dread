# Technology Stack: v1.1 Enhancements

**Project:** Dread Horror Mod
**Version:** 1.1 (Polish & Immersion)
**Researched:** 2026-01-25
**Confidence:** HIGH

## Executive Summary

v1.1 requires NO new runtime dependencies beyond existing Fabric 1.21.1 stack. All five enhancements (crawl pose, attack prevention, dread texture, cinematic intensity, real audio) use existing Fabric API capabilities and require only asset creation tools and one optional development library.

**Key Finding:** Fabric API already provides all necessary event callbacks and entity manipulation APIs. Focus shifts from technology acquisition to asset creation workflows.

---

## Runtime Stack (NO CHANGES)

| Component | Version | Status |
|-----------|---------|--------|
| Minecraft | 1.21.1 | Unchanged |
| Fabric Loader | 0.18.2 | Unchanged |
| Fabric API | Latest for 1.21.1 | Unchanged |
| GeckoLib | 4.7.1 | Unchanged |
| Satin API | 1.17.0 | Unchanged |

**Rationale:** All v1.1 features leverage existing capabilities:
- Crawl pose → Entity API (`setPose()`, `EntityPose.SWIMMING`)
- Attack prevention → `ServerLivingEntityEvents.AllowDamage`
- Texture → Standard PNG in existing resource structure
- Camera shake → Client-side transformation (see optional library below)
- Audio → Standard OGG Vorbis in existing sound system

---

## Development Stack Additions

### Required: Asset Creation Tools

#### 1. Blockbench (Entity Textures)
| Property | Value |
|----------|-------|
| **Purpose** | Paint Dread entity texture over gray placeholder |
| **Version** | Latest (January 2026 update with direct 3D painting) |
| **Source** | https://www.blockbench.net |
| **License** | Free, open-source |
| **Platform** | Windows, macOS, web browser |

**Why Blockbench:**
- Industry standard for Minecraft entity textures
- Direct UV painting on 3D model prevents texture misalignment
- Exports PNG in correct format for GeckoLib models
- January 2026 update added enhanced 3D painting capabilities
- Existing Dread model can be imported for texture painting

**Integration:**
1. Import existing Dread GeckoLib model
2. Paint texture in Paint Mode (direct 3D or UV editor)
3. Export texture as PNG to `resources/assets/dread/textures/entity/dread/`
4. No code changes (texture path already configured)

**Alternative Considered:** GIMP or Photoshop for UV painting
**Why Not:** Requires manual UV map export and increases alignment errors. Blockbench's integrated workflow is faster and less error-prone for entity textures.

#### 2. Audacity (Audio Creation/Editing)
| Property | Value |
|----------|-------|
| **Purpose** | Edit/process horror audio, export to OGG Vorbis mono |
| **Version** | 3.x+ (2026) |
| **Source** | https://www.audacityteam.org |
| **License** | GPL, free |
| **Platform** | Windows, macOS, Linux |

**Why Audacity:**
- Native OGG Vorbis export with quality control
- Mono channel export (required for Minecraft positional audio)
- Professional audio processing (reverb, pitch shift, distortion for horror effects)
- Zero cost, reliable, well-documented

**Export Settings for Minecraft:**
- **Format:** Ogg Vorbis
- **Channels:** Mono (critical for directional sound)
- **Quality:** 5-7 (balance between quality and file size)
- **Sample Rate:** 44100 Hz (CD quality, Minecraft standard)

**Why Mono:** Minecraft requires mono audio for positional/directional sounds. Stereo files play at constant volume regardless of distance. Ambient tracks can be stereo, but entity sounds (Dread ambient, chase, death) must be mono.

**Alternatives Considered:**
- **Bfxr/jsfxr:** Too retro/synthetic for horror atmosphere
- **LMMS:** Overkill for simple audio editing, steeper learning curve
- **Online converters:** Cannot create/edit audio, only convert

---

### Optional: Camera Shake Library

#### fabric-camera-shake
| Property | Value |
|----------|-------|
| **Purpose** | Add screen shake during death cinematic for intensity |
| **Version** | Latest compatible with Fabric 1.21.1 |
| **Source** | https://github.com/LoganDark/fabric-camera-shake |
| **License** | Check repository |
| **Integration** | modImplementation + include in build.gradle |

**Why This Library:**
- Multi-mod compatibility (doesn't conflict with other shake mods)
- Simple one-line API: `CameraShakeManager.getInstance().addEvent(new BoomEvent(magnitude, 0, duration))`
- Portal mod support (respects Immersive Portals view bobbing)
- Third-person mode optimized (shake applied before zoom calculation)

**When to Use:**
- Death cinematic climax (when Dread appears on screen)
- Optional: when Dread spawns near player
- Intensity parameter tunable per-event

**Integration Steps:**
1. Add Maven repository: `https://maven.logandark.net`
2. Add to `build.gradle`:
   ```gradle
   modImplementation "net.logandark:camera-shake:<version>"
   include "net.logandark:camera-shake:<version>"
   ```
3. Add to `fabric.mod.json` depends: `"camera-shake": "*"`
4. Call in death cinematic: `CameraShakeManager.getInstance().addEvent(new BoomEvent(.15, 0, 1.0))`

**Why Optional:** Camera shake enhances cinematic but is not critical path. Evaluate during implementation phase if shake feels appropriate for horror tone (some horror benefits from stillness vs chaos).

**Alternative:** Manual camera transformation via client tick event
**Why Library is Better:** Tested compatibility, handles edge cases (portals, third-person), less maintenance burden

---

## Asset Sources

### Horror Audio Libraries (Royalty-Free)

| Source | License | Quality | Notes |
|--------|---------|---------|-------|
| **Pixabay** | CC0, no attribution | High | https://pixabay.com/sound-effects/search/horror/ |
| **Mixkit** | Royalty-free | High | https://mixkit.co/free-sound-effects/horror/ (31 effects) |
| **Freesound** | Varies, check per-file | Variable | https://freesound.org/people/klankbeeld/packs/9250/ (horror ambience pack, requires attribution) |
| **Partners in Rhyme** | Free for use | Medium | https://www.partnersinrhyme.com/soundfx/scary_halloween_sounds.shtml (WAV, convert to OGG) |

**Recommended Workflow:**
1. Source base audio from Pixabay or Mixkit (no attribution hassle)
2. Import to Audacity for processing:
   - Pitch shift down for deeper, more menacing tones
   - Add reverb for spatial horror effect
   - Layer multiple sounds for complex ambient
   - Apply distortion sparingly for unnatural feel
3. Export as mono OGG Vorbis (quality 5-7)
4. Replace placeholder OGGs in `resources/assets/dread/sounds/`

**Avoid:** Epidemic Sound, Storyblocks (subscription required, overkill for mod)

**Sound Design Tips:**
- **Dread ambient:** Slow tonal drones, low frequency (< 200 Hz) for unease
- **Chase theme:** Rising tension, dissonant chords, percussive elements
- **Death scream:** Layered human scream + animalistic growl + reverb
- **Crawl sounds:** Wet dragging, joint cracking, breathing (all subtle)

---

## Fabric API Events (No Library Addition)

### 1. Crawl Pose Forcing

**API:** `Entity.setPose(EntityPose.SWIMMING)`
**Package:** `net.minecraft.entity` (vanilla, yarn-mapped)
**Confidence:** HIGH (confirmed in Fabric API docs, used by "Go Down!" mod)

**Implementation Pattern:**
```java
// In Dread entity tick or AI goal
if (shouldCrawl()) {
    this.setPose(EntityPose.SWIMMING);
}
```

**When Applied:**
- Dread entity hunting phase
- Moving through low spaces (< 1.5 blocks)
- Stalking behavior (low profile)

**No Library Needed:** This is core Entity API, available in all Minecraft versions since 1.14.

### 2. Attack Prevention

**API:** `ServerLivingEntityEvents.ALLOW_DAMAGE`
**Package:** `net.fabricmc.fabric.api.entity.event.v1`
**Confidence:** HIGH (verified in Fabric API 0.119.2+ for 1.21.5, compatible with 1.21.1)

**Event Registration Pattern:**
```java
ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
    if (shouldPreventAttack(entity, source)) {
        return false; // Cancel damage
    }
    return true; // Allow damage
});
```

**Use Cases:**
- Prevent player from attacking Dread during invulnerability phases
- Block damage during cinematic sequences
- Create "unkillable stalker" phases

**Why This Event:** Fires BEFORE armor/mitigation calculations, allows complete damage cancellation. Alternative `AttackEntityCallback` only covers player left-clicks, not all damage sources (projectiles, environmental).

**No Library Needed:** Part of standard Fabric API, already in project dependencies.

---

## Asset Format Requirements

### Textures
- **Format:** PNG (24-bit RGB or 32-bit RGBA for transparency)
- **Resolution:** Match existing Dread model UV map (check in Blockbench)
- **Color Profile:** sRGB
- **Path:** `resources/assets/dread/textures/entity/dread/dread_form*.png`

### Audio
- **Container:** OGG (Ogg Vorbis codec)
- **Channels:** Mono (1 channel) for positional sounds, stereo (2 channels) only for non-directional ambience
- **Sample Rate:** 44100 Hz (CD quality, Minecraft standard)
- **Bit Rate:** Variable (quality 5-7 in Audacity = ~160-192 kbps)
- **Path:** `resources/assets/dread/sounds/*.ogg`
- **Registration:** Already configured in `sounds.json`

---

## What NOT to Add

| Technology | Reason to Avoid |
|------------|-----------------|
| **OptiFine compatibility layer** | Not a Fabric mod, causes conflicts with Satin shaders. Players should use Sodium/Iris instead. |
| **Separate audio engine (FMOD, OpenAL)** | Overkill, Minecraft's sound system is sufficient for mod-scale audio. Adds complexity and compatibility risk. |
| **Substance Painter** | Professional tool for AAA game textures. $150+ for entity texture that Blockbench handles for free. |
| **Camera shake from scratch** | Reinventing wheel, fabric-camera-shake is tested and compatible. Only skip if library conflicts discovered. |
| **AI art generators** | For horror authenticity, hand-painted or photo-manipulated textures feel more intentional than AI artifacts. |

---

## Build Environment (Unchanged)

| Variable | Value |
|----------|-------|
| **JAVA_HOME** | `X:/Vibe Coding/jdk-21.0.6+7` |
| **Gradle Wrapper** | Use existing `gradlew.bat` |
| **IDE** | Any (IntelliJ IDEA, VS Code, Eclipse) |

**No JDK upgrade needed:** Java 21 is current LTS, compatible with all planned features.

---

## Installation Instructions

### For Development

```bash
# No runtime dependencies to add - use existing build.gradle

# Optional: Add camera shake library
# In build.gradle, add to repositories:
maven { url 'https://maven.logandark.net' }

# In dependencies:
modImplementation "net.logandark:camera-shake:<version>"
include "net.logandark:camera-shake:<version>"
```

### For Asset Creation

```bash
# Download Blockbench
# Visit https://www.blockbench.net and download for Windows

# Download Audacity
# Visit https://www.audacityteam.org and download latest 3.x

# Both are portable installs, no system configuration needed
```

---

## Validation Checklist

- [x] All v1.1 features covered (crawl, attack prevention, texture, cinematic, audio)
- [x] No unnecessary dependencies added
- [x] Asset creation tools are free and current (2026)
- [x] Fabric API events verified in official docs
- [x] Audio format requirements match Minecraft standards
- [x] Camera shake library is optional, not required
- [x] Integration steps provided for all additions
- [x] Existing validated stack preserved

---

## Sources

### Fabric API Events
- [Fabric Events Documentation](https://docs.fabricmc.net/develop/events) - Official event system guide
- [AttackEntityCallback Docs](https://fabricmc.docs.concern.i.ng/fabric-events-interaction-v0/fabric-events-interaction-v0/net.fabricmc.fabric.api.event.player/-attack-entity-callback/index.html) - Attack interception API
- [ServerLivingEntityEvents](https://maven.fabricmc.net/docs/fabric-api-0.119.2+1.21.5/net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.html) - Damage prevention events

### Entity Pose
- [Entity Yarn Docs](https://maven.fabricmc.net/docs/yarn-1.21+build.2/net/minecraft/entity/Entity.html) - Entity pose API
- [Go Down! Mod](https://www.curseforge.com/minecraft/mc-mods/go-down) - Example of crawl pose forcing
- [Forge Forums: Force Crawl](https://forums.minecraftforge.net/topic/121716-solved1192-force-crawl-position-on-player-entity-and-how-to-prevent-getting-suffocation-damage/) - Implementation discussion

### Texture Tools
- [Blockbench Official](https://www.blockbench.net/) - Download and docs
- [Blockbench Wiki](https://wiki.bedrock.dev/guide/blockbench) - Modeling and texturing guide
- [Microsoft Learn: Blockbench](https://learn.microsoft.com/en-us/minecraft/creator/documents/vibrantvisuals/useblockbenchtocreatemodelswithtextures?view=minecraft-bedrock-stable) - Official texture tutorial
- [Foro3D: Direct 3D Painting](https://foro3d.com/en/2026/january/blockbench-integrates-direct-texture-painting-on-3d-models.html) - January 2026 update

### Audio Tools
- [Audacity Manual: OGG Export](https://manual.audacityteam.org/man/ogg_vorbis_export_options.html) - Export settings
- [Audacity Official](https://www.audacityteam.org) - Download
- [MCreator: Convert to OGG](https://mcreator.net/wiki/how-convert-mp3-or-wav-file-ogg-minecraft-sounds) - Minecraft audio format guide
- [Minecraft Wiki: sounds.json](https://minecraft.wiki/w/Sounds.json) - Sound event system

### Audio Sources
- [Pixabay Horror SFX](https://pixabay.com/sound-effects/search/horror/) - CC0 horror sounds
- [Mixkit Horror SFX](https://mixkit.co/free-sound-effects/horror/) - 31 royalty-free effects
- [Freesound: Horror Ambience](https://freesound.org/people/klankbeeld/packs/9250/) - Attribution required
- [Partners in Rhyme](https://www.partnersinrhyme.com/soundfx/scary_halloween_sounds.shtml) - Free scary sounds

### Camera Shake
- [fabric-camera-shake GitHub](https://github.com/LoganDark/fabric-camera-shake) - Library source
- [Camera Overhaul Mod](https://modrinth.com/mod/cameraoverhaul) - Alternative example (full mod, not library)
- [Subtly Camera Shake](https://www.curseforge.com/minecraft/mc-mods/subtly-camera-shake) - Another implementation reference

---

## Confidence Assessment

| Area | Level | Rationale |
|------|-------|-----------|
| **Runtime Stack** | HIGH | No changes needed, existing dependencies sufficient |
| **Fabric API Events** | HIGH | Verified in official docs for 1.21.5 (backwards compatible with 1.21.1) |
| **Entity Pose** | HIGH | Core vanilla API, confirmed in multiple mod examples |
| **Blockbench** | HIGH | Official Microsoft documentation, industry standard |
| **Audacity** | HIGH | Verified export settings in official manual |
| **Audio Sources** | MEDIUM | Licenses verified, but quality subjective until tested |
| **Camera Shake Library** | MEDIUM | GitHub repository accessible, but version compatibility needs verification during implementation |

---

## Next Steps for Roadmap

Based on stack research, recommended phase structure:

1. **Phase 1: Crawl Pose** - Single API call, lowest risk
2. **Phase 2: Attack Prevention** - Event registration, straightforward logic
3. **Phase 3: Real Audio** - Parallel asset creation + integration testing
4. **Phase 4: Dread Texture** - Blockbench workflow, parallel to audio
5. **Phase 5: Cinematic Intensity** - Camera shake (optional library), requires playtesting

**Dependency notes:**
- Audio and texture are independent, can parallelize
- Camera shake requires death cinematic testing, should come last
- Crawl and attack prevention have no interdependencies

**Research flags:**
- Camera shake library version compatibility (check during Phase 5)
- Audio quality subjective (may need iteration with community feedback)
