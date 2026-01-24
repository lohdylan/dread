---
phase: 01-foundation-entity
plan: 01
subsystem: entity
tags: [fabric, geckolib, minecraft, entity-registration, gradle]

# Dependency graph
requires:
  - phase: none
    provides: "Initial project setup"
provides:
  - Fabric mod skeleton with Gradle build system
  - GeckoLib 4.7.1 integration for entity animations
  - DreadEntity type registered with Minecraft
  - Entity registration infrastructure (ModEntities)
  - Basic AI goals and attributes configuration
affects: [01-02, 01-03, renderer, animations, behaviors]

# Tech tracking
tech-stack:
  added:
    - Fabric Loader 0.16.14
    - Fabric API 0.116.8+1.21.1
    - GeckoLib 4.7.1
    - Gradle 8.13 with Java 21 toolchain
  patterns:
    - Split source sets (main/client) for proper client/server separation
    - Entity registry pattern with FabricEntityTypeBuilder
    - GeoEntity interface implementation for GeckoLib animations
    - NBT persistence for entity state

key-files:
  created:
    - build.gradle
    - gradle.properties
    - src/main/java/com/dread/DreadMod.java
    - src/main/java/com/dread/registry/ModEntities.java
    - src/main/java/com/dread/entity/DreadEntity.java
    - src/client/java/com/dread/DreadClient.java
    - src/main/resources/fabric.mod.json
    - src/client/resources/fabric.mod.json
  modified: []

key-decisions:
  - "Java 21 toolchain with Gradle 8.13 for compatibility"
  - "Fabric API 0.116.8 (latest available for 1.21.1)"
  - "Split source sets for client/server code separation"
  - "DuplicatesStrategy.EXCLUDE for jar tasks to handle fabric.mod.json duplicates"

patterns-established:
  - "Entity registration in ModEntities.register() called from ModInitializer"
  - "GeoEntity implementation with AnimatableInstanceCache"
  - "Form variant persistence via NBT serialization"
  - "Memory leak prevention in remove() method (MC-260605)"

# Metrics
duration: 14.5min
completed: 2026-01-24
---

# Phase 01 Plan 01: Foundation & Entity Registration Summary

**Fabric mod skeleton with GeckoLib 4.7.1, registered Dread entity type, and basic AI behaviors for hostile mob**

## Performance

- **Duration:** 14.5 min
- **Started:** 2026-01-24T05:27:56Z
- **Completed:** 2026-01-24T05:42:23Z
- **Tasks:** 2
- **Files modified:** 14

## Accomplishments

- Fabric mod project structure with Gradle build system and GeckoLib dependency
- DreadEntity registered and available via `/summon dread:dread_entity` command
- Entity implements GeoEntity interface with animation controller infrastructure
- Basic hostile AI (attack players, wander, swim) and entity attributes configured
- Mod loads successfully in Minecraft 1.21.1 without errors

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Fabric mod project structure with GeckoLib dependency** - `5f894b7` (chore)
2. **Task 2: Implement DreadEntity with GeoEntity interface and registration** - `4567efb` (feat)

## Files Created/Modified

- `build.gradle` - Gradle build configuration with Fabric Loom, GeckoLib dependency, Java 21 toolchain
- `gradle.properties` - Mod version, Minecraft 1.21.1, Fabric API, GeckoLib versions
- `settings.gradle` - Project name and plugin management for Fabric Loom
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.13 wrapper configuration
- `src/main/resources/fabric.mod.json` - Main mod metadata and entrypoints
- `src/client/resources/fabric.mod.json` - Client-side entrypoints
- `src/main/resources/dread.mixins.json` - Mixin configuration (empty placeholder)
- `src/main/java/com/dread/DreadMod.java` - Server-side mod initializer
- `src/main/java/com/dread/registry/ModEntities.java` - Entity type registration
- `src/main/java/com/dread/entity/DreadEntity.java` - Dread entity class with GeoEntity implementation
- `src/client/java/com/dread/DreadClient.java` - Client-side initializer (renderer placeholder)
- `.gitignore` - Exclude build artifacts and IDE files

## Decisions Made

**1. Java 21 toolchain requirement**
- Plan specified Java 21 target compatibility
- System has Java 25 installed, which is incompatible with Gradle 8.x
- Solution: Downloaded portable Java 21 JDK to `.gradle-jdk/` and configured JAVA_HOME for Gradle
- Configured Gradle toolchain to use Java 21 for compilation

