package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CpuTtcRateLimit {
    public static final int MAX_PACKETS_PER_SECOND = 4;
    public static final int MAX_SERIALS_PER_SECOND = 128;
    private final int packetLimit;
    private final int serialLimit;
    private final Map<UUID, Window> windows = new HashMap<>();

    public CpuTtcRateLimit() {
        this(MAX_PACKETS_PER_SECOND, MAX_SERIALS_PER_SECOND);
    }

    CpuTtcRateLimit(int packetLimit, int serialLimit) {
        if (packetLimit <= 0 || serialLimit < 0) {
            throw new IllegalArgumentException("invalid CPU TTC rate limit");
        }
        this.packetLimit = packetLimit;
        this.serialLimit = serialLimit;
    }

    public boolean allow(UUID playerId, int serialCount, long nowMillis) {
        if (serialCount < 0 || serialCount > CpuTtcCache.MAX_CPUS) {
            return false;
        }
        var current = windows.get(playerId);
        if (current == null || nowMillis - current.startedAtMillis >= 1_000) {
            windows.put(playerId, new Window(nowMillis, 1, serialCount));
            return true;
        }
        if (current.packets == packetLimit || current.serials + serialCount > serialLimit) {
            return false;
        }
        windows.put(playerId, new Window(current.startedAtMillis, current.packets + 1,
                current.serials + serialCount));
        return true;
    }

    public void clear(UUID playerId) {
        windows.remove(playerId);
    }

    public void clear() {
        windows.clear();
    }

    private record Window(long startedAtMillis, int packets, int serials) {
    }
}
