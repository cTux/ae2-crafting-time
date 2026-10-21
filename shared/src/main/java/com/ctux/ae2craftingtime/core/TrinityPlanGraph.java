package com.ctux.ae2craftingtime.core;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/** Projects native scheduled firings without expanding their logical repeat counts. */
public final class TrinityPlanGraph {
    public static CraftingJobEstimate.DependencyGraph project(Set<ProfileKey> crafted,
            List<CraftingJobEstimate.Pattern> firings, boolean repeatedOrCyclic) {
        var graph = CraftingJobEstimate.dependencies(crafted, firings);
        var produced = new HashSet<ProfileKey>();
        boolean competingProducers = firings.stream().flatMap(firing -> firing.outputs().stream())
                .anyMatch(output -> !produced.add(output));
        return new CraftingJobEstimate.DependencyGraph(graph.edges(),
                !firings.isEmpty() && !repeatedOrCyclic && !competingProducers && graph.complete());
    }

    private TrinityPlanGraph() { }
}