**2. Fabric API version correction**
- Plan specified 0.110.5+1.21.1 (not available in Maven)
- Updated to 0.116.8+1.21.1 (latest available for Minecraft 1.21.1)
- Verified compatibility with GeckoLib 4.7.1

**3. Jar duplicate handling**
- Multiple fabric.mod.json files (main and client) caused build failure
- Added `duplicatesStrategy = DuplicatesStrategy.EXCLUDE` to all Jar tasks
- This is standard for Fabric mods with split source sets

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Gradle wrapper generation**
- **Found during:** Task 1 (Fabric mod project structure)
- **Issue:** System gradle not available in PATH, couldn't generate wrapper
- **Fix:** Manually created gradlew scripts and downloaded gradle-wrapper.jar from official repository
- **Files modified:** gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.properties, gradle/wrapper/gradle-wrapper.jar
- **Verification:** `./gradlew --version` succeeds, shows Gradle 8.13
- **Committed in:** 5f894b7 (Task 1 commit)

**2. [Rule 3 - Blocking] Java 25 incompatibility**
- **Found during:** Task 1 (First gradle build)
- **Issue:** Gradle 8.x cannot parse build scripts on Java 25 (class file major version 69 unsupported)
- **Fix:** Downloaded Java 21 JDK to `.gradle-jdk/jdk-21.0.9+10`, configured JAVA_HOME for all Gradle commands, added Java 21 toolchain to build.gradle
- **Files modified:** build.gradle (added toolchain configuration)
- **Verification:** Build succeeds with Java 21 toolchain
- **Committed in:** 5f894b7 (Task 1 commit)

**3. [Rule 3 - Blocking] Fabric API version unavailable**
- **Found during:** Task 1 (Dependency resolution)
- **Issue:** Plan specified 0.110.5+1.21.1 which doesn't exist in Fabric Maven
- **Fix:** Updated to 0.116.8+1.21.1 (latest stable for Minecraft 1.21.1)
- **Files modified:** gradle.properties
- **Verification:** Dependencies resolve successfully, mod loads
- **Committed in:** 5f894b7 (Task 1 commit)

**4. [Rule 3 - Blocking] Duplicate fabric.mod.json in jar**
- **Found during:** Task 1 (Jar creation)
- **Issue:** Both main and client source sets have fabric.mod.json, causing duplicate entry error
- **Fix:** Added `tasks.withType(Jar) { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }`
- **Files modified:** build.gradle
- **Verification:** Jar and sourcesJar tasks succeed
- **Committed in:** 5f894b7 (Task 1 commit)

---

**Total deviations:** 4 auto-fixed (4 blocking)
**Impact on plan:** All auto-fixes were necessary to unblock compilation and build. No scope changes or feature additions. Environment compatibility issues resolved.

## Issues Encountered

**Gradle + Java 25 compatibility:**
- Gradle 8.x doesn't support Java 25 bytecode (class major version 69)
- Solution: Downloaded Java 21 JDK and configured JAVA_HOME for Gradle execution
- Added toolchain configuration to build.gradle to ensure Java 21 target
- This required adding `.gradle-jdk/` to .gitignore

**Fabric API version mismatch:**
- Plan specified version 0.110.5+1.21.1 which doesn't exist
- Checked Fabric Maven repository for available versions
- Updated to 0.116.8+1.21.1 (latest stable)

## User Setup Required

None - no external service configuration required.

**Note for development:** Gradle commands must be run with JAVA_HOME set:
```bash
JAVA_HOME="X:/Vibe Coding/.gradle-jdk/jdk-21.0.9+10" ./gradlew build
```

This is already configured in the project and no user action is needed.

## Next Phase Readiness

**Ready for Phase 01-02 (Model & Renderer):**
- Entity type registered and available
- GeoEntity interface implemented with AnimatableInstanceCache
- Animation controller infrastructure in place
- Entity can be summoned (currently invisible without renderer)

**Ready for Phase 01-03 (Behaviors):**
- Basic AI goals structure established
- Entity attributes configured (health, speed, damage, follow range)
- Form variant system implemented and persisting to NBT

**No blockers identified.**

**Notes:**
- Entity is currently invisible when spawned (expected - renderer in Plan 02)
- `/summon dread:dread_entity` command is available and functional
- Animation controller defined but animations need to be created (Plan 02)
- Form variant is persisted but not yet set based on world day (Plan 03)

---
*Phase: 01-foundation-entity*
*Completed: 2026-01-24*
