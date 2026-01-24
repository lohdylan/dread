# Technology Stack

**Project:** Dread - Minecraft Horror Mod
**Platform:** Fabric 1.21.x
**Researched:** 2026-01-23
**Overall Confidence:** HIGH

## Recommended Stack

### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Minecraft | 1.21.11 | Target platform | Latest stable Fabric-compatible version. Note: 1.21.11 is expected to be the last obfuscated version; next release (26.1) will be unobfuscated |
| Java JDK | 21+ | Development language | Required for Minecraft 1.20.5+. Full JDK needed for compilation and debugging |
| Fabric Loader | 0.18.2 | Mod loader | Flexible platform-independent mod loader. Includes MixinExtras out of the box |
| Fabric API | 0.139.4+1.21.11 | Core modding hooks | Essential hooks and patches for Fabric modding. Provides registry sync, crash report info, and base APIs |
| Gradle | 8.x | Build system | Build automation. Used with Loom plugin |
| Fabric Loom | 1.14-SNAPSHOT | Gradle plugin | Enables mod development and debugging. Handles Yarn mappings automatically |

**Confidence:** HIGH - Verified from official Fabric example mod gradle.properties and documentation

### Entity & Animation
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| GeckoLib | 8.x (latest for 1.21.x) | Entity animation engine | Complex 3D keyframe animations, sound/particle keyframes, concurrent animations. Superior to vanilla animations for horror creature with complex movement patterns. Integrates with Blockbench for animation export. No need to register model layers/mesh definitions like vanilla |
| Fabric Entity API | (in Fabric API) | Entity registration & attributes | Use `FabricEntityTypeBuilder` for entity registration, `FabricDefaultAttributeRegistry` for attributes. Required for custom "Dread" entity |

**Confidence:** HIGH - GeckoLib is industry standard for complex animated entities in mods

### Audio & Sound
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Vanilla Sound System | (built-in) | Sound registration & playback | Use `SoundEvent.createVariableRangeEvent()` with `BuiltInRegistries.SOUND_EVENT`. Supports OGG Vorbis (Mono) format. Handles 3D positional audio natively |
| Auudio [Optional] | Latest | Advanced audio playback | Only if needed for background/menu audio from external sources (URLs/paths). Provides individual volume control. NOT needed for standard in-game sounds |

**Audio Format Requirements:**
- Format: OGG Vorbis
- Channels: Mono (required for distance calculations)
- Location: `resources/assets/[mod-id]/sounds/`
- Registration: `sounds.json` + Java registry

**Confidence:** HIGH - Official Fabric documentation, OGG Vorbis format verified

### Rendering & Screen Effects
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Fabric HUD API | (in Fabric API) | Screen overlays | Use `HudElementRegistry` (replaces deprecated `HudRenderCallback` since 0.116). For downed state blur effect, register with `attachElementBefore()`/`attachElementAfter()`. Provides `GuiGraphics` and `DeltaTracker` |
| Fabric Rendering API | (in Fabric API) | Custom rendering | Base rendering API for material properties, emissive effects, blend modes. Use `RendererAccess.INSTANCE.getRenderer()` |
| Canvas Renderer [Optional] | Latest | Advanced shaders | Shader-based renderer implementing FREX (Fabric Rendering Extensions). Only if need custom post-processing shaders beyond HUD overlays. EARLY ALPHA status |

**For screen blur (downed state):**
- Use HudElementRegistry to render full-screen overlay with blur shader
- Alternative: GuiGraphics drawing with translucent overlay (simpler, good enough for blur effect)
- Canvas only needed if custom shader pipeline required (likely overkill)

**Confidence:** MEDIUM - HudElementRegistry is current standard (HIGH), but blur implementation approach not verified in official docs (MEDIUM)

### Networking & Multiplayer Sync
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Fabric Networking API | (in Fabric API) | Server-client synchronization | Modern payload-based system (1.20.5+ rewrite). Use Java Records implementing `CustomPacketPayload`. Register via `PayloadTypeRegistry.playS2C()` / `playC2S()`. Required for downed/revive state sync |
| Cardinal Components API | 7.3.0+ | Persistent entity data | Attach custom data to entities (downed state, timers). Provides automatic save/sync/tick. Use for entity-attached state. Vanilla data components only work for items in 1.21+ |

