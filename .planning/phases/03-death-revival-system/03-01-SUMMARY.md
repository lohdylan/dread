---
phase: 03-death-revival-system
plan: 01
subsystem: networking
tags: [satin, shaders, networking, packets, fabric-api, death-mechanic]

# Dependency graph
requires:
  - phase: 02-ai-horror-experience
    provides: Sound registration pattern and ModSounds infrastructure
  - phase: 01-foundation-entity
    provides: Mod initialization pattern and DreadMod.onInitialize()
provides:
  - Satin API integrated for post-processing shader effects
  - Network packet infrastructure for death/revival synchronization
  - DREAD_DEATH sound event for death sequence audio
  - Four S2C packet types (cinematic trigger, downed state, revival progress, effect removal)
affects: [03-02, 03-03, 03-04]

# Tech tracking
tech-stack:
  added: [org.ladysnake:satin:1.17.0, Ladysnake maven repository]
  patterns: [Custom PacketCodec for UUID serialization, Server-to-Client packet registration via PayloadTypeRegistry]

key-files:
  created:
    - src/main/java/com/dread/network/DreadNetworking.java
    - src/main/java/com/dread/network/packets/CinematicTriggerS2C.java
    - src/main/java/com/dread/network/packets/DownedStateUpdateS2C.java
    - src/main/java/com/dread/network/packets/RevivalProgressS2C.java
    - src/main/java/com/dread/network/packets/RemoveDownedEffectsS2C.java
    - src/main/resources/assets/dread/sounds/dread_death.ogg
  modified:
    - build.gradle
    - src/main/java/com/dread/sound/ModSounds.java
    - src/main/resources/assets/dread/sounds.json
    - src/main/java/com/dread/DreadMod.java

key-decisions:
  - "Satin API from Ladysnake maven (org.ladysnake:satin:1.17.0) instead of Modrinth maven due to repository availability"
  - "Custom UUID codec for RevivalProgressS2C using ByteBuf read/writeLong for compatibility with Minecraft 1.21 PacketCodec API"
  - "Placeholder death sound (54 byte OGG) following existing sound file pattern from Phase 2"
  - "Network packets registered in DreadNetworking hub class for centralized management"

patterns-established:
  - "Pattern 1: Network packets as record types implementing CustomPayload with static ID and CODEC fields"
  - "Pattern 2: PayloadTypeRegistry.playS2C() for Server-to-Client packet registration during mod initialization"
  - "Pattern 3: Custom PacketCodec implementations for types not provided by PacketCodecs utility"

# Metrics
duration: 9min
completed: 2026-01-24
---

# Phase 3 Plan 1: Death System Foundation Summary

**Satin API integrated for shaders, network packets defined for death cinematics and revival synchronization, DREAD_DEATH sound registered**

## Performance

- **Duration:** 9 min
- **Started:** 2026-01-25T03:55:31Z
- **Completed:** 2026-01-25T04:04:16Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments
- Satin API dependency added and resolved successfully from Ladysnake maven
- DREAD_DEATH sound event registered with placeholder audio file
- Four S2C packet types defined for complete death/revival communication
- Network packet registration integrated into mod initialization

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Satin API dependency and death sound registration** - `81fdf34` (chore)
2. **Task 2: Create network packet definitions for death/revival sync** - `b00e199` (feat)

## Files Created/Modified
- `build.gradle` - Added Ladysnake maven repository and Satin API dependency (org.ladysnake:satin:1.17.0)
- `src/main/java/com/dread/sound/ModSounds.java` - Added DREAD_DEATH sound event registration
- `src/main/resources/assets/dread/sounds.json` - Added dread_death entry with hostile category and non-streaming config
- `src/main/resources/assets/dread/sounds/dread_death.ogg` - Created placeholder audio file (54 bytes)
- `src/main/java/com/dread/network/DreadNetworking.java` - Central hub for packet registration with PayloadTypeRegistry
- `src/main/java/com/dread/network/packets/CinematicTriggerS2C.java` - Triggers death cinematic on client (int dreadEntityId, BlockPos deathPos)
- `src/main/java/com/dread/network/packets/DownedStateUpdateS2C.java` - Syncs downed state to client (boolean isDowned, int remainingSeconds)
- `src/main/java/com/dread/network/packets/RevivalProgressS2C.java` - Shows revival progress bar (UUID downedPlayerUUID, boolean active, float progress)
- `src/main/java/com/dread/network/packets/RemoveDownedEffectsS2C.java` - Clears shaders on revival/respawn (no data)
- `src/main/java/com/dread/DreadMod.java` - Added DreadNetworking.registerPackets() call in onInitialize()

