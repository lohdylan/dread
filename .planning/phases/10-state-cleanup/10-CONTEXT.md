# Phase 10: State Cleanup - Context

**Gathered:** 2026-01-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Manage downed state lifecycle across world boundaries. Fix the bug where downed state persists incorrectly across different save files. Dimension changes (Nether/End) within the same world should NOT clear state.

</domain>

<decisions>
## Implementation Decisions

### World Exit Behavior
- All exits clear downed state (disconnect, /quit, server kick, crash — all treated the same)
- Player returns at 2 hearts (4 HP) as a soft penalty for escaping via disconnect
- Simple, consistent rule: leaving = escape, but with consequences

### World Entry Behavior
- Always start with clean state on world join (new world or returning)
- Dread entity persists normally like any mob (stays where it was)
- Dread retains target memory — if it was hunting you, it still is when you return
- Dimension changes within same world do NOT clear downed state (only different save files)

### Edge Cases
- /kill and void damage bypass downed timer — trigger immediate death with cinematic
- Admin /gamemode commands take precedence — switching to spectator or creative clears downed state
- Server restart clears all downed states (no disk persistence)
- Log abnormal state clears (admin commands, gamemode changes, etc.) for debugging

### Multiplayer Sync
- Chat message on reconnect after escape: notify other players when someone "narrowly escaped Dread"
- Full visual sync: other players see downed player's crawl pose
- State updates broadcast to nearby players only (rendering distance) to save bandwidth
- Death announcements go to all players server-wide
- Rejoining players receive current downed state of all players on join

### Claude's Discretion
- Crash detection (if distinguishable from normal exit)
- Kick handling specifics (if different from normal exit)
- Whether reconnect immunity is needed to prevent Dread instant-killing at 2 HP
- Exact chat message wording for escape notification

</decisions>

<specifics>
## Specific Ideas

- The 2 HP penalty should feel like "you barely escaped" — one hit from most mobs would kill
- Dread remembering its target creates tension: you can disconnect, but it's still there waiting

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 10-state-cleanup*
*Context gathered: 2026-01-26*
