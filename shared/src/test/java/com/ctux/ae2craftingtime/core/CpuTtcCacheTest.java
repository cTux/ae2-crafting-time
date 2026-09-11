package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CpuTtcCacheTest {
    @Test
    void bindsRepliesToTheLatestScreenRequestAndExpiresTheWholeBatch() {
        var cache = new CpuTtcCache();
        cache.open(7, 4);
        var request = cache.refresh(List.of(view(1, "iron", 10)), 1_000).orElseThrow();

        assertEquals(new CpuTtcCache.Request(4, 7, 0, List.of(1)), request);
        assertFalse(cache.apply(6, 0, List.of(entry(1, 5)), 1_001));
        assertFalse(cache.apply(7, 1, List.of(entry(1, 5)), 1_001));
        assertTrue(cache.apply(7, 0, List.of(entry(1, 5)), 1_001));
        assertEquals(5, cache.seconds(1, 4_000).orElseThrow());
        assertFalse(cache.seconds(1, 4_001).isPresent());
    }

    @Test
    void replacesBatchesAndRejectsForeignDuplicateOrIncompleteEntries() {
        var cache = new CpuTtcCache();
        cache.open(1, 2);
        cache.refresh(List.of(view(1, "a", 1), view(2, "b", 2)), 0).orElseThrow();

        assertFalse(cache.apply(1, 0, List.of(entry(1, 2), entry(3, 3)), 1));
        assertFalse(cache.apply(1, 0, List.of(entry(1, 2), entry(1, 3)), 1));
        assertFalse(cache.apply(1, 0, List.of(entry(1, 2)), 1));
        assertTrue(cache.apply(1, 0, List.of(entry(1, 2), entry(2, 0)), 1));
        assertEquals(2, cache.seconds(1, 1).orElseThrow());
        assertFalse(cache.seconds(2, 1).isPresent());
    }

    @Test
    void invalidatesChangesImmediatelyQueuesThemBehindTheUniversalSendBudgetAndRejectsOldReply() {
        var cache = new CpuTtcCache();
        cache.open(1, 2);
        cache.refresh(List.of(view(1, "same", 4, 20), view(2, "old", 4, 20), view(3, "idle", 4, 20)), 0);
        cache.apply(1, 0, List.of(entry(1, 10), entry(2, 20), entry(3, 30)), 1);

        assertTrue(cache.refresh(List.of(view(1, "same", 4, 21), view(2, "new", 4, 1), idle(3)), 2).isEmpty());
        assertEquals(10, cache.seconds(1, 2).orElseThrow());
        assertFalse(cache.seconds(2, 2).isPresent());
        assertFalse(cache.seconds(3, 2).isPresent());
        assertFalse(cache.apply(1, 0, List.of(entry(1, 10), entry(2, 20), entry(3, 30)), 3));
        var request = cache.refresh(List.of(view(1, "same", 4, 22), view(2, "new", 4, 2), idle(3)), 1_000)
                .orElseThrow();
        assertEquals(List.of(1, 2, 3), request.serials());
        assertTrue(cache.refresh(List.of(view(1, "same", 4, 1)), 1_001).isEmpty());
        assertFalse(cache.seconds(1, 3).isPresent());
        assertFalse(cache.apply(1, request.sequence(), List.of(entry(1, 10), entry(2, 20), entry(3, 30)), 1_002));
        assertEquals(List.of(1), cache.refresh(List.of(view(1, "same", 4, 1)), 2_000).orElseThrow().serials());
    }

    @Test
    void amountOnlyChangeAndMembershipChangeInvalidateWithoutBypassingOneSecond() {
        var cache = new CpuTtcCache();
        cache.open(1, 8);
        cache.refresh(List.of(view(1, "same", 4, 10)), 0).orElseThrow();
        assertTrue(cache.apply(1, 0, List.of(entry(1, 12)), 1));
        assertTrue(cache.refresh(List.of(view(1, "same", 4, 1)), 50).isEmpty());
        assertFalse(cache.seconds(1, 50).isPresent());
        assertTrue(cache.refresh(List.of(view(1, "same", 8), view(2, "new", 1)), 100).isEmpty());
        assertFalse(cache.seconds(1, 100).isPresent());
        assertEquals(List.of(1, 2), cache.refresh(List.of(view(1, "same", 8), view(2, "new", 1)), 1_000)
                .orElseThrow().serials());
    }

    @Test
    void reusedContainerNeedsANewSessionAndClosedScreensRejectReplies() {
        var cache = new CpuTtcCache();
        cache.open(1, 9);
        cache.refresh(List.of(view(1, "a", 1)), 0);
        cache.open(2, 9);
        cache.refresh(List.of(view(1, "a", 1)), 0);
        assertFalse(cache.apply(1, 0, List.of(entry(1, 2)), 1));
        cache.clear();
        assertFalse(cache.apply(2, 0, List.of(entry(1, 2)), 1));
        assertFalse(cache.seconds(1, 1).isPresent());
    }

    @Test
    void refreshesOncePerSecondAndCapsDeduplicatedSerialsInCallerOrder() {
        var cache = new CpuTtcCache();
        cache.open(1, 2);
        var views = java.util.stream.IntStream.rangeClosed(1, 40)
                .mapToObj(i -> view(i, "job", i)).toList();

        assertEquals(32, cache.refresh(views, 0).orElseThrow().serials().size());
        assertTrue(cache.refresh(views, 999).isEmpty());
        assertEquals(1, cache.refresh(views, 1_000).orElseThrow().sequence());
        cache.clear();
        assertTrue(cache.refresh(views, 2_000).isEmpty());
    }

    @Test
    void emptyUnknownAndIdleRepliesReplaceTheBatchAndCannotBeReplayed() {
        var cache = new CpuTtcCache();
        cache.open(0, 0);
        var first = cache.refresh(List.of(view(1, "job", 1)), 0).orElseThrow();
        assertTrue(cache.refresh(List.of(view(1, "job", 1), idle(2)), 1).isEmpty());
        assertFalse(cache.apply(0, first.sequence(), List.of(entry(1, 10)), 2));
        var next = cache.refresh(List.of(view(1, "job", 1), idle(2)), 1_000).orElseThrow();
        assertTrue(cache.apply(0, next.sequence(), List.of(
                new CpuTtcCache.Entry(1, OptionalLong.empty()), entry(2, 20)), 1_001));
        assertFalse(cache.seconds(1, 1_002).isPresent());
        assertFalse(cache.seconds(2, 1_002).isPresent());
        assertFalse(cache.apply(0, next.sequence(), List.of(entry(1, 10), entry(2, 20)), 1_002));
        var empty = cache.refresh(List.of(), 2_000).orElseThrow();
        assertTrue(cache.apply(0, empty.sequence(), List.of(), 2_001));
        assertTrue(cache.refresh(List.of(), 2_002).isEmpty());
    }

    @Test
    void validatesScreenViewsAndEntries() {
        var cache = new CpuTtcCache();
        assertThrows(IllegalArgumentException.class, () -> cache.open(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> cache.open(0, -1));
        cache.open(0, 0);
        assertThrows(IllegalArgumentException.class, () -> cache.refresh(List.of(view(0, "x", 1)), 0));
        assertThrows(IllegalArgumentException.class, () -> cache.refresh(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.CpuView(1, "x", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.CpuView(1, "x", 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.Entry(0, OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.Entry(1, OptionalLong.of(-1)));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.Entry(1, null));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.Request(0, 0, 0, null));
        assertFalse(cache.apply(0, 0, null, 0));
    }

    private static CpuTtcCache.CpuView view(int serial, String job, long amount) {
        return view(serial, job, amount, 0);
    }

    private static CpuTtcCache.CpuView view(int serial, String job, long amount, long elapsed) {
        return new CpuTtcCache.CpuView(serial, job, amount, elapsed);
    }

    private static CpuTtcCache.CpuView idle(int serial) {
        return new CpuTtcCache.CpuView(serial, null, 0, 0);
    }

    private static CpuTtcCache.Entry entry(int serial, long seconds) {
        return new CpuTtcCache.Entry(serial, OptionalLong.of(seconds));
    }
}
