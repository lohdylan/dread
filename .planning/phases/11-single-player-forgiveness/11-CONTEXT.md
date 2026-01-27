# Phase 11: Single-Player Forgiveness - Context

**Gathered:** 2026-01-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Different downed/death behavior based on game mode — shorter timer and respawn for true singleplayer, hardcore permanent death for multiplayer/LAN. Detection is based on whether the world was opened as singleplayer (not LAN, not dedicated server).

</domain>

<decisions>
## Implementation Decisions

### Single-player detection
- "Single-player" means true singleplayer only — not LAN host, not dedicated server
- If LAN opens while downed, keep current forgiveness timer for this instance
- Previously-LAN worlds that return to singleplayer still get forgiveness (no "tainted" state)
- Mode indicator shows near the timer: "MERCY" (singleplayer) or "NO MERCY" (multiplayer)
- Mode text uses same red horror styling as existing downed text
- When in MERCY mode, both text and timer are orange/amber colored

### Timeout tuning
- Singleplayer downed state lasts 30 seconds (default)
- Multiplayer remains 300 seconds (default)
- Separate config options: `singleplayer_timeout` and `multiplayer_timeout`
- No minimum timeout for singleplayer — death leads to respawn anyway
- No final warning effects — timer just expires, death comes suddenly
- Timer color: orange/amber in singleplayer, red in multiplayer
- Vignette overlay stays red in both modes (only text/timer changes)

### Death transition
- Full death cinematic plays when singleplayer timer expires (not skipped or shortened)
- After cinematic: normal Minecraft death (death screen, respawn at bed/spawn)
- Death message is standard Dread death message (same as multiplayer)
- Extra debuff applied on respawn from Dread death in singleplayer
- keepInventory gamerule respected as normal

### Mode switching
- Player joins while downed: finish current downed state with current rules, next downed uses new rules
- Player leaves while downed: revert to singleplayer rules immediately
- Timer adjustment on revert: Claude's discretion (proportional scaling or fairest approach)
- Mode indicator updates live when player count changes
- Timer color also updates live with mode changes

### Claude's Discretion
- Exact timer scaling formula when reverting from MP to SP mid-downed
- Specific debuff type/duration for singleplayer respawn penalty
- Exact orange/amber color hex values for MERCY mode

</decisions>

<specifics>
## Specific Ideas

- "MERCY" / "NO MERCY" wording fits the horror tone — thematic rather than technical
- No warning before death adds to the tension — sudden death after the silence
- Live mode indicator updates keep player informed of stakes changing

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 11-single-player-forgiveness*
*Context gathered: 2026-01-26*
