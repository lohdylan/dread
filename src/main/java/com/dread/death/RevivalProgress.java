package com.dread.death;

import java.util.UUID;

/**
 * Tracks an active revival attempt.
 * Revival is uninterruptible once started (per CONTEXT.md decision).
 */
public class RevivalProgress {
    public final UUID downedPlayerId;
    public final UUID reviverPlayerId;
    public int remainingTicks;  // 3 seconds = 60 ticks

    public static final int REVIVAL_DURATION_TICKS = 60;  // 3 seconds

    public RevivalProgress(UUID downedPlayerId, UUID reviverPlayerId) {
        this.downedPlayerId = downedPlayerId;
        this.reviverPlayerId = reviverPlayerId;
        this.remainingTicks = REVIVAL_DURATION_TICKS;
    }

    public float getProgress() {
        // 0.0 = just started, 1.0 = complete
        return 1.0f - ((float) remainingTicks / REVIVAL_DURATION_TICKS);
    }

    public boolean isComplete() {
        return remainingTicks <= 0;
    }

    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }
}
