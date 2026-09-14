package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecurrentMissingTest {
    @Test void clearsOnlyEntriesOfAnAvailablePlan() {
        var cleared = new java.util.ArrayList<String>();
        RecurrentMissing.clearPlan(null, ignored -> {
            fail("A cleared plan has no entries to inspect");
            return Set.<String>of();
        }, cleared::add);
        assertTrue(cleared.isEmpty());
        RecurrentMissing.clearPlan(java.util.List.of("first", "second"), value -> value, cleared::add);
        assertEquals(java.util.List.of("first", "second"), cleared);
        RecurrentMissing.clearPlan(java.util.List.<String>of(), value -> value, cleared::add);
        assertEquals(java.util.List.of("first", "second"), cleared);
    }

    @Test void classifiesOnlyTerminalPositiveRecurrence() {
        assertTrue(RecurrentMissing.record(true, false, 1));
        assertFalse(RecurrentMissing.record(false, false, 1));
        assertFalse(RecurrentMissing.record(true, true, 1));
        assertFalse(RecurrentMissing.record(true, false, 0));
    }

    @Test void intersectsOnlyFinalSimulatedMissingKeys() {
        assertEquals(Set.of("a"), RecurrentMissing.finalKeys(true, Set.of("a", "b"), Set.of("a", "c")));
        assertEquals(Set.of(), RecurrentMissing.finalKeys(false, Set.of("a"), Set.of("a")));
        assertEquals(Set.of(), RecurrentMissing.finalKeys(true, Set.of(), Set.of("a")));
        assertEquals(Set.of(), RecurrentMissing.finalKeys(true, Set.of("a"), Set.of()));
    }
}
