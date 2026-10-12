package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TtcSortTest {
    @Test
    void arrivingStatsRefreshOrderWithoutChangingEntriesOrMovingWaitingAhead() {
        var entries = List.of("waiting", "running");
        var times = new java.util.HashMap<String, Long>();
        java.util.function.Function<String, Optional<TtcEstimate>> seconds = key -> times.containsKey(key)
                ? Optional.of(new TtcEstimate(times.get(key), true)) : Optional.empty();
        assertEquals(entries, TtcSort.copySorted(entries, seconds, Comparator.naturalOrder(), true));
        times.put("running", 5L);
        for (boolean descending : List.of(false, true)) {
            assertEquals(List.of("running", "waiting"),
                    TtcSort.copySorted(entries, seconds, Comparator.naturalOrder(), descending));
        }
        assertEquals(List.of("waiting", "running"), entries);
    }

    @Test
    void knownTimesSortBeforeUnknownAndKeepStableUnknownOrder() {
        var entries = List.of(entry("unknown-a", Optional.empty()), entry("slow", Optional.of(new TtcEstimate(10, true))),
                entry("unknown-b", Optional.empty()), entry("fast", Optional.of(new TtcEstimate(2, true))));

        var sorted = TtcSort.copySorted(entries, Entry::seconds, Comparator.comparing(Entry::name), false);

        assertEquals(List.of("fast", "slow", "unknown-a", "unknown-b"),
                sorted.stream().map(Entry::name).toList());
    }

    @Test
    void descendingReversesKnownTimesOnly() {
        var entries = List.of(entry("unknown", Optional.empty()), entry("slow", Optional.of(new TtcEstimate(10, true))),
                entry("fast", Optional.of(new TtcEstimate(2, true))));

        var sorted = TtcSort.copySorted(entries, Entry::seconds, Comparator.comparing(Entry::name), true);

        assertEquals(List.of("slow", "fast", "unknown"), sorted.stream().map(Entry::name).toList());
    }

    @Test
    void equalKnownTimesUseFallbackOrder() {
        var entries = List.of(entry("b", Optional.of(new TtcEstimate(2, true))), entry("a", Optional.of(new TtcEstimate(2, true))));

        var sorted = TtcSort.copySorted(entries, Entry::seconds, Comparator.comparing(Entry::name), false);

        assertEquals(List.of("a", "b"), sorted.stream().map(Entry::name).toList());
    }

    @Test
    void priorityRowsStayFirstInAe2OrderAndBothTtcDirections() {
        var entries = List.of(entry("normal-unknown", Optional.empty(), false),
                entry("missing-fast", Optional.of(new TtcEstimate(2, true)), true), entry("normal-slow", Optional.of(new TtcEstimate(10, true)), false),
                entry("missing-slow", Optional.of(new TtcEstimate(8, true)), true), entry("missing-unknown", Optional.empty(), true));

        assertNames(List.of("missing-fast", "missing-slow", "missing-unknown", "normal-unknown", "normal-slow"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds, Comparator.comparing(Entry::name), false, false));
        assertNames(List.of("missing-fast", "missing-slow", "missing-unknown", "normal-slow", "normal-unknown"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds, Comparator.comparing(Entry::name), true, false));
        assertNames(List.of("missing-slow", "missing-fast", "missing-unknown", "normal-slow", "normal-unknown"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds, Comparator.comparing(Entry::name), true, true));
    }

    @Test
    void priorityGroupsKeepEqualTtcFallbackOrder() {
        var entries = List.of(entry("missing-b", Optional.of(new TtcEstimate(2, true)), true),
                entry("normal-b", Optional.of(new TtcEstimate(2, true)), false), entry("missing-a", Optional.of(new TtcEstimate(2, true)), true),
                entry("normal-a", Optional.of(new TtcEstimate(2, true)), false));

        assertNames(List.of("missing-a", "missing-b", "normal-a", "normal-b"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds,
                        Comparator.comparing(Entry::name), true, false));
    }

    @Test
    void arrivingStatsReorderOnlyInsidePriorityGroups() {
        var entries = List.of(entry("normal", Optional.empty(), false),
                entry("missing-unknown", Optional.empty(), true),
                entry("missing-known", Optional.of(new TtcEstimate(5, true)), true));

        assertNames(List.of("missing-known", "missing-unknown", "normal"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds,
                        Comparator.comparing(Entry::name), true, true));
        var refreshed = List.of(entry("normal", Optional.of(new TtcEstimate(20, true)), false),
                entry("missing-unknown", Optional.of(new TtcEstimate(10, true)), true),
                entry("missing-known", Optional.of(new TtcEstimate(5, true)), true));
        assertNames(List.of("missing-unknown", "missing-known", "normal"),
                TtcSort.copyPrioritizedSorted(refreshed, Entry::priority, Entry::seconds,
                        Comparator.comparing(Entry::name), true, true));
    }

    private static void assertNames(List<String> expected, List<Entry> entries) {
        assertEquals(expected, entries.stream().map(Entry::name).toList());
    }

    private static Entry entry(String name, Optional<TtcEstimate> seconds) {
        return entry(name, seconds, false);
    }

    private static Entry entry(String name, Optional<TtcEstimate> seconds, boolean priority) {
        return new Entry(name, seconds, priority);
    }

    private record Entry(String name, Optional<TtcEstimate> seconds, boolean priority) {
    }
}
