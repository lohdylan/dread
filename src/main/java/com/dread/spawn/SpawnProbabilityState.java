package com.dread.spawn;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent state for tracking spawn probability data per player.
 * Stores mining counters, spawn cooldowns, and fake-out tracking.
 */
public class SpawnProbabilityState extends PersistentState {

    private final Map<UUID, PlayerSpawnData> playerData = new HashMap<>();

    // Cooldown durations in ticks (20 ticks = 1 second)
    private static final int STANDARD_COOLDOWN_MIN = 30 * 20; // 30 seconds
    private static final int STANDARD_COOLDOWN_MAX = 60 * 20; // 60 seconds
    private static final int SHORT_COOLDOWN_MIN = 10 * 20; // 10 seconds
    private static final int SHORT_COOLDOWN_MAX = 20 * 20; // 20 seconds

    /**
     * Player-specific spawn tracking data.
     */
    public static class PlayerSpawnData {
        public int blocksMined = 0;
        public long lastSpawnTick = 0;
        public long cooldownEndTick = 0; // Tick when cooldown expires
        public int fakeoutCount = 0;

        public PlayerSpawnData() {}

        public PlayerSpawnData(NbtCompound nbt) {
            this.blocksMined = nbt.getInt("blocksMined");
            this.lastSpawnTick = nbt.getLong("lastSpawnTick");
            this.cooldownEndTick = nbt.getLong("cooldownEndTick");
            this.fakeoutCount = nbt.getInt("fakeoutCount");
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("blocksMined", blocksMined);
            nbt.putLong("lastSpawnTick", lastSpawnTick);
            nbt.putLong("cooldownEndTick", cooldownEndTick);
            nbt.putInt("fakeoutCount", fakeoutCount);
            return nbt;
        }
    }

    public SpawnProbabilityState() {
        super();
    }

    /**
     * Get or create the spawn probability state for a world.
     */
    public static SpawnProbabilityState getOrCreate(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
            new Type<>(
                SpawnProbabilityState::new,
                SpawnProbabilityState::createFromNbt,
                null
            ),
            "dread_spawn_probability"
        );
    }

    /**
     * Create state from NBT data.
     */
    public static SpawnProbabilityState createFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        SpawnProbabilityState state = new SpawnProbabilityState();

        NbtList playerList = nbt.getList("players", 10); // 10 = NBT Compound type
        for (int i = 0; i < playerList.size(); i++) {
            NbtCompound playerNbt = playerList.getCompound(i);
            UUID uuid = playerNbt.getUuid("uuid");
            PlayerSpawnData data = new PlayerSpawnData(playerNbt.getCompound("data"));
            state.playerData.put(uuid, data);
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList playerList = new NbtList();

        for (Map.Entry<UUID, PlayerSpawnData> entry : playerData.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUuid("uuid", entry.getKey());
            playerNbt.put("data", entry.getValue().toNbt());
            playerList.add(playerNbt);
        }

        nbt.put("players", playerList);
        return nbt;
    }

    /**
     * Get or create player data for the given UUID.
     */
    private PlayerSpawnData getOrCreatePlayerData(UUID player) {
        return playerData.computeIfAbsent(player, k -> new PlayerSpawnData());
    }

    /**
     * Increment the mined blocks counter for a player.
     */
    public void incrementMinedBlocks(UUID player) {
        PlayerSpawnData data = getOrCreatePlayerData(player);
        data.blocksMined++;
        markDirty();
    }

    /**
     * Reset player data after a real spawn occurs.
     * Sets standard cooldown (30-60s).
     */
    public void resetAfterSpawn(UUID player, long currentTick) {
        PlayerSpawnData data = getOrCreatePlayerData(player);
        data.blocksMined = 0;
        data.lastSpawnTick = currentTick;
        data.fakeoutCount = 0;

        // Set standard cooldown (30-60s random)
        int cooldownDuration = STANDARD_COOLDOWN_MIN +
            (int)(Math.random() * (STANDARD_COOLDOWN_MAX - STANDARD_COOLDOWN_MIN));
        data.cooldownEndTick = currentTick + cooldownDuration;

        markDirty();
    }

    /**
     * Set a shorter cooldown after a fake-out.
     * Used to allow quicker follow-up scares.
     */
    public void setShortCooldown(UUID player, long currentTick) {
        PlayerSpawnData data = getOrCreatePlayerData(player);
        data.lastSpawnTick = currentTick;

        // Set short cooldown (10-20s random)
        int cooldownDuration = SHORT_COOLDOWN_MIN +
            (int)(Math.random() * (SHORT_COOLDOWN_MAX - SHORT_COOLDOWN_MIN));
        data.cooldownEndTick = currentTick + cooldownDuration;

        markDirty();
    }

    /**
     * Get the number of blocks mined by a player since last spawn.
     */
    public int getMinedBlocks(UUID player) {
        PlayerSpawnData data = playerData.get(player);
        return data != null ? data.blocksMined : 0;
    }

    /**
     * Get the last spawn tick for a player.
     */
    public long getLastSpawnTick(UUID player) {
        PlayerSpawnData data = playerData.get(player);
        return data != null ? data.lastSpawnTick : 0;
    }

    /**
     * Increment the fake-out counter for a player.
     */
    public void incrementFakeout(UUID player) {
        PlayerSpawnData data = getOrCreatePlayerData(player);
        data.fakeoutCount++;
        markDirty();
    }

    /**
     * Check if a player is currently on cooldown.
     */
    public boolean isOnCooldown(UUID player, long currentTick) {
        PlayerSpawnData data = playerData.get(player);
        if (data == null) {
            return false;
        }
        return currentTick < data.cooldownEndTick;
    }
}
