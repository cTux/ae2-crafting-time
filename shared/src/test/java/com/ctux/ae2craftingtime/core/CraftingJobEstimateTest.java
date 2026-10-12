package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CraftingJobEstimateTest {
    private static final ProfileKey ROOT = new ProfileKey("test:root");
    private static final ProfileKey MIDDLE = new ProfileKey("test:middle");
    private static final ProfileKey LEAF = new ProfileKey("test:leaf");

    @Test
    void sumsLaddersAndUsesLongestParallelBranch() {
        var estimate = new CraftingJobEstimate(ROOT, Map.of(ROOT, 5L, MIDDLE, 10L, LEAF, 20L),
                Map.of(ROOT, Set.of(MIDDLE, LEAF), MIDDLE, Set.of(LEAF)));

        assertEquals(35, estimate.remainingEstimate(CraftingJobEstimateTest::seconds).orElseThrow().durationMs());
    }

    @Test
    void buildsDependenciesFromUsedPatternsAndOmitsAmbiguousInputs() {
        var unrelated = new ProfileKey("test:unrelated");
        var patterns = List.of(
                new CraftingJobEstimate.Pattern(1, List.of(ROOT),
                        List.of(Set.of(MIDDLE, unrelated), Set.of(LEAF, MIDDLE), Set.of(ROOT))),
                new CraftingJobEstimate.Pattern(1, List.of(MIDDLE), List.of(Set.of(LEAF))),
                new CraftingJobEstimate.Pattern(0, List.of(LEAF), List.of(Set.of(MIDDLE))),
                new CraftingJobEstimate.Pattern(1, List.of(unrelated), List.of(Set.of(LEAF))));

        assertEquals(Map.of(ROOT, Set.of(MIDDLE), MIDDLE, Set.of(LEAF)),
                CraftingJobEstimate.dependencies(Set.of(ROOT, MIDDLE, LEAF), patterns).edges());
    }

    @Test
    void tracksRemainingWorkWithoutDoubleCountingSharedDependencies() {
        var other = new ProfileKey("test:other");
        var estimate = new CraftingJobEstimate(ROOT, Map.of(ROOT, 5L, MIDDLE, 10L, other, 8L, LEAF, 20L),
                Map.of(ROOT, Set.of(MIDDLE, other), MIDDLE, Set.of(LEAF), other, Set.of(LEAF)));

        assertEquals(35, estimate.remainingEstimate(CraftingJobEstimateTest::seconds).orElseThrow().durationMs());
        estimate.complete(LEAF, 12);
        assertEquals(23, estimate.remainingEstimate(CraftingJobEstimateTest::seconds).orElseThrow().durationMs());
    }

    @Test
    void remainsAnHonestLowerBoundWithMissingOrCyclicEdges() {
        var disconnected = new ProfileKey("test:disconnected");
        var estimate = new CraftingJobEstimate(ROOT, Map.of(ROOT, 5L, MIDDLE, 10L, disconnected, 40L),
                Map.of(ROOT, Set.of(MIDDLE), MIDDLE, Set.of(ROOT)));

        assertEquals(40, estimate.remainingEstimate((key, amount) -> key.equals(MIDDLE)
                ? Optional.empty() : seconds(key, amount)).orElseThrow().durationMs());
        assertFalse(estimate.remainingEstimate((key, amount) -> Optional.empty()).isPresent());
    }

    @Test
    void includesKnownDependenciesWhenTheirParentIsUnknown() {
        var estimate = new CraftingJobEstimate(ROOT, Map.of(ROOT, 5L, LEAF, 20L),
                Map.of(ROOT, Set.of(LEAF)));

        assertEquals(20, estimate.remainingEstimate((key, amount) -> key.equals(ROOT)
                ? Optional.empty() : seconds(key, amount)).orElseThrow().durationMs());
    }

    @Test
    void boundsPathDepthForPathologicalLadders() {
        var amounts = new HashMap<ProfileKey, Long>();
        var dependencies = new HashMap<ProfileKey, Set<ProfileKey>>();
        var root = new ProfileKey("test:depth_0");
        var current = root;
        for (int depth = 0; depth < 600; depth++) {
            amounts.put(current, 1L);
            var next = new ProfileKey("test:depth_" + (depth + 1));
            dependencies.computeIfAbsent(current, ignored -> new HashSet<>()).add(next);
            current = next;
        }

        assertEquals(512, new CraftingJobEstimate(root, amounts, dependencies)
                .remainingEstimate(CraftingJobEstimateTest::seconds).orElseThrow().durationMs());
    }

    private static Optional<TtcEstimate> seconds(ProfileKey key, long amount) {
        return amount > 0 ? Optional.of(new TtcEstimate(amount, true)) : Optional.empty();
    }

    @Test
    void preservesFractionalTotalsAndConservativeConfidence() {
        var graph = CraftingJobEstimate.dependencies(Set.of(ROOT, MIDDLE, LEAF), List.of(
                new CraftingJobEstimate.Pattern(1, List.of(ROOT), List.of(Set.of(MIDDLE, LEAF), Set.of()))));
        assertFalse(graph.complete());
        var ambiguous = new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L), graph);
        assertFalse(ambiguous.remainingEstimate((key, amount) -> Optional.of(new TtcEstimate(1, true)))
                .orElseThrow().reliable());
        var independent = new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L, LEAF, 1L, MIDDLE, 0L), Map.of());
        assertFalse(independent.remainingEstimate((key, amount) -> Optional.of(new TtcEstimate(1.2,
                key.equals(ROOT)))).orElseThrow().reliable());
        var fractional = new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L, LEAF, 1L), Map.of(ROOT, Set.of(LEAF)));
        assertEquals(new TtcEstimate(2.5, true), fractional.remainingEstimate((key, amount) ->
                Optional.of(new TtcEstimate(1.25, true))).orElseThrow());
        assertFalse(fractional.remainingEstimate((key, amount) -> key.equals(ROOT) ? Optional.empty()
                : Optional.of(new TtcEstimate(1, true))).orElseThrow().reliable());
        org.junit.jupiter.api.Assertions.assertTrue(fractional.remainingEstimate((key, amount) ->
                Optional.of(new TtcEstimate(Double.MAX_VALUE, true))).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(new CraftingJobEstimate(ROOT, Map.of(ROOT, 0L), Map.of())
                .remainingEstimate((key, amount) -> Optional.empty()).isEmpty());
    }

    @Test
    void offRootCyclesAndDepthLimitsCannotProduceInstant() {
        var cyclic = new CraftingJobEstimate(ROOT, Map.of(ROOT, 1L, MIDDLE, 1L, LEAF, 1L),
                Map.of(MIDDLE, Set.of(LEAF), LEAF, Set.of(MIDDLE)));
        assertEquals(new TtcEstimate(1, false), cyclic.remainingEstimate((key, amount) ->
                Optional.of(new TtcEstimate(1, true))).orElseThrow());
        var amounts = new HashMap<ProfileKey, Long>();
        var edges = new HashMap<ProfileKey, Set<ProfileKey>>();
        amounts.put(ROOT, 1L);
        edges.put(ROOT, Set.of(new ProfileKey("test:off_root_300")));
        for (int i = 0; i < 600; i++) {
            var key = new ProfileKey("test:off_root_" + i);
            amounts.put(key, 1L);
            edges.put(key, Set.of(new ProfileKey("test:off_root_" + (i + 1))));
        }
        assertFalse(new CraftingJobEstimate(ROOT, amounts, edges).remainingEstimate((key, amount) ->
                Optional.of(new TtcEstimate(0.001, true))).orElseThrow().reliable());
    }
}
