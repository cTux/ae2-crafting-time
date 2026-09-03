package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class MissingProviderTrackerTest {
    private final MissingProviderTracker<String> tracker = new MissingProviderTracker<>();
    private final Object cpu = new Object();
    private final ProfileKey iron = new ProfileKey("network", "minecraft:iron_ingot");
    private final ProfileKey gold = new ProfileKey("network", "minecraft:gold_ingot");

    @Test
    void onlyAnEmptyLookupRecordsPositiveOutputs() {
        assertTrue(tracker.missingOutputs(null, pattern -> false).isEmpty());
        tracker.observe(cpu, "smelt", Map.of(iron, 1L), true);
        assertTrue(tracker.missingOutputs(cpu, pattern -> false).isEmpty());
        tracker.observe(cpu, "smelt", Map.of(iron, 1L, gold, 0L), false);
        assertEquals(Set.of(iron), tracker.missingOutputs(cpu, pattern -> false));
        tracker.observe(cpu, "smelt", Map.of(iron, -1L), false);
        assertTrue(tracker.missingOutputs(cpu, pattern -> false).isEmpty());
        tracker.observe(cpu, "empty", Map.of(), false);
        assertTrue(tracker.missingOutputs(cpu, pattern -> false).isEmpty());
    }

    @Test
    void aHealthyPatternCannotHideABlockedPatternWithTheSameOutput() {
        tracker.observe(cpu, "first", Map.of(iron, 1L), false);
        tracker.observe(cpu, "second", Map.of(iron, 2L, gold, 1L), false);
        tracker.observe(cpu, "first", Map.of(iron, 1L), true);
        assertEquals(Set.of(iron, gold), tracker.missingOutputs(cpu, pattern -> false));
        assertTrue(tracker.missingOutputs(cpu, pattern -> pattern.equals("second")).isEmpty());
        // Revalidation removes restored patterns; losing that provider needs another dispatch observation.
        assertTrue(tracker.missingOutputs(cpu, pattern -> false).isEmpty());
    }

    @Test
    void queriesRevalidateExactPatternsAndReturnAnIndependentUnion() {
        tracker.observe(cpu, "first", Map.of(iron, 1L), false);
        tracker.observe(cpu, "second", Map.of(gold, 2L), false);
        assertEquals(Set.of(gold), tracker.missingOutputs(cpu, pattern -> pattern.equals("first")));
        var snapshot = tracker.missingOutputs(cpu, pattern -> false);
        snapshot.clear();
        assertEquals(Set.of(gold), tracker.missingOutputs(cpu, pattern -> false));
        tracker.observe(cpu, "second", Map.of(iron, 3L), false);
        assertEquals(Set.of(iron), tracker.missingOutputs(cpu, pattern -> false));
    }

    @Test
    void scopesUseCpuIdentityAndOutputsKeepNetworkIdentity() {
        var first = new String("cpu");
        var second = new String("cpu");
        var otherIron = new ProfileKey("other-network", "minecraft:iron_ingot");
        tracker.observe(first, "pattern", Map.of(iron, 1L), false);
        tracker.observe(second, "pattern", Map.of(otherIron, 1L), false);
        assertEquals(Set.of(iron), tracker.missingOutputs(first, pattern -> false));
        assertEquals(Set.of(otherIron), tracker.missingOutputs(second, pattern -> false));
        tracker.clear(first);
        assertTrue(tracker.missingOutputs(first, pattern -> false).isEmpty());
        assertFalse(tracker.missingOutputs(second, pattern -> false).isEmpty());
        tracker.clear();
        assertTrue(tracker.missingOutputs(second, pattern -> false).isEmpty());
    }

    @Test
    void disablingClearsStateAndRejectsNewObservationsUntilReenabled() {
        tracker.observe(cpu, "pattern", Map.of(iron, 1L), false);
        tracker.setEnabled(false);
        assertTrue(tracker.missingOutputs(cpu, pattern -> false).isEmpty());
        tracker.observe(cpu, "pattern", Map.of(iron, 1L), false);
        assertTrue(tracker.missingOutputs(cpu, pattern -> false).isEmpty());
        tracker.setEnabled(true);
        tracker.observe(cpu, "pattern", Map.of(iron, 1L), false);
        assertEquals(Set.of(iron), tracker.missingOutputs(cpu, pattern -> false));
    }
}
