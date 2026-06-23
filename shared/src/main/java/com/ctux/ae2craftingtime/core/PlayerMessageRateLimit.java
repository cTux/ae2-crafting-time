package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerMessageRateLimit {
    public static final long COOLDOWN_MILLIS = 2000;

    private final Map<UUID, Long> lastMessageByPlayer = new HashMap<>();

    public boolean allow(UUID playerId, long nowMillis) {
        var previous = lastMessageByPlayer.get(playerId);
        if (previous != null && nowMillis - previous < COOLDOWN_MILLIS) {
            return false;
        }

        lastMessageByPlayer.put(playerId, nowMillis);
        return true;
    }
}
