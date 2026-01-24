---
phase: 02-ai-horror-experience
plan: 02
subsystem: spawn-probability
tags: [persistence, events, probability, NBT, spawn-system]
requires:
  - 01-01: Entity registration framework
  - 01-03: Entity behaviors and world interaction
  - 02-01: Sound system for spawn events
provides:
  - Persistent spawn probability tracking with NBT serialization
  - Mining counter per player (increments on block break)
  - Day-based spawn escalation formula (capped at day 20)
  - Cooldown system (standard 30-60s, short 10-20s for fake-outs)
  - Real spawn vs fake-out decision logic (3:1 ratio)
  - Server tick evaluation at 1-second intervals
affects:
  - 02-03: Will use spawn triggers and cooldowns
  - 02-04: Will consume mining counter data for adaptive behavior
  - 02-05: Will integrate with turn-around mechanics
tech-stack:
  added:
    - "Fabric ServerTickEvents API"
    - "Fabric PlayerBlockBreakEvents API"
    - "PersistentState with NBT serialization"
  patterns:
    - "Event-driven architecture for block tracking"
    - "Probability calculation with multiple weighted factors"
    - "World-persistent state management"
key-files:
  created:
    - src/main/java/com/dread/spawn/SpawnProbabilityState.java
    - src/main/java/com/dread/spawn/DreadSpawnManager.java
  modified:
    - src/main/java/com/dread/DreadMod.java
decisions:
  - id: PROB-01
    what: "Base spawn chance: 0.5% per second"
    why: "Provides 10-20% chance per minute without escalation, feels rare but achievable"
    impact: "Sets baseline horror tension without overwhelming players"
  - id: PROB-02
    what: "Day escalation capped at day 20 (11x multiplier)"
    why: "Prevents infinite scaling that makes late-game unplayable"
    impact: "Balances long-term progression with survivability"
  - id: PROB-03
    what: "Mining adds +0.1% per block mined"
    why: "Rewards active players, makes mining risky and atmospheric"
    impact: "Encourages strategic mining behavior, creates tension"
  - id: PROB-04
    what: "Real spawn 25%, fake-out 75% (3:1 ratio)"
    why: "Maintains psychological horror through unpredictability"
    impact: "Prevents player desensitization to spawn sounds/effects"
  - id: PROB-05
    what: "Standard cooldown 30-60s random, short cooldown 10-20s for fake-outs"
    why: "Prevents spawn spam while allowing varied pacing"
    impact: "Natural rhythm, fake-outs can lead to quicker follow-up tension"
metrics:
  duration: "4.2 min"
  completed: 2026-01-24
---

# Phase 2 Plan 2: Spawn Probability System Summary

**One-liner:** World-persistent spawn probability tracking with mining counters, day escalation (capped at day 20), and dual cooldown system (30-60s standard, 10-20s fake-out).

## What Was Built

Created the complete spawn probability system that tracks player activity and determines when Dread spawns should occur.

**Core Components:**

1. **SpawnProbabilityState (PersistentState)**
   - Per-player tracking: blocksMined, lastSpawnTick, cooldownEndTick, fakeoutCount
   - NBT serialization for cross-session persistence
   - Standard cooldown (30-60s random) after real spawns
   - Short cooldown (10-20s random) after fake-outs
   - All modifications call markDirty() for proper persistence

2. **DreadSpawnManager (Event Handler)**
   - Server tick evaluation every 1 second (20 ticks)
   - Block break event tracking for mining counters
   - Probability formula: (0.5% base × day multiplier) + mining bonus
   - Day escalation: Linear up to day 20 cap (1x to 11x multiplier)
   - Mining bonus: +0.1% per block mined since last spawn
   - Real spawn vs fake-out decision (25% real / 75% fake-out)
   - Cooldown enforcement before spawn checks
   - Debug logging for spawn decisions

3. **Integration**
   - Registered in DreadMod.onInitialize() after ModSounds and ModEntities
   - Uses Fabric API events: ServerTickEvents.END_WORLD_TICK, PlayerBlockBreakEvents.AFTER

## Technical Implementation

**Probability Calculation:**
```
totalChance = (baseChance * dayMultiplier) + miningBonus

Where:
- baseChance = 0.005 (0.5% per check)
- dayMultiplier = 1.0 + min(worldDay, 20) * 0.5 (capped at 11x)
- miningBonus = blocksMined * 0.001
```

**Example Scenarios:**
- Day 1, 0 blocks mined: 0.75% per check (~15% per minute)
- Day 10, 50 blocks mined: 3% per check + 5% bonus = 8% per check
- Day 20+, 100 blocks mined: 5.5% per check + 10% bonus = 15.5% per check (capped day scaling)

**NBT Structure:**
```
{
  "players": [
    {
      "uuid": <player UUID>,
      "data": {
        "blocksMined": <int>,
        "lastSpawnTick": <long>,
        "cooldownEndTick": <long>,
        "fakeoutCount": <int>
      }
    }
  ]
}
```