**Networking Pattern:**
1. Define payload as Record with `CustomPacketPayload`
2. Register in common initializer with `PayloadTypeRegistry`
3. Send via `ServerPlayNetworking` / `ClientPlayNetworking`
4. Validate on server side to prevent exploits
5. Use `PlayerLookup` to send to relevant players only

**Confidence:** HIGH - Official Fabric documentation, modern API verified

### Data Persistence
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Vanilla Data Components | (built-in 1.20.5+) | Item data storage | For any item-based state (if needed). Replaces NBT for items. Register custom components namespaced |
| Cardinal Components API | 7.3.0+ | Entity/World/Chunk data | For entity state (Dread spawn cooldown, player downed timer, death counter). Auto-saves to disk with Codec. Handles sync automatically |
| PersistentState | (built-in) | World-level data | For global state (days survived, etc). Extend `PersistentState` with Codec for save/load |

**Why not vanilla for entities:** Vanilla data components (1.21+) only work for ItemStacks. Cardinal Components provides unified interface for entities, worlds, chunks, scoreboards with auto-sync/save/tick.

**Confidence:** HIGH - Data component transition verified in official docs and Cardinal Components changelog

### Development Tools
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Yarn Mappings | (via Loom) | Readable code names | Open mappings for Minecraft. Note: Yarn will NOT be available after 1.21.11. Consider migrating to Mojang Mappings for future versions |
| IntelliJ IDEA | Community | IDE | Recommended by Fabric community. Install Minecraft Development plugin |
| Blockbench | Latest | Model & animation | Create entity models and export GeckoLib animations |

**Confidence:** HIGH - Official Fabric recommendations

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Mod Loader | Fabric | Forge/NeoForge | Fabric is lightweight, faster updates, better performance. Horror mod doesn't need Forge's extensive hooks |
| Animation | GeckoLib | Vanilla Animations | Vanilla animations (added 2024.4) lack concurrent animations, sound/particle keyframes, math-based animations needed for complex horror creature |
| Entity Data | Cardinal Components | Manual NBT | Cardinal Components handles sync/save/tick automatically. NBT requires manual serialization and sync packets |
| Audio Library | Vanilla Sound System | Auudio | Vanilla system sufficient for in-game positional audio. Auudio only needed for menu/external audio (not required for horror mod) |
| Rendering | Fabric Rendering API + HudElementRegistry | Canvas Renderer | Canvas is EARLY ALPHA and overkill for HUD overlays. Use only if need custom shader pipeline |
| Networking | Fabric Networking API (1.20.5+) | Legacy Packet System | Modern payload system is type-safe, structured, official standard since 1.20.5 |
| Language | Java | Kotlin (with Fabric Language Kotlin) | Java is standard, better documentation, no extra dependency. Kotlin adds complexity without clear benefit for this mod |

## Not Recommended

| Technology | Why Avoid |
|------------|-----------|
| `HudRenderCallback` | Deprecated since Fabric API 0.116. Use `HudElementRegistry` instead |
| Auudio for in-game sounds | Vanilla sound system handles everything needed. Auudio is for menu/external audio only |
| Manual NBT for entity state | Error-prone, requires manual sync. Cardinal Components automates this |
| Canvas Renderer (for this project) | EARLY ALPHA status, unnecessary complexity for HUD overlays and entity rendering. Use Fabric Rendering API + HudElementRegistry |
| Forge/NeoForge | Different ecosystem, slower development cycle, heavier runtime |

## Installation & Setup

### Project Setup (build.gradle)

```gradle
dependencies {
    minecraft "com.mojang:minecraft:1.21.11"
    mappings "net.fabricmc:yarn:1.21.11+build.x:v2"
    modImplementation "net.fabricmc:fabric-loader:0.18.2"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.139.4+1.21.11"

    // Animation
    modImplementation "software.bernie.geckolib:geckolib-fabric-1.21:8.x.x"

    // Entity & World Data
    modImplementation "dev.onyxstudios.cardinal-components-api:cardinal-components-api:7.3.0"
}
```

### gradle.properties

```properties
minecraft_version=1.21.11
loader_version=0.18.2
fabric_version=0.139.4+1.21.11
loom_version=1.14-SNAPSHOT

org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true
```

### fabric.mod.json

