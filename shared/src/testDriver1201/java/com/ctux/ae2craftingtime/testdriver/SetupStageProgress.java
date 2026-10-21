package com.ctux.ae2craftingtime.testdriver;

import java.time.Duration;

/** Reports a native fixture setup stage and bounds a stalled stage. */
final class SetupStageProgress {
    private static final long TIMEOUT_NANOS = Duration.ofSeconds(45).toNanos();
    private volatile String stage = "placing";
    private long startedAt;

    String stage() { return stage; }

    boolean observe(String next, long now) {
        if (!next.equals(stage)) {
            stage = next;
            startedAt = now;
        } else if (now - startedAt > TIMEOUT_NANOS) {
            throw new IllegalStateException("Trinity fixture setup stalled at " + stage);
        }
        return false;
    }
}
