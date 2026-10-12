package com.ctux.ae2craftingtime.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public final class CraftingJobEstimate {
    private static final int MAX_DEPENDENCY_DEPTH = 512;
    private final ProfileKey root;
    private final Map<ProfileKey, Long> remainingAmounts;
    private final Map<ProfileKey, Set<ProfileKey>> dependencies;
    private final boolean complete;

    public CraftingJobEstimate(ProfileKey root, Map<ProfileKey, Long> remainingAmounts,
            Map<ProfileKey, Set<ProfileKey>> dependencies) {
        this(root, remainingAmounts, new DependencyGraph(dependencies, true));
    }

    public CraftingJobEstimate(ProfileKey root, Map<ProfileKey, Long> remainingAmounts,
            DependencyGraph graph) {
        this.root = root;
        this.remainingAmounts = new HashMap<>(remainingAmounts);
        this.dependencies = new HashMap<>();
        graph.edges().forEach((key, values) -> this.dependencies.put(key, Set.copyOf(values)));
        complete = graph.complete();
    }

    public static DependencyGraph dependencies(Set<ProfileKey> crafted,
            List<Pattern> patterns) {
        var dependencies = new HashMap<ProfileKey, Set<ProfileKey>>();
        boolean complete = true;
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
                } else if (candidates.size() > 1) {
                    complete = false;
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
        return new DependencyGraph(dependencies, complete);
    }

    public void complete(ProfileKey key, long amount) {
        remainingAmounts.computeIfPresent(key, (ignored, remaining) -> Math.max(0, remaining - amount));
    }

    public Optional<TtcEstimate> remainingEstimate(BiFunction<ProfileKey, Long, Optional<TtcEstimate>> estimate) {
        var rows = new HashMap<ProfileKey, Optional<TtcEstimate>>();
        boolean reliable = complete;
        double longestKnownRow = -1;
        for (var entry : remainingAmounts.entrySet()) {
            if (entry.getValue() <= 0) continue;
            var row = estimate.apply(entry.getKey(), entry.getValue());
            rows.put(entry.getKey(), row);
            reliable &= row.isPresent() && row.get().reliable();
            if (row.isPresent()) {
                longestKnownRow = Math.max(longestKnownRow, row.get().durationMs());
            }
        }
        var state = new Traversal(reliable);
        var critical = path(root, rows, new HashMap<>(), new HashSet<>(), 0, state);
        var total = Math.max(critical, longestKnownRow);
        return total > 0 && Double.isFinite(total)
                ? Optional.of(new TtcEstimate(total, state.reliable)) : Optional.empty();
    }

    private double path(ProfileKey key, Map<ProfileKey, Optional<TtcEstimate>> rows,
            Map<ProfileKey, Double> cache, Set<ProfileKey> visiting, int depth, Traversal state) {
        if (depth >= MAX_DEPENDENCY_DEPTH) {
            state.reliable = false;
            return -1;
        }
        var cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(key)) {
            state.reliable = false;
            return -1;
        }

        var self = rows.getOrDefault(key, Optional.empty());
        double longestDependency = -1;
        for (var dependency : dependencies.getOrDefault(key, Set.of())) {
            longestDependency = Math.max(longestDependency, path(dependency, rows, cache, visiting, depth + 1, state));
        }
        visiting.remove(key);

        var result = self.isEmpty() && longestDependency < 0
                ? -1
                : self.map(TtcEstimate::durationMs).orElse(0.0) + Math.max(0, longestDependency);
        cache.put(key, result);
        return result;
    }

    public record Pattern(long uses, List<ProfileKey> outputs, List<Set<ProfileKey>> inputChoices) {
    }

    public record DependencyGraph(Map<ProfileKey, Set<ProfileKey>> edges, boolean complete) { }

    private static final class Traversal {
        private boolean reliable;
        private Traversal(boolean reliable) { this.reliable = reliable; }
    }
}