```json
{
  "schemaVersion": 1,
  "id": "dread",
  "version": "1.0.0",
  "name": "Dread",
  "description": "A Minecraft horror mod featuring a terrifying stalker entity",
  "environment": "*",
  "entrypoints": {
    "main": ["com.example.dread.Dread"],
    "client": ["com.example.dread.DreadClient"]
  },
  "depends": {
    "fabricloader": ">=0.18.2",
    "fabric-api": ">=0.139.4+1.21.11",
    "minecraft": "~1.21.11",
    "java": ">=21",
    "geckolib": ">=8.0.0",
    "cardinal-components-api": ">=7.3.0"
  }
}
```

## Version Notes & Future Compatibility

**Critical Version Warning:**
- Minecraft 1.21.11 is expected to be the **last obfuscated version**
- Next version (26.1, expected early 2026) will be **unobfuscated**
- **Yarn mappings will NOT be available after 1.21.11**
- **Action Required:** Plan migration to Mojang Mappings if targeting post-1.21.11 versions

**Migration Path:**
1. Develop initial version on 1.21.11 with Yarn
2. Test thoroughly on current stable version
3. When 26.1 releases, migrate to Mojang Mappings
4. Update dependencies to compatible versions

**GeckoLib Version Compatibility:**
- Verify GeckoLib version supports target Minecraft version
- GeckoLib uses major version per Minecraft version (e.g., GeckoLib 8.x for 1.21.x)
- Check releases on Modrinth/CurseForge before finalizing dependency

## Sources

### Official Documentation (HIGH Confidence)
- [Fabric Development Portal](https://fabricmc.net/develop/)
- [Fabric Wiki - Setup Tutorial](https://wiki.fabricmc.net/tutorial:setup)
- [Fabric Documentation - Entity Attributes](https://docs.fabricmc.net/develop/entities/attributes)
- [Fabric Documentation - Custom Sounds](https://docs.fabricmc.net/develop/sounds/custom)
- [Fabric Documentation - Networking](https://docs.fabricmc.net/develop/networking)
- [Fabric Documentation - HUD Rendering](https://docs.fabricmc.net/develop/rendering/hud)
- [Fabric Documentation - Custom Data Components](https://docs.fabricmc.net/develop/items/custom-data-components)
- [Fabric Example Mod - build.gradle](https://github.com/FabricMC/fabric-example-mod/blob/1.21/build.gradle)
- [Fabric Example Mod - gradle.properties](https://github.com/FabricMC/fabric-example-mod/blob/1.21/gradle.properties)

### Library Documentation (HIGH Confidence)
- [Cardinal Components API - GitHub](https://github.com/Ladysnake/Cardinal-Components-API)
- [GeckoLib Wiki - Entities (GeckoLib4)](https://github.com/bernie-g/geckolib/wiki/Geckolib-Entities-(Geckolib4))
- [Canvas Renderer - GitHub](https://github.com/vram-guild/canvas)
- [FREX - GitHub](https://github.com/grondag/frex)

### Community Resources (MEDIUM Confidence)
- [Fabric 1.21.11 Release Announcement](https://fabricmc.net/2025/12/05/12111.html)
- [CurseForge - Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- [Modrinth - GeckoLib](https://modrinth.com/mod/geckolib)
- [CurseForge - Cardinal Components API](https://www.curseforge.com/minecraft/mc-mods/cardinal-components-api)

### Horror Mod References (MEDIUM Confidence)
- [The Man From The Fog - Fabric](https://www.curseforge.com/minecraft/mc-mods/the-man-from-the-fog-fabric)
- [DISTURBED Horror Mod](https://www.curseforge.com/minecraft/mc-mods/disturbed)
- [Top Minecraft Horror Mods](https://blog.curseforge.com/top-minecraft-horror-mods/)

## Key Findings Summary

1. **Modern Fabric APIs**: 1.20.5+ introduced major rewrites (networking payloads, data components). Ensure you're using 2025 patterns, not pre-1.20.5 tutorials
2. **Data Component Transition**: Items use vanilla data components (1.20.5+), but entities/world/chunks still need Cardinal Components API
3. **HUD Rendering Changed**: `HudRenderCallback` deprecated. Use `HudElementRegistry` for screen overlays
4. **Yarn Sunset**: 1.21.11 is last Yarn-mapped version. Plan Mojang Mappings migration
5. **GeckoLib for Complex Animations**: Vanilla animations insufficient for multi-state horror creature with particle/sound sync
6. **Cardinal Components Essential**: Automatic sync/save/tick for entity state crucial for multiplayer downed/revive mechanics
