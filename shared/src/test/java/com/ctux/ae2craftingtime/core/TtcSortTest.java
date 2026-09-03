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

    private static Entry entry(String name, OptionalLong seconds) {
        return new Entry(name, seconds);
    }

    private record Entry(String name, OptionalLong seconds) {
    }
}