## Decisions Made
- **Satin dependency resolution:** Plan specified `maven.modrinth:satin:mc1.21-1.17.0` but correct coordinates are `org.ladysnake:satin:1.17.0` from Ladysnake's maven repository at `https://maven.ladysnake.org/releases`. This was discovered through iterative dependency resolution attempts.
- **UUID codec implementation:** PacketCodecs.UUID doesn't exist in Minecraft 1.21 API. Implemented custom PacketCodec<ByteBuf, UUID> using ByteBuf.readLong/writeLong for mostSigBits and leastSigBits to serialize UUIDs for RevivalProgressS2C packet.
- **Placeholder audio file:** Used existing 54-byte OGG placeholder pattern from Phase 2 sounds instead of generating 5-second silence (ffmpeg/python not available on system). Placeholder will be replaced with actual death audio asset in later phase.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Corrected Satin API dependency coordinates**
- **Found during:** Task 1 (Satin dependency addition)
- **Issue:** Plan specified `maven.modrinth:satin:mc1.21-1.17.0` which couldn't be resolved. Attempted multiple coordinate formats: `maven.modrinth:satin:1.17.0+1.21`, `maven.modrinth:satin:1.17.0-1.21`, `ladysnake:satin:1.17.0-1.21`, `io.github.ladysnake:satin:1.17.0-1.21` - all failed to resolve.
- **Fix:** Added Ladysnake maven repository at `https://maven.ladysnake.org/releases` and used correct coordinates `org.ladysnake:satin:1.17.0`. This is the official Satin dependency configuration from Ladysnake's documentation.
- **Files modified:** build.gradle
- **Verification:** Build succeeded with Satin dependency resolving correctly (52 mods remapped including Satin)
- **Committed in:** 81fdf34 (Task 1 commit)

**2. [Rule 3 - Blocking] Implemented custom UUID codec for packet serialization**
- **Found during:** Task 2 (RevivalProgressS2C packet compilation)
- **Issue:** PacketCodecs.UUID doesn't exist in Minecraft 1.21's PacketCodecs utility class. Compilation failed with "cannot find symbol: variable UUID location: interface PacketCodecs"
- **Fix:** Implemented custom PacketCodec<ByteBuf, UUID> using ByteBuf's readLong/writeLong methods to serialize UUID's mostSigBits and leastSigBits (2 longs = 16 bytes, standard UUID serialization)
- **Files modified:** src/main/java/com/dread/network/packets/RevivalProgressS2C.java
- **Verification:** Build succeeded, packet compiles correctly
- **Committed in:** b00e199 (Task 2 commit)

**3. [Rule 3 - Blocking] Used existing placeholder audio instead of generating new file**
- **Found during:** Task 1 (placeholder audio generation)
- **Issue:** Plan specified using ffmpeg to generate 5-second silence OGG. ffmpeg not available on system. Python also not available for alternative generation approach.
- **Fix:** Copied existing placeholder sound file (dread_ambient.ogg, 54 bytes) to dread_death.ogg following the same pattern used in Phase 2. All Phase 2 sound files are identical 54-byte OGG placeholders.
- **Files modified:** src/main/resources/assets/dread/sounds/dread_death.ogg (created)
- **Verification:** File exists, build succeeds, follows existing placeholder pattern
- **Committed in:** 81fdf34 (Task 1 commit)

---

**Total deviations:** 3 auto-fixed (3 blocking issues)
**Impact on plan:** All auto-fixes were blocking issues preventing task completion. Dependency coordinates and UUID codec fixes required for compilation. Placeholder audio approach maintains consistency with existing Phase 2 pattern. No scope changes or feature additions.

## Issues Encountered
- **Satin dependency resolution:** Multiple coordinate formats attempted before finding correct Ladysnake maven configuration. Plan assumed Modrinth maven hosting but Satin is distributed via Ladysnake's own maven repository.
- **UUID serialization API:** Minecraft 1.21's PacketCodecs doesn't provide UUID codec (may be added in later versions). Custom implementation required for network packet serialization.
- **Build environment:** ffmpeg and python not available for audio file generation. Reused existing placeholder pattern instead.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for Phase 3 Plan 2 (Death Event Handling):**
- Network packet infrastructure complete and registered
- Satin API available for shader effects in future plans
- DREAD_DEATH sound event ready for use in death sequence
- Custom packet codec pattern established for complex types

**Technical foundation established:**
- CinematicTriggerS2C ready to trigger client-side death camera animation
- DownedStateUpdateS2C ready to sync downed state timer to client
- RevivalProgressS2C ready for real-time revival progress rendering
- RemoveDownedEffectsS2C ready to clean up visual effects on revival/respawn

**No blockers or concerns.**

Client-side packet receivers will be implemented in Plan 03-04 when shader effects and cinematic rendering are added. Server-side packet sending will be implemented in Plan 03-02 (death event handling) and Plan 03-03 (revival mechanics).

---
*Phase: 03-death-revival-system*
*Completed: 2026-01-24*
