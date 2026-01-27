package com.dread.death;

import com.dread.config.DreadConfigLoader;
import com.dread.death.GameModeDetector.DreadGameMode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.*;

/**
 * Persistent state tracking all downed players in a world.
 * Handles countdown timers, revival progress, and spectator transitions.
 */
public class DownedPlayersState extends PersistentState {

    private static final String STATE_NAME = "dread_downed_players";
    // Note: DOWNED_DURATION_TICKS removed - now uses mode-aware config (singleplayerDownedTimeout/multiplayerDownedTimeout)

    private final Map<UUID, DownedPlayerData> downedPlayers = new HashMap<>();
    private final Map<UUID, RevivalProgress> activeRevivals = new HashMap<>();
    // Transient set tracking players who disconnected while downed (not persisted across server restarts)
    private final transient Set<UUID> escapedPlayers = new HashSet<>();
    // Transient set tracking players who died from Dread expiration (for respawn debuff)
    private final transient Set<UUID> recentDreadDeaths = new HashSet<>();

    public DownedPlayersState() {
        super();
    }

    public static DownedPlayersState getOrCreate(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            new Type<>(
                DownedPlayersState::new,
                DownedPlayersState::createFromNbt,
                null
            ),
            STATE_NAME
        );
    }

    public static DownedPlayersState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        DownedPlayersState state = new DownedPlayersState();

        NbtList downedList = nbt.getList("DownedPlayers", 10);  // 10 = Compound type
        for (int i = 0; i < downedList.size(); i++) {
            NbtCompound playerNbt = downedList.getCompound(i);
            DownedPlayerData data = new DownedPlayerData(playerNbt);
            state.downedPlayers.put(data.playerId, data);
        }

        // Note: activeRevivals are not persisted - they reset on server restart
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList downedList = new NbtList();
        for (DownedPlayerData data : downedPlayers.values()) {
            downedList.add(data.toNbt());
        }
        nbt.put("DownedPlayers", downedList);
        return nbt;
    }

    // --- Downed State Management ---

    public void setDowned(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        BlockPos pos = player.getBlockPos();

        // Detect game mode and get appropriate timeout
        DreadGameMode mode = GameModeDetector.detectMode(player.getServerWorld());
        var config = DreadConfigLoader.getConfig();

        int timeoutTicks;
        if (mode == DreadGameMode.SINGLEPLAYER) {
            timeoutTicks = config.singleplayerDownedTimeout * 20;  // 30 seconds default
        } else {
            timeoutTicks = config.multiplayerDownedTimeout * 20;   // 300 seconds default
        }

        downedPlayers.put(playerId, new DownedPlayerData(playerId, pos, timeoutTicks, mode));
        markDirty();
    }

    public boolean isDowned(UUID playerId) {
        return downedPlayers.containsKey(playerId);
    }

    public boolean isDowned(ServerPlayerEntity player) {
        return isDowned(player.getUuid());
    }

    public DownedPlayerData getDownedData(UUID playerId) {
        return downedPlayers.get(playerId);
    }

    /**
     * Decrement timer for a downed player.
     * @return remaining seconds, or -1 if player not downed
     */
    public int decrementTimer(UUID playerId) {
        DownedPlayerData data = downedPlayers.get(playerId);
        if (data == null) return -1;

        data.remainingTicks--;
        markDirty();
        return data.getRemainingSeconds();
    }

    public void removeDowned(UUID playerId) {
        downedPlayers.remove(playerId);
        activeRevivals.remove(playerId);  // Cancel any revival in progress
        markDirty();
    }

    public Collection<DownedPlayerData> getAllDowned() {
        return Collections.unmodifiableCollection(downedPlayers.values());
    }

    // --- Revival Management ---

    public void startRevival(UUID downedPlayerId, UUID reviverPlayerId) {
        if (!isDowned(downedPlayerId)) return;

        activeRevivals.put(downedPlayerId, new RevivalProgress(downedPlayerId, reviverPlayerId));
        // Note: Revivals are uninterruptible per CONTEXT.md - no cancel on damage/movement
    }

    public boolean isBeingRevived(UUID downedPlayerId) {
        return activeRevivals.containsKey(downedPlayerId);
    }

    public RevivalProgress getRevivalProgress(UUID downedPlayerId) {
        return activeRevivals.get(downedPlayerId);
    }

    /**
     * Tick all active revivals.
     * @return List of player IDs whose revival just completed
     */
    public List<UUID> tickRevivals() {
        List<UUID> completed = new ArrayList<>();

        Iterator<Map.Entry<UUID, RevivalProgress>> iter = activeRevivals.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, RevivalProgress> entry = iter.next();
            RevivalProgress revival = entry.getValue();
            revival.tick();

            if (revival.isComplete()) {
                completed.add(entry.getKey());
                iter.remove();
            }
        }

        return completed;
    }

    public void cancelRevival(UUID downedPlayerId) {
        activeRevivals.remove(downedPlayerId);
    }

    // --- Escape Tracking (Transient - Not Persisted) ---

    /**
     * Mark a player as having escaped while downed (disconnected before timer expired).
     * This flag is NOT persisted - server restart clears all escape penalties.
     */
    public void markEscapedPlayer(UUID playerId) {
        escapedPlayers.add(playerId);
        // Note: Do NOT call markDirty() - this is transient data
    }

    /**
     * Check if a player was marked as escaped.
     * @return true if player disconnected while downed
     */
    public boolean wasEscapedPlayer(UUID playerId) {
        return escapedPlayers.contains(playerId);
    }

    /**
     * Clear escape flag after applying reconnect penalty.
     */
    public void clearEscapedPlayer(UUID playerId) {
        escapedPlayers.remove(playerId);
    }

    // --- Dread Death Tracking (Transient - Not Persisted) ---

    /**
     * Mark a player as having died from Dread timer expiration (for respawn debuff).
     * This flag is NOT persisted - server restart clears all debuff penalties.
     */
    public void markDreadDeath(UUID playerId) {
        recentDreadDeaths.add(playerId);
        // Note: Do NOT call markDirty() - this is transient data
    }

    /**
     * Check if a player recently died from Dread.
     * @return true if player died from Dread timer expiration
     */
    public boolean hadRecentDreadDeath(UUID playerId) {
        return recentDreadDeaths.contains(playerId);
    }

    /**
     * Clear Dread death flag after applying respawn debuff.
     */
    public void clearDreadDeathFlag(UUID playerId) {
        recentDreadDeaths.remove(playerId);
    }

    // --- Mode Transitions ---

    /**
     * Transition a downed player from SINGLEPLAYER to MULTIPLAYER mode.
     * Scales remaining timer proportionally to the new max.
     */
    public void transitionToMultiplayer(UUID playerId) {
        DownedPlayerData data = downedPlayers.get(playerId);
        if (data == null || data.mode != GameModeDetector.DreadGameMode.SINGLEPLAYER) {
            return; // Not downed or already in multiplayer
        }

        var config = DreadConfigLoader.getConfig();
        int spMaxTicks = config.singleplayerDownedTimeout * 20;
        int mpMaxTicks = config.multiplayerDownedTimeout * 20;

        // Proportional scaling: maintain percentage of time remaining
        float timeRatio = (float) data.remainingTicks / spMaxTicks;
        int newRemaining = Math.max(1, (int) (timeRatio * mpMaxTicks));

        data.remainingTicks = newRemaining;
        data.mode = GameModeDetector.DreadGameMode.MULTIPLAYER;
        markDirty();
    }

    /**
     * Transition a downed player from MULTIPLAYER to SINGLEPLAYER mode.
     * Scales remaining timer proportionally to the new max.
     */
    public void transitionToSingleplayer(UUID playerId) {
        DownedPlayerData data = downedPlayers.get(playerId);
        if (data == null || data.mode != GameModeDetector.DreadGameMode.MULTIPLAYER) {
            return; // Not downed or already in singleplayer
        }

        var config = DreadConfigLoader.getConfig();
        int mpMaxTicks = config.multiplayerDownedTimeout * 20;
        int spMaxTicks = config.singleplayerDownedTimeout * 20;

        // Proportional scaling: maintain percentage of time remaining
        float timeRatio = (float) data.remainingTicks / mpMaxTicks;
        int newRemaining = Math.max(1, (int) (timeRatio * spMaxTicks));

        data.remainingTicks = newRemaining;
        data.mode = GameModeDetector.DreadGameMode.SINGLEPLAYER;
        markDirty();
    }
}
