package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DispatchObservationTest {
    @Test
    void realAmountsAndOneAcceptedCompletedJobAreRequired() {
        var scope = new Object();
        DispatchObservation.watch("network", "output");
        DispatchObservation.amount(scope, "output", 1, true);
        DispatchObservation.finished(scope, true);
        DispatchObservation.accepted("other", "output", scope, 4);
        DispatchObservation.accepted("network", "other", scope, 4);
        assertFalse(DispatchObservation.snapshot().completedExactlyOnce());
        DispatchObservation.accepted("network", "output", scope, 4);
        DispatchObservation.amount(new Object(), "output", 99, true);
        DispatchObservation.amount(scope, "other", 99, true);
        DispatchObservation.finished(new Object(), true);
        DispatchObservation.amount(scope, "output", 4, true);
        assertFalse(DispatchObservation.snapshot().completedExactlyOnce());
        DispatchObservation.amount(scope, "output", 4, false);
        assertFalse(DispatchObservation.snapshot().completedExactlyOnce());
        DispatchObservation.finished(scope, false);
        assertFalse(DispatchObservation.snapshot().completedExactlyOnce());
        DispatchObservation.watch("network", "output");
        DispatchObservation.accepted("network", "output", scope, 4);
        DispatchObservation.amount(scope, "output", 4, true);
        DispatchObservation.amount(scope, "output", 4, false);
        DispatchObservation.finished(scope, true);
        DispatchObservation.fastPath(new Object(), 999);
        DispatchObservation.fastPath(scope, 4);
        DispatchObservation.tick(new Object());
        DispatchObservation.tick(scope);
        assertEquals(1, DispatchObservation.snapshot().ticks());
        assertEquals("java.lang.Object", DispatchObservation.snapshot().scopeType());
        assertEquals(4, DispatchObservation.snapshot().fastPathCrafts());
        assertTrue(DispatchObservation.snapshot().completedExactlyOnce());
        DispatchObservation.amount(scope, "output", 4, true);
        assertFalse(DispatchObservation.snapshot().completedExactlyOnce(), "duplicate dispatch must fail");
        assertFalse(new DispatchObservation.Snapshot(4, 4, 4, 2, 1, true, 0, "scope", 0).completedExactlyOnce());
        assertFalse(new DispatchObservation.Snapshot(4, 4, 4, 1, 2, true, 0, "scope", 0).completedExactlyOnce());
        DispatchObservation.watch(null, null);
        DispatchObservation.accepted("network", "output", scope, 4);
        assertEquals(new DispatchObservation.Snapshot(0, 0, 0, 0, 0, false, 0, "", 0),
                DispatchObservation.snapshot(), "a new case must not inherit a prior job's evidence");
    }
}
