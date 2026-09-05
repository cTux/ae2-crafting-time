package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BiFunction;

public final class CraftingJobEstimate {
    private static final int MAX_DEPENDENCY_DEPTH = 512;
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

    public static Map<ProfileKey, Set<ProfileKey>> dependencies(Set<ProfileKey> crafted,
            List<Pattern> patterns) {
        var dependencies = new HashMap<ProfileKey, Set<ProfileKey>>();
        for (var pattern : patterns) {
            if (pattern.uses <= 0) {
                continue;
            }
            var inputs = new HashSet<ProfileKey>();
            for (var choices : pattern.inputChoices) {
                var candidates = new HashSet<>(choices);
                candidates.retainAll(crafted);
                if (candidates.size() == 1) {
                    inputs.add(candidates.iterator().next());
                }
            }
            for (var output : pattern.outputs) {
                if (!crafted.contains(output)) {
                    continue;
                }
                var outputDependencies = dependencies.computeIfAbsent(output, ignored -> new HashSet<>());
                inputs.stream().filter(input -> !input.equals(output)).forEach(outputDependencies::add);
            }
        }
        return dependencies;
    }

    public void complete(ProfileKey key, long amount) {
        remainingAmounts.computeIfPresent(key, (ignored, remaining) -> Math.max(0, remaining - amount));
    }

    public OptionalLong remainingSeconds(BiFunction<ProfileKey, Long, OptionalLong> estimate) {
        var cache = new HashMap<ProfileKey, Long>();
        var critical = path(root, estimate, cache, new HashSet<>(), 0);
        long longestKnownRow = -1;
        for (var entry : remainingAmounts.entrySet()) {
            var row = estimate.apply(entry.getKey(), entry.getValue());
            if (row.isPresent()) {
                longestKnownRow = Math.max(longestKnownRow, row.getAsLong());
            }
        }
        var total = Math.max(critical, longestKnownRow);
        return total >= 0 ? OptionalLong.of(total) : OptionalLong.empty();
    }

    private long path(ProfileKey key, BiFunction<ProfileKey, Long, OptionalLong> estimate,
            Map<ProfileKey, Long> cache, Set<ProfileKey> visiting, int depth) {
        if (depth >= MAX_DEPENDENCY_DEPTH) {
            return -1;
        }
        var cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(key)) {
            return -1;
        }

        var self = estimate.apply(key, remainingAmounts.getOrDefault(key, 0L));
        long longestDependency = -1;
        for (var dependency : dependencies.getOrDefault(key, Set.of())) {
            longestDependency = Math.max(longestDependency, path(dependency, estimate, cache, visiting, depth + 1));
        }
        visiting.remove(key);

        var result = self.isEmpty() && longestDependency < 0
                ? -1
                : self.orElse(0) + Math.max(0, longestDependency);
        cache.put(key, result);
        return result;
    }

    public record Pattern(long uses, List<ProfileKey> outputs, List<Set<ProfileKey>> inputChoices) {
    }
}
