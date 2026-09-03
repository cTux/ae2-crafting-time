package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Records only failed dispatch lookups, then rechecks them before reporting. */
public final class MissingProviderTracker<P> {
    private final Map<Object, Map<P, Set<ProfileKey>>> missing = new IdentityHashMap<>();
    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    public void observe(Object cpu, P pattern, Map<ProfileKey, Long> outputs, boolean hasProvider) {
        if (!enabled) {
            return;
        }
        var patterns = missing.computeIfAbsent(cpu, ignored -> new HashMap<>());
        var keys = new HashSet<ProfileKey>();
        if (!hasProvider) {
            outputs.forEach((key, amount) -> {
                if (amount > 0) {
                    keys.add(key);
                }
            });
        }
        if (keys.isEmpty()) {
            patterns.remove(pattern);
        } else {
            patterns.put(pattern, keys);
        }
        removeEmpty(cpu, patterns);
    }

    public Set<ProfileKey> missingOutputs(Object cpu, Predicate<P> hasProvider) {
        var patterns = missing.getOrDefault(cpu, new HashMap<>());
        patterns.keySet().removeIf(hasProvider);
        var keys = new HashSet<ProfileKey>();
        patterns.values().forEach(keys::addAll);
        removeEmpty(cpu, patterns);
        return keys;
    }

    private void removeEmpty(Object cpu, Map<P, Set<ProfileKey>> patterns) {
        if (patterns.isEmpty()) {
            missing.remove(cpu);
        }
    }

    public void clear(Object cpu) {
        missing.remove(cpu);
    }

    public void clear() {
        missing.clear();
    }
}
