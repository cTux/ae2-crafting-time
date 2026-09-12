package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CpuTtcCacheTest {
    @Test
    void prioritizesSelectedAndVisibleRowsThenCoversEveryBusyCpuFairly() {
        for (var count : List.of(0, 1, 25, 26, 32, 33, 100, 1_000)) {
            var cache = open();
            cache.observe(views(count), true, 0);
            var priorities = IntStream.rangeClosed(1, Math.min(7, count)).boxed().toList();
            var seen = new HashSet<Integer>();
            var rounds = Math.max(1, (count + CpuTtcCache.BACKGROUND_SLOTS - 1)
                    / CpuTtcCache.BACKGROUND_SLOTS);
            for (var round = 0; round < rounds; round++) {
                var request = cache.refresh(priorities, round * 1_000L);
                if (count == 0) {
                    assertTrue(request.isEmpty());
                    continue;
                }
                var actual = request.orElseThrow();
                assertTrue(actual.serials().size() <= CpuTtcCache.MAX_CPUS);
                assertEquals(priorities, actual.serials().subList(0, priorities.size()));
                seen.addAll(actual.serials());
                assertTrue(cache.apply(1, actual.sequence(), unknown(actual.serials()), round * 1_000L + 1));
            }
            assertEquals(count, seen.size());
        }
    }

    @Test
    void deduplicatesPrioritiesCapsThemAndStillAdvancesPastPriorityQueueMembers() {
        var cache = open();
        cache.observe(views(40), true, 0);
        var first = cache.refresh(List.of(40, 40, 39, 38, 37, 36, 35, 34, 33), 0).orElseThrow();
        assertEquals(List.of(40, 39, 38, 37, 36, 35, 34), first.serials().subList(0, 7));
        assertEquals(32, first.serials().size());
        assertTrue(cache.apply(1, first.sequence(), unknown(first.serials()), 1));
        var second = cache.refresh(List.of(40, 39, 38, 37, 36, 35, 34), 1_000).orElseThrow();
        assertTrue(second.serials().containsAll(List.of(26, 27, 28, 29, 30, 31, 32, 33)));
    }

    @Test
    void keepsOneOutstandingRequestAndContinuesAfterTimeoutWithoutBursting() {
        var cache = open();
        cache.observe(views(33), true, 0);
        var first = cache.refresh(List.of(1), 0).orElseThrow();
        assertTrue(cache.refresh(List.of(2), 1_000).isEmpty());
        assertTrue(cache.refresh(List.of(2), 2_999).isEmpty());
        var second = cache.refresh(List.of(2), 3_000).orElseThrow();
        assertEquals(first.sequence() + 1, second.sequence());
        assertTrue(cache.refresh(List.of(2), 3_001).isEmpty());
        assertFalse(cache.apply(1, first.sequence(), unknown(first.serials()), 3_001));
        assertFalse(cache.apply(1, second.sequence(), unknown(second.serials()), 6_000));
        assertTrue(cache.refresh(List.of(2), 6_000).isPresent());
    }

    @Test
    void mergesBatchesClearsExplicitUnknownAndRetainsUnrelatedValues() {
        var cache = open();
        cache.observe(views(40), true, 0);
        var first = cache.refresh(List.of(1), 0).orElseThrow();
        assertTrue(cache.apply(1, first.sequence(), seconds(first.serials(), 10), 1));
        assertEquals(10, cache.seconds(1, 1).orElseThrow());
        assertTrue(cache.refresh(List.of(33), 999).isEmpty());
        var second = cache.refresh(List.of(33), 1_000).orElseThrow();
        var reply = seconds(second.serials(), 20);
        reply.set(reply.indexOf(reply.stream().filter(entry -> entry.serial() == 33).findFirst().orElseThrow()),
                new CpuTtcCache.Entry(33, OptionalLong.empty()));
        assertTrue(cache.apply(1, second.sequence(), reply, 1_001));
        assertEquals(10, cache.seconds(25, 1_001).orElseThrow());
        assertEquals(20, cache.seconds(1, 1_001).orElseThrow());
        assertFalse(cache.seconds(33, 1_001).isPresent());
    }

    @Test
    void invalidatesOnlyChangedJobsFromAnOtherwiseValidInFlightReply() {
        var cache = open();
        cache.observe(List.of(view(1, "a", 1, 10), view(2, "b", 1, 10)), true, 0);
        var request = cache.refresh(List.of(1, 2), 0).orElseThrow();
        cache.observe(List.of(view(1, "a", 1, 11), view(2, "b", 2, 0)), true, 1);
        assertTrue(cache.apply(1, request.sequence(), List.of(entry(1, 5), entry(2, 6)), 2));
        assertEquals(5, cache.seconds(1, 2).orElseThrow());
        assertFalse(cache.seconds(2, 2).isPresent());

        var next = cache.refresh(List.of(1, 2), 1_000).orElseThrow();
        cache.observe(List.of(view(1, "a", 1, 1), idle(2)), true, 1_001);
        assertTrue(cache.apply(1, next.sequence(), List.of(entry(1, 7), entry(2, 8)), 1_002));
        assertFalse(cache.seconds(1, 1_002).isPresent());
        assertFalse(cache.seconds(2, 1_002).isPresent());
    }

    @Test
    void validatesTheExactReplyBeforeConsumingIt() {
        var cache = open();
        cache.observe(List.of(view(1, "a", 1), view(2, "b", 1)), true, 0);
        var request = cache.refresh(List.of(1, 2), 0).orElseThrow();
        assertFalse(cache.apply(2, request.sequence(), List.of(entry(1, 1), entry(2, 2)), 1));
        assertFalse(cache.apply(1, request.sequence() + 1, List.of(entry(1, 1), entry(2, 2)), 1));
        assertFalse(cache.apply(1, request.sequence(), null, 1));
        assertFalse(cache.apply(1, request.sequence(), List.of(entry(1, 1)), 1));
        assertFalse(cache.apply(1, request.sequence(), List.of(entry(1, 1), entry(3, 3)), 1));
        assertFalse(cache.apply(1, request.sequence(), List.of(entry(1, 1), entry(1, 2)), 1));
        assertTrue(cache.apply(1, request.sequence(), List.of(entry(1, 1), entry(2, 2)), 1));
        assertFalse(cache.apply(1, request.sequence(), List.of(entry(1, 1), entry(2, 2)), 2));
    }

    @Test
    void usesAdaptiveExpiryThenShrinksButNeverExtendsAnExistingDeadline() {
        var cache = open();
        cache.observe(views(100), true, 0);
        var request = cache.refresh(List.of(1), 0).orElseThrow();
        assertTrue(cache.apply(1, request.sequence(), seconds(request.serials(), 10), 1));
        assertEquals(10, cache.seconds(1, 5_999).orElseThrow());
        assertFalse(cache.seconds(1, 6_001).isPresent());

        cache = open();
        cache.observe(views(1_000), true, 0);
        request = cache.refresh(List.of(1), 0).orElseThrow();
        assertTrue(cache.apply(1, request.sequence(), seconds(request.serials(), 10), 1));
        cache.observe(views(100), true, 2);
        assertEquals(10, cache.seconds(1, 5_999).orElseThrow());
        assertFalse(cache.seconds(1, 6_001).isPresent());

        cache = open();
        cache.observe(views(100), true, 0);
        request = cache.refresh(List.of(1), 0).orElseThrow();
        assertTrue(cache.apply(1, request.sequence(), seconds(request.serials(), 10), 1));
        cache.observe(views(1_000), true, 2);
        assertFalse(cache.seconds(1, 6_001).isPresent());
        exercisesDeadlineOverflow();
    }

    @Test
    void ae2ModeKeepsOnlyPriorityRowsAndRejectsBackgroundFromAnOlderMode() {
        var cache = open();
        cache.observe(views(40), true, 0);
        var first = cache.refresh(List.of(1), 0).orElseThrow();
        assertTrue(cache.apply(1, first.sequence(), seconds(first.serials(), 10), 1));
        assertTrue(cache.seconds(20, 1).isPresent());
        cache.setCollectionMode(false, 2);
        assertTrue(cache.seconds(1, 2).isPresent());
        assertFalse(cache.seconds(20, 2).isPresent());

        cache.setCollectionMode(true, 1_000);
        var second = cache.refresh(List.of(1), 1_000).orElseThrow();
        cache.setCollectionMode(false, 1_001);
        assertTrue(cache.apply(1, second.sequence(), seconds(second.serials(), 30), 1_002));
        assertEquals(30, cache.seconds(1, 1_002).orElseThrow());
        assertFalse(cache.seconds(33, 1_002).isPresent());
        cache.observe(views(40), false, 1_003);
        assertFalse(cache.seconds(1, 4_002).isPresent());
    }

    @Test
    void queueTracksMembershipChangesWithoutRestartingAtTheFirstCpu() {
        var cache = open();
        cache.observe(views(40), true, 0);
        var first = cache.refresh(List.of(), 0).orElseThrow();
        assertEquals(IntStream.rangeClosed(1, 32).boxed().toList(), first.serials());
        assertTrue(cache.apply(1, first.sequence(), unknown(first.serials()), 1));
        var changed = new ArrayList<>(views(40).subList(10, 40));
        changed.add(view(41, "job", 41));
        cache.observe(changed, true, 2);
        var second = cache.refresh(List.of(), 1_000).orElseThrow();
        assertEquals(IntStream.rangeClosed(33, 40).boxed().toList(), second.serials().subList(0, 8));
        assertTrue(second.serials().contains(41));
    }

    @Test
    void changingPrioritiesStillCoverEveryBusyCpuWithinTheFairnessBound() {
        var cache = open();
        cache.observe(views(100), true, 0);
        var seen = new HashSet<Integer>();
        for (int round = 0; round < 4; round++) {
            var priorities = IntStream.rangeClosed(round * 7 + 1, round * 7 + 7).boxed().toList();
            var request = cache.refresh(priorities, round * 1_000L).orElseThrow();
            seen.addAll(request.serials());
            assertTrue(cache.apply(1, request.sequence(), unknown(request.serials()), round * 1_000L + 1));
        }
        assertEquals(100, seen.size());
    }

    @Test
    void equalSizedReplacementRemovesOldValuesAndQueuesEveryNewMember() {
        var cache = open();
        cache.observe(views(32), true, 0);
        var first = cache.refresh(List.of(), 0).orElseThrow();
        assertTrue(cache.apply(1, first.sequence(), seconds(first.serials(), 10), 1));
        var replacement = IntStream.rangeClosed(101, 132).mapToObj(i -> view(i, "new-" + i, i)).toList();
        cache.observe(replacement, true, 2);
        assertFalse(cache.seconds(1, 2).isPresent());
        assertEquals(IntStream.rangeClosed(101, 132).boxed().toList(),
                cache.refresh(List.of(), 1_000).orElseThrow().serials());
    }

    @Test
    void removalDuringOutstandingReplyDiscardsOnlyTheRemovedMember() {
        var cache = open();
        cache.observe(List.of(view(1, "a", 1), view(2, "b", 1)), true, 0);
        var request = cache.refresh(List.of(1, 2), 0).orElseThrow();
        cache.observe(List.of(view(2, "b", 1, 1)), true, 1);
        assertTrue(cache.apply(1, request.sequence(), List.of(entry(1, 4), entry(2, 5)), 2));
        assertFalse(cache.seconds(1, 2).isPresent());
        assertEquals(5, cache.seconds(2, 2).orElseThrow());
    }

    @Test
    void changingAe2PrioritiesPrunesExistingValuesBeforeTheNextReply() {
        var cache = open();
        cache.observe(List.of(view(1, "a", 1), view(2, "b", 1)), false, 0);
        var first = cache.refresh(List.of(1, 2), 0).orElseThrow();
        assertTrue(cache.apply(1, first.sequence(), List.of(entry(1, 4), entry(2, 5)), 1));
        assertTrue(cache.seconds(2, 1).isPresent());
        assertTrue(cache.refresh(List.of(1), 1_000).isPresent());
        assertTrue(cache.seconds(1, 1_000).isPresent());
        assertFalse(cache.seconds(2, 1_000).isPresent());
    }

    @Test
    void unavailableClosedAndReopenedSessionsCannotReuseValuesOrReplies() {
        var cache = new CpuTtcCache();
        cache.observe(List.of(), true, 0);
        assertTrue(cache.refresh(List.of(), 0).isEmpty());
        cache.channelUnavailable();
        cache.open(1, 2);
        cache.observe(List.of(view(1, "a", 1)), true, 0);
        var request = cache.refresh(List.of(1), 0).orElseThrow();
        cache.channelUnavailable();
        cache.channelUnavailable();
        assertFalse(cache.apply(1, request.sequence(), List.of(entry(1, 1)), 1));
        cache.open(2, 2);
        cache.observe(List.of(view(1, "a", 1)), true, 0);
        var reopened = cache.refresh(List.of(1), 0).orElseThrow();
        assertFalse(cache.apply(1, request.sequence(), List.of(entry(1, 1)), 1));
        assertTrue(cache.apply(2, reopened.sequence(), List.of(entry(1, 2)), 1));
        cache.clear();
        assertFalse(cache.seconds(1, 1).isPresent());
    }

    @Test
    void coversAe2PriorityZeroIdleRemovalReappearanceAndUnchangedObservations() {
        var cache = new CpuTtcCache();
        cache.setCollectionMode(true, 0);
        cache.open(1, 2);
        var current = List.of(view(1, "a", 1, 1), idle(2));
        cache.observe(current, false, 0);
        cache.observe(current, false, 0);

        var first = cache.refresh(java.util.Arrays.asList(null, 99, 2, 1), 0).orElseThrow();
        assertEquals(List.of(2, 1), first.serials());
        assertTrue(cache.apply(1, first.sequence(), List.of(entry(2, 4),
                new CpuTtcCache.Entry(1, OptionalLong.empty())), 1));
        assertFalse(cache.seconds(1, 1).isPresent());
        assertFalse(cache.seconds(2, 1).isPresent());

        var second = cache.refresh(List.of(1), 1_000).orElseThrow();
        assertTrue(cache.apply(1, second.sequence(), List.of(entry(1, 0)), 1_001));
        assertFalse(cache.seconds(1, 1_001).isPresent());

        var third = cache.refresh(List.of(1), 2_000).orElseThrow();
        assertTrue(cache.apply(1, third.sequence(), List.of(entry(1, 5)), 2_001));
        cache.observe(List.of(idle(2)), false, 2_002);
        assertFalse(cache.seconds(1, 2_002).isPresent());
        cache.observe(List.of(view(1, "a", 1), idle(2)), false, 2_003);
        cache.setCollectionMode(false, 2_003);
        assertTrue(cache.refresh(List.of(), 3_000).isEmpty());
    }

    @Test
    void validatesInputsAndCopiesPublicRecords() {
        var cache = new CpuTtcCache();
        assertThrows(IllegalArgumentException.class, () -> cache.open(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> cache.open(0, -1));
        cache.open(0, 0);
        assertThrows(IllegalArgumentException.class, () -> cache.observe(null, true, 0));
        assertThrows(IllegalArgumentException.class, () -> cache.observe(List.of(view(0, "x", 1)), true, 0));
        cache.observe(List.of(view(1, "first", 1), view(1, "ignored", 2)), true, 0);
        assertThrows(IllegalArgumentException.class, () -> cache.refresh(null, 0));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.CpuView(1, "x", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.CpuView(1, "x", 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.Entry(0, OptionalLong.empty()));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.Entry(1, OptionalLong.of(-1)));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.Entry(1, null));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcCache.Request(0, 0, 0, null));
        var serials = new ArrayList<>(List.of(1));
        var request = new CpuTtcCache.Request(0, 0, 0, serials);
        serials.add(2);
        assertEquals(List.of(1), request.serials());
        assertTrue(view(1, "x", 1).busy());
        assertFalse(idle(1).busy());
        assertTrue(view(1, "x", 1).sameJob(view(1, "x", 1, 2)));
        assertFalse(view(1, "x", 1).sameJob(view(1, "y", 1)));
        assertFalse(view(1, "x", 1).sameJob(view(1, "x", 2)));
    }

    @Test
    void completionAndChannelLossClearPreviouslyAcceptedTotals() {
        var cache = open();
        cache.observe(List.of(view(1, "a", 1), view(2, "b", 1)), true, 0);
        var request = cache.refresh(List.of(), 0).orElseThrow();
        assertTrue(cache.apply(1, request.sequence(), List.of(entry(1, 4), entry(2, 5)), 1));
        cache.observe(List.of(idle(1), view(2, "b", 1)), true, 2);
        assertFalse(cache.seconds(1, 2).isPresent());
        assertEquals(5, cache.seconds(2, 2).orElseThrow());
        cache.channelUnavailable();
        assertTrue(cache.snapshot(2).seconds().isEmpty());
    }

    private static CpuTtcCache open() {
        var cache = new CpuTtcCache();
        cache.open(1, 2);
        return cache;
    }

    private static List<CpuTtcCache.CpuView> views(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> view(i, "job-" + i, i)).toList();
    }

    private static List<CpuTtcCache.Entry> unknown(List<Integer> serials) {
        return serials.stream().map(serial -> new CpuTtcCache.Entry(serial, OptionalLong.empty())).toList();
    }

    private static ArrayList<CpuTtcCache.Entry> seconds(List<Integer> serials, long seconds) {
        return new ArrayList<>(serials.stream().map(serial -> entry(serial, seconds)).toList());
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

    private static void exercisesDeadlineOverflow() {
        var cache = open();
        cache.observe(List.of(view(1, "a", 1)), true, Long.MAX_VALUE - 1);
        var request = cache.refresh(List.of(1), Long.MAX_VALUE - 1).orElseThrow();
        assertTrue(cache.apply(1, request.sequence(), List.of(entry(1, 1)), Long.MAX_VALUE - 1));
        assertEquals(1, cache.seconds(1, Long.MAX_VALUE - 1).orElseThrow());
    }
}
