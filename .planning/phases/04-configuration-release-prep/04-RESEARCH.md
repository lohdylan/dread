# Phase 4: Configuration & Release Prep - Research

**Researched:** 2026-01-25
**Domain:** Fabric mod configuration, multiplayer testing, shader compatibility
**Confidence:** MEDIUM

## Summary

Configuration for Fabric mods in 2026 has settled around simple JSON/GSON-based approaches for lightweight configs and comprehensive libraries (Cloth Config, YACL) for GUI-driven configuration. Given the project's constraints (config changes require restart, no GUI needed per CONTEXT.md), a minimal GSON-based solution is ideal. GSON is already bundled with Minecraft, avoiding additional dependencies.

Multiplayer testing requires manual dedicated server setup, as Fabric's automated game test framework doesn't support multi-client scenarios. Shader compatibility with Iris/Optifine requires runtime detection using `FabricLoader.isModLoaded()` and graceful fallback when post-processing conflicts arise.

**Primary recommendation:** Use GSON for simple JSON configuration with documented field comments, implement config validation with clamping, detect Iris at runtime for shader fallback, and test multiplayer manually on dedicated server with 2-4 clients.

## Standard Stack

The established libraries/tools for this domain:

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| GSON | Bundled (Minecraft) | JSON serialization/deserialization | Already included in Minecraft, zero dependencies, automatic field mapping |
| FabricLoader API | 0.16.10+ | Mod detection and metadata | Official Fabric API for runtime mod detection (`isModLoaded()`) |
| Fabric API | 0.116.8+ (1.21.1) | Config directory access | `FabricLoader.getInstance().getConfigDir()` for standard config location |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Night Config 4 Fabric | Latest | TOML/JSON support with validation | If TOML format preferred over JSON |
| Cloth Config API | Latest | GUI config screens | When in-game config GUI required (NOT needed per CONTEXT.md) |
| YACL (YetAnotherConfigLib) | 3.3.2+ | Modern builder-based config GUI | Client-side config screens (NOT needed here) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| GSON (JSON) | TOML via Night Config | TOML has better comment support, but adds dependency; JSON simpler for 4-6 config values |
| GSON (JSON) | JSON5 via Jankson | JSON5 allows comments/trailing commas, but GSON simpler and guaranteed available |
| Manual multiplayer testing | Automated game tests | Fabric game tests don't support multi-client scenarios; manual testing required |

**Installation:**
No additional dependencies needed. GSON is bundled with Minecraft. Use existing `FabricLoader` and `Fabric API` dependencies.

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/dread/
├── config/
│   ├── DreadConfig.java           # Config data class with public fields
│   └── DreadConfigLoader.java     # Load/save logic, validation, singleton
└── DreadMod.java                   # Load config in onInitialize()

config/                             # Game/server config directory
└── dread.json                      # Generated config file
```

### Pattern 1: GSON Config with Defaults and Validation
**What:** Config class with public fields and default values, GSON serializes/deserializes automatically
**When to use:** Simple configs (5-20 fields) without nested complexity
**Example:**
```java
// Source: https://www.quiltservertools.net/ServerSideDevDocs/config/gson_config/
public class DreadConfig {
    // Spawn configuration
    public float baseSpawnChancePerSecond = 0.005f; // 0.5% from Phase 2
    public float maxSpawnChancePerMinute = 0.15f;   // 10-20% per minute from Phase 2

    // Damage configuration
    public float dreadAttackDamage = 20.0f; // One-shot kill default (horror experience)

    // Feature toggles
    public boolean modEnabled = true;
    public boolean skipDeathCinematic = false; // Default: cinematic plays
    public boolean disableDownedEffects = false; // Force disable blur/vignette regardless of shaders

    // Simulated comment fields for documentation
    @SerializedName("_comment_spawn")
    public final String comment1 = "baseSpawnChancePerSecond: Probability per second (0.005 = 0.5%). maxSpawnChancePerMinute: Cap on escalated spawn rate.";

    @SerializedName("_comment_damage")
    public final String comment2 = "dreadAttackDamage: Base damage dealt by Dread entity (20.0 = one-shot for 20 HP players).";

    @SerializedName("_comment_features")
    public final String comment3 = "modEnabled: Master toggle (false = all Dread features disabled). skipDeathCinematic: Skip cinematic on death. disableDownedEffects: Force disable screen effects.";
}
```

### Pattern 2: Config Loading with Validation
**What:** Singleton loader that reads config, validates/clamps values, writes defaults if missing
**When to use:** All Fabric mods with configuration
**Example:**
```java
// Source: https://www.quiltservertools.net/ServerSideDevDocs/config/gson_config/
public class DreadConfigLoader {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .disableHtmlEscaping()
        .create();

