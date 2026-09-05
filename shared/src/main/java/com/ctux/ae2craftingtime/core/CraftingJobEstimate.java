package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BiFunction;

public final class CraftingJobEstimate {
    private final ProfileKey root;
    private final Map<ProfileKey, Long> remainingAmounts;
    private final Map<ProfileKey, Set<ProfileKey>> dependencies;

    public CraftingJobEstimate(ProfileKey root, Map<ProfileKey, Long> remainingAmounts,
            Map<ProfileKey, Set<ProfileKey>> dependencies) {
        this.root = root;
        this.remainingAmounts = new HashMap<>(remainingAmounts);
        this.dependencies = new HashMap<>();
        dependencies.forEach((key, values) -> this.dependencies.put(key, Set.copyOf(values)));
    }

    public void complete(ProfileKey key, long amount) {
        remainingAmounts.computeIfPresent(key, (ignored, remaining) -> Math.max(0, remaining - amount));
    }

    public OptionalLong remainingSeconds(BiFunction<ProfileKey, Long, OptionalLong> estimate) {
        var cache = new HashMap<ProfileKey, Path>();
        var critical = path(root, estimate, cache, new HashSet<>());
        long longestKnownRow = 0;
        boolean anyKnown = critical.known;
        for (var entry : remainingAmounts.entrySet()) {
            var row = estimate.apply(entry.getKey(), entry.getValue());
            if (row.isPresent()) {
                anyKnown = true;
                longestKnownRow = Math.max(longestKnownRow, row.getAsLong());
            }
        }
        return anyKnown ? OptionalLong.of(Math.max(critical.seconds, longestKnownRow)) : OptionalLong.empty();
    }

    private Path path(ProfileKey key, BiFunction<ProfileKey, Long, OptionalLong> estimate,
            Map<ProfileKey, Path> cache, Set<ProfileKey> visiting) {
        var cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(key)) {
            return new Path(0, false);
        }

        var self = estimate.apply(key, remainingAmounts.getOrDefault(key, 0L));
        var longestDependency = new Path(0, false);
        for (var dependency : dependencies.getOrDefault(key, Set.of())) {
            var candidate = path(dependency, estimate, cache, visiting);
            if (candidate.seconds > longestDependency.seconds) {
                longestDependency = candidate;
            } else if (candidate.seconds == longestDependency.seconds && candidate.known) {
                longestDependency = candidate;
            }
        }
        visiting.remove(key);

        var result = new Path(self.orElse(0) + longestDependency.seconds,
                self.isPresent() || longestDependency.known);
        cache.put(key, result);
        return result;
    }

    private record Path(long seconds, boolean known) {
    }
}
