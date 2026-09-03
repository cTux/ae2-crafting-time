package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Observations from AE2's simulated dispatch extraction, never network inactivity. */
public final class DispatchPowerTracker {
    private record Failure(Set<ProfileKey> outputs, long tick) { }

    private final Map<Object, Map<Object, Failure>> failures = new IdentityHashMap<>();
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    public void observe(Object cpu, Object pattern, Map<ProfileKey, Long> outputs,
            double required, double extracted, long tick) {
        if (!enabled || cpu == null) {
            return;
        }
        var patterns = failures.computeIfAbsent(cpu, ignored -> new HashMap<>());
        var keys = new HashSet<ProfileKey>();
        // Match AE2's own comparison, including its rounding tolerance.
        if (extracted < required - 0.01) {
            outputs.forEach((key, amount) -> {
                if (amount > 0) {
                    keys.add(key);
                }
            });
        }
        if (keys.isEmpty()) {
            patterns.remove(pattern);
        } else {
            patterns.put(pattern, new Failure(keys, tick));
        }
        removeEmpty(cpu, patterns);
    }

    public Map<ProfileKey, CraftingBlockReason> reasons(Object cpu, long tick, Set<ProfileKey> missingProviders) {
        var patterns = failures.getOrDefault(cpu, new HashMap<>());
        patterns.values().removeIf(failure -> tick < failure.tick || tick - failure.tick >= 20);
        var reasons = new HashMap<ProfileKey, CraftingBlockReason>();
        patterns.values().forEach(failure -> failure.outputs.forEach(key -> reasons.put(key, CraftingBlockReason.NO_POWER)));
        missingProviders.forEach(key -> reasons.put(key, CraftingBlockReason.NO_PROVIDER));
        removeEmpty(cpu, patterns);
        return reasons;
    }

    private void removeEmpty(Object cpu, Map<Object, Failure> patterns) {
        if (patterns.isEmpty()) {
            failures.remove(cpu);
        }
    }

    public void clear(Object cpu) {
        failures.remove(cpu);
    }

    public void clear() {
        failures.clear();
    }
}
