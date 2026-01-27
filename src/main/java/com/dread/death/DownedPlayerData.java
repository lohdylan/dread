package com.dread.death;

import com.dread.death.GameModeDetector.DreadGameMode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

/**
 * Tracks a single player's downed state.
 */
public class DownedPlayerData {
    public final UUID playerId;
    public BlockPos downedPos;
    public int remainingTicks;  // 300 seconds = 6000 ticks
    public DreadGameMode mode;

    public DownedPlayerData(UUID playerId, BlockPos downedPos, int remainingTicks, DreadGameMode mode) {
        this.playerId = playerId;
        this.downedPos = downedPos;
        this.remainingTicks = remainingTicks;
        this.mode = mode;
    }

    public DownedPlayerData(NbtCompound nbt) {
        this.playerId = nbt.getUuid("PlayerId");
        this.downedPos = BlockPos.fromLong(nbt.getLong("DownedPos"));
        this.remainingTicks = nbt.getInt("RemainingTicks");

        // Read mode from NBT, default to MULTIPLAYER for backwards compatibility
        try {
            String modeStr = nbt.getString("Mode");
            this.mode = modeStr.isEmpty() ? DreadGameMode.MULTIPLAYER : DreadGameMode.valueOf(modeStr);
        } catch (IllegalArgumentException e) {
            this.mode = DreadGameMode.MULTIPLAYER;
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("PlayerId", playerId);
        nbt.putLong("DownedPos", downedPos.asLong());
        nbt.putInt("RemainingTicks", remainingTicks);
        nbt.putString("Mode", mode.name());
        return nbt;
    }

    public int getRemainingSeconds() {
        return remainingTicks / 20;
    }
}
