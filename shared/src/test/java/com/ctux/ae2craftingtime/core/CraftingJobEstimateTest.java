package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
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

        assertEquals(35, estimate.remainingSeconds(CraftingJobEstimateTest::seconds).orElseThrow());
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
                CraftingJobEstimate.dependencies(Set.of(ROOT, MIDDLE, LEAF), patterns));
    }

    @Test
    void tracksRemainingWorkWithoutDoubleCountingSharedDependencies() {
        var other = new ProfileKey("test:other");
        var estimate = new CraftingJobEstimate(ROOT, Map.of(ROOT, 5L, MIDDLE, 10L, other, 8L, LEAF, 20L),
                Map.of(ROOT, Set.of(MIDDLE, other), MIDDLE, Set.of(LEAF), other, Set.of(LEAF)));

        assertEquals(35, estimate.remainingSeconds(CraftingJobEstimateTest::seconds).orElseThrow());
        estimate.complete(LEAF, 12);
        assertEquals(23, estimate.remainingSeconds(CraftingJobEstimateTest::seconds).orElseThrow());
    }

    @Test
    void remainsAnHonestLowerBoundWithMissingOrCyclicEdges() {
        var disconnected = new ProfileKey("test:disconnected");
        var estimate = new CraftingJobEstimate(ROOT, Map.of(ROOT, 5L, MIDDLE, 10L, disconnected, 40L),
                Map.of(ROOT, Set.of(MIDDLE), MIDDLE, Set.of(ROOT)));

        assertEquals(40, estimate.remainingSeconds((key, amount) -> key.equals(MIDDLE)
                ? OptionalLong.empty() : seconds(key, amount)).orElseThrow());
        assertFalse(estimate.remainingSeconds((key, amount) -> OptionalLong.empty()).isPresent());
    }

    @Test
    void includesKnownDependenciesWhenTheirParentIsUnknown() {
        var estimate = new CraftingJobEstimate(ROOT, Map.of(ROOT, 5L, LEAF, 20L),
                Map.of(ROOT, Set.of(LEAF)));

        assertEquals(20, estimate.remainingSeconds((key, amount) -> key.equals(ROOT)
                ? OptionalLong.empty() : seconds(key, amount)).orElseThrow());
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
                .remainingSeconds(CraftingJobEstimateTest::seconds).orElseThrow());
    }

    private static OptionalLong seconds(ProfileKey key, long amount) {
        return amount > 0 ? OptionalLong.of(amount) : OptionalLong.empty();
    }
}
