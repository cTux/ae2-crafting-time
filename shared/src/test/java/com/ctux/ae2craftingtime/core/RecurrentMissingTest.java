package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecurrentMissingTest {
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
