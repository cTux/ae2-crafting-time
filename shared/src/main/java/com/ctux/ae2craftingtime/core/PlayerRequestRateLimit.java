package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerRequestRateLimit {
    public static final int MAX_KEYS_PER_SECOND = 512;
    private final Map<UUID, Window> windows = new HashMap<>();

    public boolean allow(UUID playerId, int keyCount, long nowMillis) {
        if (keyCount < 0 || keyCount > MAX_KEYS_PER_SECOND) {
            return false;
        }
        var cost = Math.max(1, keyCount);
        var current = windows.get(playerId);
        if (current == null || nowMillis - current.startedAtMillis >= 1000) {
            windows.put(playerId, new Window(nowMillis, cost));
            return true;
        }
        if (current.keyCount + cost > MAX_KEYS_PER_SECOND) {
            return false;
        }
        windows.put(playerId, new Window(current.startedAtMillis, current.keyCount + cost));
        return true;
    }

    private record Window(long startedAtMillis, int keyCount) {
    }
}