## Deviations from Plan

None - plan executed exactly as written.

## Decisions Made

**PROB-01: Base spawn chance 0.5% per second**
- Provides natural pacing without overwhelming players
- At 1 check/second, this is ~10-20% chance per minute baseline
- Feels rare enough to be surprising but common enough to maintain tension

**PROB-02: Day escalation capped at day 20**
- Prevents infinite scaling that makes late-game unplayable
- 11x multiplier at cap means day 100 plays same as day 20
- Balances long-term progression with survivability

**PROB-03: Mining bonus +0.1% per block**
- Makes mining inherently risky and atmospheric
- Resets after spawn, encouraging strategic behavior
- 100 blocks mined = +10% spawn chance (significant but not overwhelming)

**PROB-04: 3:1 fake-out to real spawn ratio**
- 75% fake-outs prevent desensitization to spawn sounds
- 25% real spawns maintain genuine threat
- Unpredictability is core to psychological horror

**PROB-05: Dual cooldown system**
- Standard 30-60s after real spawns prevents spam
- Short 10-20s after fake-outs allows tension build-up
- Random ranges prevent predictability

## Challenges & Solutions

**Challenge:** Ensuring markDirty() is called on all state modifications
**Solution:** Centralized all state changes through dedicated methods (incrementMinedBlocks, resetAfterSpawn, setShortCooldown, incrementFakeout) that all call markDirty() internally. Added comments in code to reinforce this pattern.

**Challenge:** Balancing spawn probability to avoid overwhelming players
**Solution:** Implemented day cap at 20 to prevent exponential scaling. Mining bonus resets after spawn, so it can't accumulate indefinitely. Base chance calibrated to ~15% per minute without any modifiers.

## Files Changed

**Created:**
- `src/main/java/com/dread/spawn/SpawnProbabilityState.java` (190 lines)
  - PersistentState implementation with NBT serialization
  - PlayerSpawnData inner class for per-player tracking
  - Cooldown management (standard and short)
  - Mining counter tracking

- `src/main/java/com/dread/spawn/DreadSpawnManager.java` (127 lines)
  - Server tick event registration (1-second intervals)
  - Block break event registration for mining tracking
  - Probability calculation with multi-factor formula
  - Real spawn vs fake-out decision logic
  - Debug logging for spawn events

**Modified:**
- `src/main/java/com/dread/DreadMod.java`
  - Added DreadSpawnManager.register() after ModEntities.register()
  - Added import for com.dread.spawn.DreadSpawnManager

## Testing Evidence

**Build verification:**
```
BUILD SUCCESSFUL in 6s
9 actionable tasks: 9 up-to-date
```

**Code verification:**
- NBT serialization methods: writeNbt(), createFromNbt() ✓
- Event registration: END_WORLD_TICK, AFTER block break ✓
- Probability formula: base chance, day multiplier (capped), mining bonus ✓
- Cooldown enforcement: isOnCooldown() checks before spawn evaluation ✓
- markDirty() calls: 4 locations (incrementMinedBlocks, resetAfterSpawn, setShortCooldown, incrementFakeout) ✓

## Next Phase Readiness

**Ready for Plan 03 (Spawn Triggering):**
- Spawn probability system provides trigger decisions
- Real spawn vs fake-out flag available for handler logic
- Cooldown system in place to prevent spam
- Player mining data persisted across sessions
- Debug logging enabled for probability tuning

**Blockers:** None

**Concerns:** None - system is self-contained and ready for integration

## Commits

| Task | Description | Commit | Files |
|------|-------------|--------|-------|
| 1 | Create SpawnProbabilityState for persistent spawn tracking | f4bdea4 | SpawnProbabilityState.java |
| 2 | Create DreadSpawnManager with probability calculation and event registration | 5af119b | DreadSpawnManager.java, DreadMod.java |

## Knowledge for Future Sessions

**For AI continuation:**
- SpawnProbabilityState stores world-persistent player spawn data (NBT saved)
- DreadSpawnManager evaluates every 1 second but checks cooldown first
- Mining counter increments on EVERY block break (no filtering)
- Day escalation is CAPPED at day 20 (worldDay can exceed, but multiplier maxes at 11x)
- TODO markers exist for Plan 03 to implement actual spawn triggering
- Real spawn = resetAfterSpawn(30-60s cooldown), Fake-out = setShortCooldown(10-20s) + incrementFakeout()

**For human developers:**
- Probability formula can be tuned by adjusting constants in calculateSpawnChance()
- Cooldown constants are at top of SpawnProbabilityState (easy to modify)
- Debug logs show spawn decisions with player name, chance, and world day
- System is event-driven - no manual polling required
- NBT format is forward-compatible (new fields can be added with defaults)