    private static DreadConfig instance;

    public static DreadConfig load() {
        if (instance != null) return instance;

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("dread.json");

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, DreadConfig.class);
                validate(); // Clamp invalid values
                save(); // Write back clamped values
            } catch (Exception e) {
                DreadMod.LOGGER.error("Failed to load config, using defaults", e);
                instance = new DreadConfig();
                save();
            }
        } else {
            instance = new DreadConfig();
            save(); // Create default config
        }

        return instance;
    }

    private static void validate() {
        // Clamp spawn rates to sane ranges
        instance.baseSpawnChancePerSecond = Math.max(0.0f, Math.min(1.0f, instance.baseSpawnChancePerSecond));
        instance.maxSpawnChancePerMinute = Math.max(0.0f, Math.min(1.0f, instance.maxSpawnChancePerMinute));
        instance.dreadAttackDamage = Math.max(0.0f, Math.min(100.0f, instance.dreadAttackDamage));
    }

    private static void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("dread.json");
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(instance, writer);
        } catch (Exception e) {
            DreadMod.LOGGER.error("Failed to save config", e);
        }
    }
}
```

### Pattern 3: Runtime Mod Detection for Shader Compatibility
**What:** Detect Iris/Optifine at runtime and disable conflicting features
**When to use:** When mod uses post-processing shaders that may conflict with shader mods
**Example:**
```java
// Source: https://maven.fabricmc.net/docs/fabric-loader-0.16.10/net/fabricmc/loader/api/FabricLoader.html
public class ShaderCompatibilityDetector {
    private static boolean irisDetected = false;

    public static void detect() {
        irisDetected = FabricLoader.getInstance().isModLoaded("iris");

        if (irisDetected) {
            DreadMod.LOGGER.info("Iris detected, post-processing fallback available");
        }
    }

    public static boolean shouldDisablePostProcessing() {
        DreadConfig config = DreadConfigLoader.load();

        // Force disable if config says so
        if (config.disableDownedEffects) {
            return true;
        }

        // Graceful fallback: disable blur/vignette if Iris active
        return irisDetected;
    }
}
```

### Pattern 4: Config-Based Feature Toggling
**What:** Check config boolean before executing mod features
**When to use:** Master toggle or per-feature disabling
**Example:**
```java
// In DreadSpawnManager.evaluateSpawnProbability()
DreadConfig config = DreadConfigLoader.load();
if (!config.modEnabled) {
    return; // Skip all spawn logic
}

