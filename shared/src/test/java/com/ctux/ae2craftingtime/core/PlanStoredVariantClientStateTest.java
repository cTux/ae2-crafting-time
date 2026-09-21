package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.BitSet;
import org.junit.jupiter.api.Test;

class PlanStoredVariantClientStateTest {
    @Test void appliesReplacementPerChunkDespiteReorderingAndRejectsStaleIdentities() {
        var state = new PlanStoredVariantClientState();
        state.reset(257);
        var applied = new ArrayList<String>();
        var replace = (java.util.function.BiConsumer<Integer, Boolean>) (row, value) -> applied.add(row + ":" + value);
        var second = chunk(7, 2, 257, 256, 1, true);
        var first = chunk(7, 2, 257, 0, 256, true);
        assertTrue(state.apply(second, 3, 7, 2, 257, replace));
        assertTrue(state.apply(first, 2, 7, 2, 257, replace));
        assertTrue(applied.contains("256:true"));
        assertTrue(applied.contains("0:true"));
        assertFalse(state.apply(second, 3, 7, 2, 257, replace));
        assertFalse(state.apply(first, 1, 7, 2, 257, replace));
        assertFalse(state.apply(first, 0, 7, 2, 257, replace));
        assertFalse(state.apply(first, 4, 8, 2, 257, replace));
        assertFalse(state.apply(first, 4, 7, 3, 257, replace));
        assertFalse(state.apply(first, 4, 7, 2, 256, replace));
        assertTrue(state.apply(chunk(7, 2, 257, 256, 1, false), 4, 7, 2, 257, replace));
        assertEquals("256:false", applied.get(applied.size() - 1));
        state.reset(257);
        assertTrue(state.apply(first, 1, 7, 2, 257, replace));
        state.reset(1);
        assertFalse(state.apply(second, 5, 7, 2, 257, replace));
        state.reset(0);
        assertFalse(state.apply(first, 5, 7, 2, 257, replace));
    }

    @Test void rejectsMalformedChunkBoundariesAndTrailingBits() {
        var state = new PlanStoredVariantClientState();
        state.reset(1);
        var trailing = new byte[32]; trailing[0] = 2;
        assertFalse(state.apply(new PlanRecurrenceChunk(1, 1, 1, 0, 1, trailing), 1, 1, 1, 1,
                (row, value) -> fail()));
        assertFalse(state.apply(chunk(1, 1, 1, 0, 1, true), 1, 1, 1, 0,
                (row, value) -> fail()));
        assertFalse(state.apply(chunk(1, 1, 1, 0, 1, true), -1, 1, 1, 1,
                (row, value) -> fail()));
    }

    private static PlanRecurrenceChunk chunk(int container, long revision, int entries, int offset,
            int rowCount, boolean set) {
        var bits = new BitSet();
        if (set) bits.set(0);
        var mask = new byte[32];
        var bytes = bits.toByteArray();
        System.arraycopy(bytes, 0, mask, 0, bytes.length);
        return new PlanRecurrenceChunk(container, revision, entries, offset, rowCount, mask);
    }
}
