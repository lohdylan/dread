---
phase: 03-death-revival-system
plan: 04
subsystem: client-rendering
tags: [satin, shaders, opengl, hud, ui, post-processing]

# Dependency graph
requires:
  - phase: 03-01
    provides: Satin API integration, network packets (DownedStateUpdateS2C, RemoveDownedEffectsS2C)
  - phase: 03-02
    provides: Server-side downed state management
provides:
  - Heavy blur + vignette post-processing shader for downed players
  - HUD countdown timer showing MM:SS format with color interpolation
  - Client-side handlers for downed state synchronization
  - Packet receivers for state updates from server
affects: [03-05-revival-mechanics, 03-06-death-validation]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Satin API ManagedShaderEffect pattern for post-processing shaders"
    - "WorldRenderEvents.AFTER_TRANSLUCENT for shader rendering"
    - "HudRenderCallback for overlay rendering"
    - "Static handler pattern for client-side state management"

key-files:
  created:
    - src/main/resources/assets/dread/shaders/post/downed_blur.json
    - src/main/resources/assets/dread/shaders/program/downed_blur.fsh
    - src/main/resources/assets/dread/shaders/program/downed_blur.vsh
    - src/client/java/com/dread/client/DownedStateClientHandler.java
    - src/client/java/com/dread/client/DownedHudOverlay.java
  modified:
    - src/client/java/com/dread/DreadClient.java (packet receivers registered in parallel plan 03-03)

key-decisions:
  - "Gaussian blur with radius 8.0 for claustrophobic near-blind effect"
  - "Vignette intensity 0.7 to darken screen edges"
  - "Color interpolation yellow to red for countdown timer urgency"
  - "Render shader in AFTER_TRANSLUCENT event for proper layering"

patterns-established:
  - "Static client handler pattern for managing client-side state across packets"
  - "Shader uniforms passed via JSON for easy tuning without code changes"
  - "Color interpolation utility for smooth transitions"

# Metrics
duration: 6.4min
completed: 2026-01-24
---

# Phase 03 Plan 04: Downed State Visual Effects Summary

**Heavy blur/vignette post-processing shader with HUD countdown timer (yellow-to-red) for downed players using Satin API**

## Performance

- **Duration:** 6.4 min
- **Started:** 2026-01-24T23:15:57Z
- **Completed:** 2026-01-24T23:22:22Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- Implemented Gaussian blur + vignette post-processing shader for claustrophobic downed vision
- Created HUD countdown timer with MM:SS format and color interpolation (yellow to red)
- Integrated Satin API ManagedShaderEffect for clean shader lifecycle management
- Registered all S2C packet receivers for downed state synchronization
- Effects cleanly apply/remove via network packet triggers

## Task Commits

Each task was committed atomically:

1. **Task 1: Create downed state shader effects using Satin API** - `467d001` (feat)
2. **Task 2: Create HUD countdown timer for downed state** - `1916950` (feat)

_Note: DreadClient.java packet receivers were committed in parallel plan 03-03 (commit 2fbb2a0) due to Wave 2 parallel execution_

## Files Created/Modified
- `src/main/resources/assets/dread/shaders/post/downed_blur.json` - Post-processing shader definition with blur/vignette uniforms
- `src/main/resources/assets/dread/shaders/program/downed_blur.fsh` - Fragment shader with Gaussian blur and vignette effect
- `src/main/resources/assets/dread/shaders/program/downed_blur.vsh` - Vertex shader (passthrough)
- `src/client/java/com/dread/client/DownedStateClientHandler.java` - Manages shader lifecycle and countdown state
- `src/client/java/com/dread/client/DownedHudOverlay.java` - Renders centered countdown timer with color interpolation
- `src/client/java/com/dread/DreadClient.java` - Registers handlers and packet receivers (modified by both plans 03-03 and 03-04 in parallel)

## Decisions Made

**Shader configuration:**
- BlurRadius: 8.0 - Creates heavy blur for claustrophobic near-blind effect without performance issues
- VignetteIntensity: 0.7 - Darkens screen edges significantly to tunnel vision
- Gaussian blur algorithm - Better visual quality than box blur for horror atmosphere

**Timer presentation:**
- Color interpolation from yellow (0xFFFFFF00) to red (0xFFFF0000) based on time ratio
- Assumes max 300 seconds (5 minutes) for ratio calculation matching server constant
- Centered positioning with "DOWNED" label for clear state indication

**Integration approach:**
- WorldRenderEvents.AFTER_TRANSLUCENT for shader rendering ensures proper layering after world geometry
- HudRenderCallback for timer ensures it renders on top of all world elements
- Static handler methods for cross-packet state management (simpler than instance management)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

**Parallel execution coordination:**
- Plan 03-03 (death cinematic) and 03-04 (downed effects) both modified DreadClient.java simultaneously
- Plan 03-03 committed first (2fbb2a0) and included packet receiver registrations for both plans
- Resolution: Verified 03-03 commit included all required registrations, committed only DownedHudOverlay.java separately
- No merge conflicts, clean collaboration between parallel Wave 2 plans

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready for next phase:**
- Shader effects fully functional and applied/removed via network packets
- HUD countdown timer synchronized with server-side state
- All packet receivers registered and tested with clean compilation
- Static handler pattern established for state management

**For plan 03-05 (Revival mechanics):**
- DownedStateClientHandler.updateCountdown() ready for tick-based updates
- RemoveDownedEffectsS2C packet receiver ready for revival cleanup
- Revival progress packet receiver stubbed with TODO for future UI implementation

**No blockers or concerns**

---
*Phase: 03-death-revival-system*
*Completed: 2026-01-24*