// Use config values instead of hardcoded constants
float baseChance = config.baseSpawnChancePerSecond;
float maxChance = config.maxSpawnChancePerMinute;
```

### Anti-Patterns to Avoid
- **Editing config after load without save:** Config changes in-memory won't persist unless `save()` called
- **Loading config every tick:** Load once at mod init, cache singleton, only reload on game restart
- **No validation:** Players can manually edit JSON to invalid values; always clamp/validate after load
- **Hardcoded config paths:** Always use `FabricLoader.getInstance().getConfigDir()`, never assume `.minecraft/config`
- **JSON comments via data pollution:** Use dedicated comment fields (`@SerializedName("_comment")`) or switch to TOML if extensive docs needed

## Don't Hand-Roll

Problems that look simple but have existing solutions:

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JSON parsing | Custom parser | GSON (bundled) | GSON handles all JSON edge cases, automatic field mapping, type safety |
| Config GUI screen | Manual widget layout | Cloth Config or YACL | 300M+ downloads, handles all UI complexity, compatibility tested |
| Config file location | `new File("./config/")` | `FabricLoader.getInstance().getConfigDir()` | Handles launcher variations, server vs client, non-standard installs |
| Mod detection | Search filesystem | `FabricLoader.isModLoaded("modid")` | Official API, handles version checks, disabled mods, all edge cases |
| TOML parsing | Manual parser | Night Config 4 Fabric | Supports TOML spec, comment preservation, validation |
| Config sync client/server | Custom packets | Existing libraries handle this | Complex edge cases (client joins mid-game, version mismatches) |

**Key insight:** Configuration seems trivial but has many edge cases (malformed JSON, filesystem permissions, platform differences, concurrent access). Use proven solutions.

## Common Pitfalls

### Pitfall 1: JSON Doesn't Support Comments (Players Lose Context)
**What goes wrong:** Player edits `dread.json`, deletes file to see defaults, or doesn't understand value meanings
**Why it happens:** JSON has no comment syntax; GSON ignores unknown fields
**How to avoid:** Use `@SerializedName("_comment_X")` fields with `final String` to create pseudo-comments that persist in JSON
**Warning signs:** Players asking "what does this value do?" on forums/Discord

### Pitfall 2: Config Loaded Too Late (Features Already Initialized)
**What goes wrong:** Spawn manager registers with hardcoded values before config loads
**Why it happens:** Mod initialization order matters; config must load FIRST in `onInitialize()`
**How to avoid:** Load config as first line in `DreadMod.onInitialize()`, before any feature registration
**Warning signs:** Config changes don't take effect, values always use defaults

### Pitfall 3: No Validation Allows Game-Breaking Values
**What goes wrong:** Player sets `baseSpawnChancePerSecond = 999.0`, Dread spawns every tick, game unplayable
**Why it happens:** GSON deserializes ANY numeric value, no built-in range checking
**How to avoid:** Validate and clamp ALL numeric values after load, use `Math.max(min, Math.min(max, value))`
**Warning signs:** Crash reports with extreme values, players complaining about broken behavior after config edits

### Pitfall 4: Satin Post-Processing Conflicts with Iris Shaders
**What goes wrong:** Both mods try to apply post-processing, game crashes or visual glitches occur
**Why it happens:** Iris replaces entire rendering pipeline; Satin's `ManagedShaderEffect` may conflict
**How to avoid:** Detect Iris with `isModLoaded("iris")`, disable Satin effects if detected OR add config toggle
**Warning signs:** Crash logs mentioning OpenGL errors, shader compilation failures when both mods present

### Pitfall 5: Multiplayer Desync (Server Config ≠ Client Config)
**What goes wrong:** Server has `modEnabled=false`, client has `modEnabled=true`, Dread spawns client-side only (ghost entity)
**Why it happens:** Config is per-instance; server and client read separate files
**How to avoid:** Authority model: server config is authoritative for gameplay (spawn rates, damage), client config only for visuals (skip cinematic)
**Warning signs:** "I see Dread but my friend doesn't", desync bugs in multiplayer

### Pitfall 6: Testing with Only 1 Player (Multiplayer Bugs Missed)
**What goes wrong:** Revive mechanic works in single-player, fails with 2+ players (range checks, target tracking, state sync)
**Why it happens:** Single-player integrated server doesn't expose all multiplayer edge cases
**How to avoid:** Test on dedicated server with 2-4 real clients, verify all network packets, test edge cases (player leaves during revive)
**Warning signs:** Multiplayer-specific bug reports, "works in single-player but not server"

## Code Examples

Verified patterns from official sources:

### GSON Config Setup
```java
// Source: https://www.quiltservertools.net/ServerSideDevDocs/config/gson_config/
private static final Gson GSON = new GsonBuilder()
    .setPrettyPrinting()      // Human-readable formatting
    .serializeNulls()         // Include null values
    .disableHtmlEscaping()    // Don't escape unicode
    .create();

// Load from config directory
Path configFile = FabricLoader.getInstance()
    .getConfigDir()
    .resolve("dread.json");
```

### Mod Detection for Compatibility
```java
// Source: https://maven.fabricmc.net/docs/fabric-loader-0.16.10/net/fabricmc/loader/api/FabricLoader.html
if (FabricLoader.getInstance().isModLoaded("iris")) {
    DreadMod.LOGGER.warn("Iris detected, disabling post-processing effects for compatibility");
    // Disable Satin ManagedShaderEffect usage
}
```

### Config Field Documentation
```java
// Source: https://www.quiltservertools.net/ServerSideDevDocs/config/gson_config/
public class DreadConfig {
    // Actual config field
    public float baseSpawnChancePerSecond = 0.005f;

