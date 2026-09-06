package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.ProviderDispatchTracker.AttemptResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ProviderDispatchTrackerTest {
    private final Object cpu = new Object();
    private final ProfileKey output = new ProfileKey("grid", "minecraft:iron_ingot");

    @Test
    void completeAlternativesRequireOneAgreedObservedReason() {
        for (var attempt : List.of(AttemptResult.NO_TARGET, AttemptResult.INPUT_BLOCKED, AttemptResult.LOCKED)) {
            var evaluation = new ProviderDispatchTracker.Evaluation();
            evaluation.candidate();
            evaluation.busy(false);
            evaluation.attempt(attempt);
            evaluation.exhausted();
            assertEquals(reason(attempt), evaluation.result());
            assertTrue(!evaluation.succeeded());
        }

        var mixed = new ProviderDispatchTracker.Evaluation();
        mixed.candidate();
        mixed.attempt(AttemptResult.NO_TARGET);
        mixed.candidate();
        mixed.attempt(AttemptResult.LOCKED);
        mixed.exhausted();
        assertNull(mixed.result());

        var repeated = new ProviderDispatchTracker.Evaluation();
        repeated.candidate();
        repeated.attempt(AttemptResult.INPUT_BLOCKED);
        repeated.candidate();
        repeated.attempt(AttemptResult.INPUT_BLOCKED);
        repeated.exhausted();
        assertEquals(CraftingBlockReason.INPUT_BLOCKED, repeated.result());
    }

    @Test
    void successBusyUnknownEmptyAndIncompleteEvaluationsStayUnknown() {
        var success = new ProviderDispatchTracker.Evaluation();
        success.candidate();
        success.attempt(AttemptResult.SUCCESS);
        success.exhausted();
        assertTrue(success.succeeded());
        assertNull(success.result());

        var busy = new ProviderDispatchTracker.Evaluation();
        busy.candidate();
        busy.busy(true);
        busy.attempt(AttemptResult.LOCKED);
        busy.exhausted();
        assertNull(busy.result());

        var unknown = new ProviderDispatchTracker.Evaluation();
        unknown.candidate();
        unknown.attempt(AttemptResult.UNKNOWN);
        unknown.exhausted();
        assertNull(unknown.result());

        var empty = new ProviderDispatchTracker.Evaluation();
        empty.exhausted();
        assertNull(empty.result());

        var incomplete = new ProviderDispatchTracker.Evaluation();
        incomplete.candidate();
        incomplete.attempt(AttemptResult.NO_TARGET);
        assertNull(incomplete.result());
    }

    @Test
    void reasonsReplaceByPatternExpireAndUseExplicitRowPriority() {
        var tracker = new ProviderDispatchTracker();
        var other = new ProfileKey("grid", "minecraft:gold_ingot");
        tracker.observe(cpu, "target", Map.of(output, 1L, other, 0L), CraftingBlockReason.NO_TARGET, 50);
        tracker.observe(cpu, "input", Map.of(output, 1L), CraftingBlockReason.INPUT_BLOCKED, 50);
        tracker.observe(cpu, "lock", Map.of(output, 1L), CraftingBlockReason.LOCKED, 50);
        assertEquals(Map.of(output, CraftingBlockReason.LOCKED), tracker.reasons(cpu, 50));

        tracker.observe(cpu, "lock", Map.of(output, 1L), null, 50);
        assertEquals(Map.of(output, CraftingBlockReason.INPUT_BLOCKED), tracker.reasons(cpu, 50));
        tracker.observe(cpu, "input", Map.of(output, 1L), CraftingBlockReason.NO_POWER, 50);
        assertEquals(Map.of(output, CraftingBlockReason.NO_TARGET), tracker.reasons(cpu, 50));

        for (var tick : new long[] {50, 69}) {
            assertEquals(Map.of(output, CraftingBlockReason.NO_TARGET), tracker.reasons(cpu, tick));
        }
        assertTrue(tracker.reasons(cpu, 70).isEmpty());
        tracker.observe(cpu, "target", Map.of(output, 1L), CraftingBlockReason.NO_TARGET, 50);
        assertTrue(tracker.reasons(cpu, 49).isEmpty());
        assertTrue(tracker.reasons(new Object(), 50).isEmpty());
    }

    @Test
    void disabledNullAndLifecycleClearsDropObservations() {
        var tracker = new ProviderDispatchTracker();
        tracker.observe(null, "pattern", Map.of(output, 1L), CraftingBlockReason.LOCKED, 1);
        assertTrue(tracker.reasons(null, 1).isEmpty());

        tracker.setEnabled(false);
        tracker.observe(cpu, "pattern", Map.of(output, 1L), CraftingBlockReason.LOCKED, 1);
        assertTrue(tracker.reasons(cpu, 1).isEmpty());
        tracker.setEnabled(true);
        tracker.observe(cpu, "pattern", Map.of(output, -1L), CraftingBlockReason.LOCKED, 1);
        assertTrue(tracker.reasons(cpu, 1).isEmpty());

        tracker.observe(cpu, "pattern", Map.of(output, 1L), CraftingBlockReason.LOCKED, 1);
        tracker.clear(cpu);
        assertTrue(tracker.reasons(cpu, 1).isEmpty());
        tracker.observe(cpu, "pattern", Map.of(output, 1L), CraftingBlockReason.LOCKED, 1);
        tracker.clear();
        assertTrue(tracker.reasons(cpu, 1).isEmpty());
    }

    @Test
    void profilerPrioritizesExistingReasonsAndClearsEveryJobBoundary() {
        var profiler = new CraftProfiler(10);
        profiler.observeProviderDispatch(cpu, "target", Map.of(output, 1L), CraftingBlockReason.NO_TARGET, 50);
        profiler.observeDispatchPower(cpu, "power", Map.of(output, 1L), 10, 0, 50);
        assertEquals(Map.of(output, CraftingBlockReason.NO_POWER), profiler.blockReasons(cpu, 50, Set.of()));
        assertEquals(Map.of(output, CraftingBlockReason.NO_PROVIDER), profiler.blockReasons(cpu, 50, Set.of(output)));
        profiler.clearPending(cpu);

        for (var cleanup : List.<Runnable>of(() -> profiler.startWaiting(cpu, List.of(output), 50),
                () -> profiler.clearPending(cpu), () -> profiler.setEnabled(false),
                () -> profiler.loadSamples(List.of()))) {
            profiler.setEnabled(true);
            profiler.observeProviderDispatch(cpu, "pattern", Map.of(output, 1L), CraftingBlockReason.LOCKED, 50);
            assertEquals(CraftingBlockReason.LOCKED, profiler.blockReasons(cpu, 50, Set.of()).get(output));
            cleanup.run();
            assertTrue(profiler.blockReasons(cpu, 50, Set.of()).isEmpty());
        }
    }

    @Test
    void transientReasonsNeverEnterRememberedStatuses() {
        var profiler = new CraftProfiler(10);
        for (var reason : CraftingBlockReason.values()) {
            profiler.rememberBlockReason(output, reason, 50);
            var remembered = profiler.rememberedReasons().get(output);
            if (reason == CraftingBlockReason.NO_PROVIDER || reason == CraftingBlockReason.NO_POWER) {
                assertEquals(reason, remembered);
            } else {
                assertNull(remembered);
            }
            profiler.restoreStatuses(List.of());
        }
        assertTrue(profiler.snapshotStatuses().isEmpty());
    }

    private static CraftingBlockReason reason(AttemptResult result) {
        return switch (result) {
            case NO_TARGET -> CraftingBlockReason.NO_TARGET;
            case INPUT_BLOCKED -> CraftingBlockReason.INPUT_BLOCKED;
            case LOCKED -> CraftingBlockReason.LOCKED;
            default -> throw new IllegalArgumentException();
        };
    }
}
