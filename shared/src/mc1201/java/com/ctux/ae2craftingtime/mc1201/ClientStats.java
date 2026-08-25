package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ClientStatsCache;
import com.ctux.ae2craftingtime.core.ProfileKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public final class ClientStats {
    public static final ClientStatsCache CACHE = new ClientStatsCache();
    private static final Map<ProfileKey, Long> NETWORK_AMOUNTS = new HashMap<>();

    public static OptionalLong networkAmount(ProfileKey key) {
        var amount = NETWORK_AMOUNTS.get(key);
        return amount == null ? OptionalLong.empty() : OptionalLong.of(amount);
    }

    public static void replaceNetworkAmounts(List<String> requestedKeys, Map<String, Long> amounts) {
        for (var key : requestedKeys) {
            NETWORK_AMOUNTS.remove(new ProfileKey(key));
        }
        amounts.forEach((key, amount) -> NETWORK_AMOUNTS.put(new ProfileKey(key), amount));
    }

    private ClientStats() {
    }
}
