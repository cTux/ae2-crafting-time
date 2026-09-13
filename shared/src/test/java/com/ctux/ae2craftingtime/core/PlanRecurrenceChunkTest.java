package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.BitSet;
import org.junit.jupiter.api.Test;

class PlanRecurrenceChunkTest {
    @Test void validatesIdentityBoundsAndTrailingBits() {
        var bits = new BitSet(); bits.set(0); bits.set(255);
        var full = new PlanRecurrenceChunk(4, 2, 257, 0, 256, fixed(bits));
        assertTrue(full.validFor(4, 2, 257));
        assertFalse(full.validFor(5, 2, 257));
        assertFalse(full.validFor(4, 3, 257));
        assertFalse(full.validFor(4, 2, 258));
        assertFalse(new PlanRecurrenceChunk(4, 0, 1, 0, 1, fixed(new BitSet())).validFor(4, 0, 1));
        assertFalse(new PlanRecurrenceChunk(4, 2, 1, -256, 1, fixed(new BitSet())).validFor(4, 2, 1));
        assertFalse(new PlanRecurrenceChunk(4, 2, 257, 1, 256, fixed(new BitSet())).validFor(4, 2, 257));
        assertFalse(new PlanRecurrenceChunk(4, 2, 1, 0, 0, fixed(new BitSet())).validFor(4, 2, 1));
        assertFalse(new PlanRecurrenceChunk(4, 2, 257, 0, 257, fixed(new BitSet())).validFor(4, 2, 257));
        assertFalse(new PlanRecurrenceChunk(4, 2, 1, 2, 1, fixed(new BitSet())).validFor(4, 2, 1));
        assertFalse(new PlanRecurrenceChunk(4, 2, 1, 256, 1, fixed(new BitSet())).validFor(4, 2, 1));
        assertFalse(new PlanRecurrenceChunk(4, 2, 257, 0, 255, fixed(new BitSet())).validFor(4, 2, 257));
        assertFalse(new PlanRecurrenceChunk(4, 2, 1, 0, 1, new byte[31]).validFor(4, 2, 1));
        var trailing = new BitSet(); trailing.set(1);
        assertFalse(new PlanRecurrenceChunk(4, 2, 1, 0, 1, fixed(trailing)).validFor(4, 2, 1));
        assertTrue(new PlanRecurrenceChunk(4, 2, 257, 256, 1, fixed(new BitSet())).validFor(4, 2, 257));
    }

    @Test void defensivelyCopiesMask() {
        var mask = new byte[32]; mask[0] = 1;
        var chunk = new PlanRecurrenceChunk(1, 1, 1, 0, 1, mask); mask[0] = 0;
        assertTrue(chunk.rows().get(0));
        var copy = chunk.mask(); copy[0] = 0;
        assertTrue(chunk.rows().get(0));
    }

    private static byte[] fixed(BitSet bits) {
        var result = new byte[PlanRecurrenceChunk.MASK_BYTES];
        var bytes = bits.toByteArray(); System.arraycopy(bytes, 0, result, 0, bytes.length);
        return result;
    }
}
