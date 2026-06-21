package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.net.StatsRequestC2S;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientStatsRequests {
    private static final Map<ProfileKey, Long> REQUESTED_AT = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1000;

    public static void request(ProfileKey key) {
        var now = System.currentTimeMillis();
        var last = REQUESTED_AT.get(key);
        if (last != null && now - last < COOLDOWN_MS) {
            return;
        }

        REQUESTED_AT.put(key, now);
        // ponytail: one-key requests are simple; batch visible nodes if packet spam shows up.
        StatsNetwork.sendToServer(new StatsRequestC2S(List.of(key.outputId())));
    }

    private ClientStatsRequests() {
    }
}
