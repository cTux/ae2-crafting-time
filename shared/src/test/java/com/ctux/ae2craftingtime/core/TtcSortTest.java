package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class TtcSortTest {
    @Test
    void arrivingStatsRefreshOrderWithoutChangingEntriesOrMovingWaitingAhead() {
        var entries = List.of("waiting", "running");
        var times = new java.util.HashMap<String, Long>();
        java.util.function.Function<String, OptionalLong> seconds = key -> times.containsKey(key)
                ? OptionalLong.of(times.get(key)) : OptionalLong.empty();
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
        var entries = List.of(entry("unknown-a", OptionalLong.empty()), entry("slow", OptionalLong.of(10)),
                entry("unknown-b", OptionalLong.empty()), entry("fast", OptionalLong.of(2)));

        var sorted = TtcSort.copySorted(entries, Entry::seconds, Comparator.comparing(Entry::name), false);

        assertEquals(List.of("fast", "slow", "unknown-a", "unknown-b"),
                sorted.stream().map(Entry::name).toList());
    }

    @Test
    void descendingReversesKnownTimesOnly() {
        var entries = List.of(entry("unknown", OptionalLong.empty()), entry("slow", OptionalLong.of(10)),
                entry("fast", OptionalLong.of(2)));

        var sorted = TtcSort.copySorted(entries, Entry::seconds, Comparator.comparing(Entry::name), true);

        assertEquals(List.of("slow", "fast", "unknown"), sorted.stream().map(Entry::name).toList());
    }

    @Test
    void equalKnownTimesUseFallbackOrder() {
        var entries = List.of(entry("b", OptionalLong.of(2)), entry("a", OptionalLong.of(2)));

        var sorted = TtcSort.copySorted(entries, Entry::seconds, Comparator.comparing(Entry::name), false);

        assertEquals(List.of("a", "b"), sorted.stream().map(Entry::name).toList());
    }

    @Test
    void priorityRowsStayFirstInAe2OrderAndBothTtcDirections() {
        var entries = List.of(entry("normal-unknown", OptionalLong.empty(), false),
                entry("missing-fast", OptionalLong.of(2), true), entry("normal-slow", OptionalLong.of(10), false),
                entry("missing-slow", OptionalLong.of(8), true), entry("missing-unknown", OptionalLong.empty(), true));

        assertNames(List.of("missing-fast", "missing-slow", "missing-unknown", "normal-unknown", "normal-slow"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds, Comparator.comparing(Entry::name), false, false));
        assertNames(List.of("missing-fast", "missing-slow", "missing-unknown", "normal-slow", "normal-unknown"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds, Comparator.comparing(Entry::name), true, false));
        assertNames(List.of("missing-slow", "missing-fast", "missing-unknown", "normal-slow", "normal-unknown"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds, Comparator.comparing(Entry::name), true, true));
    }

    @Test
    void priorityGroupsKeepEqualTtcFallbackOrder() {
        var entries = List.of(entry("missing-b", OptionalLong.of(2), true),
                entry("normal-b", OptionalLong.of(2), false), entry("missing-a", OptionalLong.of(2), true),
                entry("normal-a", OptionalLong.of(2), false));

        assertNames(List.of("missing-a", "missing-b", "normal-a", "normal-b"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds,
                        Comparator.comparing(Entry::name), true, false));
    }

    @Test
    void arrivingStatsReorderOnlyInsidePriorityGroups() {
        var entries = List.of(entry("normal", OptionalLong.empty(), false),
                entry("missing-unknown", OptionalLong.empty(), true),
                entry("missing-known", OptionalLong.of(5), true));

        assertNames(List.of("missing-known", "missing-unknown", "normal"),
                TtcSort.copyPrioritizedSorted(entries, Entry::priority, Entry::seconds,
                        Comparator.comparing(Entry::name), true, true));
        var refreshed = List.of(entry("normal", OptionalLong.of(20), false),
                entry("missing-unknown", OptionalLong.of(10), true),
                entry("missing-known", OptionalLong.of(5), true));
        assertNames(List.of("missing-unknown", "missing-known", "normal"),
                TtcSort.copyPrioritizedSorted(refreshed, Entry::priority, Entry::seconds,
                        Comparator.comparing(Entry::name), true, true));
    }

    private static void assertNames(List<String> expected, List<Entry> entries) {
        assertEquals(expected, entries.stream().map(Entry::name).toList());
    }

    private static Entry entry(String name, OptionalLong seconds) {
        return entry(name, seconds, false);
    }

    private static Entry entry(String name, OptionalLong seconds, boolean priority) {
        return new Entry(name, seconds, priority);
    }

    private record Entry(String name, OptionalLong seconds, boolean priority) {
    }
}
