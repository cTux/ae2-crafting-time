package com.ctux.ae2craftingtime.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RequestCooldown {
    private final Map<ProfileKey, Long> requestedAt = new ConcurrentHashMap<>();
    private final long cooldownMs;

    public RequestCooldown(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public boolean markIfAllowed(ProfileKey key, long nowMs) {
        var last = requestedAt.get(key);
        if (last != null && nowMs - last < cooldownMs) {
            return false;
        }

        requestedAt.put(key, nowMs);
        return true;
    }

    public void clear() {
        requestedAt.clear();
    }
}
