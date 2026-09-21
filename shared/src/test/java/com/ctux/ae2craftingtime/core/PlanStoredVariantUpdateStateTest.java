package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanStoredVariantUpdateStateTest {
    @Test void sendsOnlyChangedChunksIncludingZeroClears() {
        var state = new PlanStoredVariantUpdateState();
        state.reset(257);
        assertTrue(state.replace(Set.of()).isEmpty());
        var first = state.replace(Set.of(0, 256));
        assertEquals(2, first.size());
        assertEquals(1, first.get(0).revision());
        assertEquals(1, first.get(1).revision());
        assertEquals(256, first.get(1).offset());
        assertEquals(1, first.get(1).rowCount());
        assertTrue(state.replace(Set.of(0, 256)).isEmpty());
        var clear = state.replace(Set.of(256));
        assertEquals(1, clear.size());
        assertEquals(0, clear.get(0).offset());
        assertEquals(2, clear.get(0).revision());
        assertTrue(clear.get(0).mask()[0] == 0);
        state.reset(1);
        assertTrue(state.replace(Set.of()).isEmpty());
        assertEquals(1, state.replace(Set.of(0)).size());
        state.reset(256);
        assertEquals(1, state.replace(Set.of(255)).size());
        state.reset(0);
        assertTrue(state.replace(Set.of()).isEmpty());
        assertFalse(state.exhausted());
    }

    @Test void neverWrapsUpdateRevision() throws Exception {
        var state = new PlanStoredVariantUpdateState();
        state.reset(1);
        var field = PlanStoredVariantUpdateState.class.getDeclaredField("revision");
        field.setAccessible(true);
        field.setLong(state, Long.MAX_VALUE);
        assertTrue(state.replace(Set.of(0)).isEmpty());
        assertTrue(state.exhausted());
        assertTrue(state.replace(Set.of(0)).isEmpty());
        state.reset(1);
        assertFalse(state.exhausted());
    }

    @Test void terminalRevisionClearsOnlyOutstandingWarningsOnTheClient() throws Exception {
        var state = new PlanStoredVariantUpdateState();
        state.reset(769);
        state.replace(Set.of(0, 256, 512));
        state.replace(Set.of(0, 512));
        var client = new PlanStoredVariantClientState();
        client.reset(769);
        var flags = new java.util.BitSet(); flags.set(0); flags.set(512);
        var field = PlanStoredVariantUpdateState.class.getDeclaredField("revision");
        field.setAccessible(true);
        field.setLong(state, Long.MAX_VALUE - 1);
        var clears = state.replace(Set.of(0, 512));
        assertEquals(2, clears.size());
        for (var clear : clears) {
            assertEquals(Long.MAX_VALUE, clear.revision());
            assertTrue(client.apply(new PlanRecurrenceChunk(1, 1, 769, clear.offset(), clear.rowCount(), clear.mask()),
                    clear.revision(), 1, 1, 769, flags::set));
        }
        assertTrue(flags.isEmpty());
        assertTrue(state.exhausted());
        assertTrue(state.replace(Set.of(0)).isEmpty());
    }
}
