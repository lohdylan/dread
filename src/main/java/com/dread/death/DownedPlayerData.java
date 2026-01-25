package com.dread.death;

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

    public DownedPlayerData(UUID playerId, BlockPos downedPos, int remainingTicks) {
        this.playerId = playerId;
        this.downedPos = downedPos;
        this.remainingTicks = remainingTicks;
    }

    public DownedPlayerData(NbtCompound nbt) {
        this.playerId = nbt.getUuid("PlayerId");
        this.downedPos = BlockPos.fromLong(nbt.getLong("DownedPos"));
        this.remainingTicks = nbt.getInt("RemainingTicks");
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("PlayerId", playerId);
        nbt.putLong("DownedPos", downedPos.asLong());
        nbt.putInt("RemainingTicks", remainingTicks);
        return nbt;
    }

    public int getRemainingSeconds() {
        return remainingTicks / 20;
    }
}
