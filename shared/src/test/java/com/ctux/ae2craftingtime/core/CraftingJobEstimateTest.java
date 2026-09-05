package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

    private static OptionalLong seconds(ProfileKey key, long amount) {
        return amount > 0 ? OptionalLong.of(amount) : OptionalLong.empty();
    }
}
