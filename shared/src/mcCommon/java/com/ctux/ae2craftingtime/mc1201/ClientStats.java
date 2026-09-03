package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.ClientStatsCache;
import com.ctux.ae2craftingtime.core.ProfileKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static void replaceWaitingTicks(List<String> requestedKeys, Map<String, Long> values) {
        var waiting = new HashMap<ProfileKey, Long>();
        values.forEach((key, ticks) -> waiting.put(new ProfileKey(key), ticks));
        CACHE.replaceWaiting(requestedKeys.stream().map(ProfileKey::new).toList(), waiting);
    }

    public static void replaceMissingProviders(List<String> requestedKeys, Set<String> values, long cpuContext) {
        CACHE.replaceMissingProviders(requestedKeys.stream().map(ProfileKey::new).toList(),
                values.stream().map(ProfileKey::new).collect(Collectors.toSet()), cpuContext);
    }

    public static boolean missingProvider(ProfileKey key) {
        var context = Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen
                ? StatsRequestContext.cpuContext(screen.getMenu()) : -1;
        return CACHE.missingProvider(key, context);
    }

    private ClientStats() {
    }
}