    // Documentation field (appears in JSON as comment-like key)
    @SerializedName("_comment_baseSpawnChance")
    public final String comment = "Probability per second (0.005 = 0.5%, 0.01 = 1%)";
}
```

### Config Validation and Clamping
```java
// Clamp to valid range after loading
private static void validateConfig(DreadConfig config) {
    config.baseSpawnChancePerSecond = Math.max(0.0f,
        Math.min(1.0f, config.baseSpawnChancePerSecond));

    config.dreadAttackDamage = Math.max(0.0f,
        Math.min(100.0f, config.dreadAttackDamage));

    // Boolean validation: ensure only true/false (GSON handles this)
    // String validation if needed:
    if (!List.of("easy", "normal", "hard").contains(config.difficulty)) {
        config.difficulty = "normal"; // Fallback to default
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Forge Config API | GSON + manual validation | Fabric era (2019+) | Fabric doesn't include config system; lightweight JSON preferred |
| Properties files (.properties) | JSON/TOML | ~2020 | Better nested structure, type safety, tooling support |
| Client-side config GUIs only | Mod Menu + Cloth Config integration | 2021+ | In-game config editing without restart (not applicable to this project) |
| Single config file for all mods | Per-mod config files | Always (Fabric standard) | Avoids conflicts, clear ownership |
| Hot reload via commands | Restart required | Fabric standard | Simplifies implementation, avoids state corruption |

**Deprecated/outdated:**
- **AutoConfig (Cloth Config):** Still works but Cloth Config dev stated "not adding features, don't want to touch it" (maintenance mode)
- **Forge ConfigBuilder:** Forge-specific, not applicable to Fabric
- **`.cfg` files:** Legacy format, replaced by JSON/TOML

## Open Questions

Things that couldn't be fully resolved:

1. **Satin + Iris Compatibility Specifics**
   - What we know: Satin uses OpenGL post-processing via `ManagedShaderEffect`, Iris replaces render pipeline
   - What's unclear: Exact conflict mechanism, whether Iris automatically disables conflicting post-process shaders
   - Recommendation: Test empirically with Iris 1.10.4+ and Satin 1.17.0, implement fallback toggle in config, monitor for OpenGL errors in logs

2. **Multiplayer Config Authority (Server vs Client)**
   - What we know: Server and client have separate config files
   - What's unclear: Best practice for which config takes precedence for which features
   - Recommendation: Server config controls gameplay (spawn rates, damage, mod enabled), client config controls local experience only (skip cinematic, disable effects toggle)

3. **Testing Multiplayer with Automated Framework**
   - What we know: Fabric game tests support server tests, but not multi-client scenarios
   - What's unclear: Any emerging tools or frameworks for automated multiplayer testing in 2026
   - Recommendation: Manual testing required; set up dedicated server with 2-4 clients, script test scenarios

## Sources

### Primary (HIGH confidence)
- [Fabric Loader API 0.16.10](https://maven.fabricmc.net/docs/fabric-loader-0.16.10/net/fabricmc/loader/api/FabricLoader.html) - `isModLoaded()` method, config directory access
- [Quilt Server Tools: GSON Config Tutorial](https://www.quiltservertools.net/ServerSideDevDocs/config/gson_config/) - Complete GSON config implementation patterns
- [Fabric Documentation: Automated Testing](https://docs.fabricmc.net/develop/automatic-testing) - Game test framework capabilities and limitations

### Secondary (MEDIUM confidence)
- [YetAnotherConfigLib GitHub](https://github.com/isXander/YetAnotherConfigLib) - Modern config library features (GUI not needed here)
- [Night Config 4 Fabric on CurseForge](https://www.curseforge.com/minecraft/mc-mods/night-config-4-fabric) - TOML/JSON support with validation
- [Iris Shaders Modrinth](https://modrinth.com/mod/iris) - Shader mod compatibility info
- [Cloth Config API on Modrinth](https://modrinth.com/mod/cloth-config) - Most popular config GUI library (300M+ downloads)
- [Fabric Wiki: fabric.mod.json Specification](https://wiki.fabricmc.net/documentation:fabric_mod_json_spec) - Mod metadata standards
- [DEV.to: JSON vs YAML vs TOML 2026](https://dev.to/jsontoall_tools/json-vs-yaml-vs-toml-which-configuration-format-should-you-use-in-2026-1hlb) - Config format comparison

### Tertiary (LOW confidence)
- Web search results about Satin + Iris compatibility (no official documentation found, requires empirical testing)
- Web search results about multiplayer testing automation (Fabric game tests don't support multi-client per documentation)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - GSON bundled with Minecraft, FabricLoader API well-documented, config patterns verified in Quilt docs
- Architecture: HIGH - GSON config pattern verified in official Quilt Server Tools docs, FabricLoader API confirmed in official javadocs
- Pitfalls: MEDIUM - Common pitfalls derived from web search findings and general modding experience, not all verified in official sources
- Shader compatibility: LOW - No official documentation on Satin + Iris conflicts, empirical testing required

**Research date:** 2026-01-25
**Valid until:** 2026-02-24 (30 days - stable ecosystem)
