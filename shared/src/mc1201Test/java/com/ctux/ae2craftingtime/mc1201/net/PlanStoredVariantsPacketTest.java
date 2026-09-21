package com.ctux.ae2craftingtime.mc1201.net;

import static org.junit.jupiter.api.Assertions.*;
import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class PlanStoredVariantsPacketTest {
    @Test void roundTripsReplacementAndClearMasks() {
        for (int entries : new int[] {1, 256, 257}) {
            for (int offset = 0; offset < entries; offset += 256) {
                var rows = Math.min(256, entries - offset);
                for (boolean set : new boolean[] {false, true}) {
                    var mask = new byte[32];
                    if (set) mask[0] = 1;
                    var original = new PlanRecurrenceChunk(4, 8, entries, offset, rows, mask);
                    var buffer = new FriendlyByteBuf(Unpooled.buffer());
                    try {
                        PlanStoredVariantsS2C.encode(new PlanStoredVariantsS2C(3, original), buffer);
                        var decoded = PlanStoredVariantsS2C.decode(buffer);
                        assertEquals(3, decoded.updateRevision());
                        assertTrue(decoded.chunk().validFor(4, 8, entries));
                        assertEquals(set, decoded.chunk().rows().get(0));
                        assertEquals(0, buffer.readableBytes());
                    } finally { buffer.release(); }
                }
            }
        }
    }

    @Test void rejectsMalformedAndTrailingPayloads() {
        for (int kind = 0; kind < 3; kind++) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                if (kind == 0) buffer.writeByte(0x80);
                else {
                    PlanStoredVariantsS2C.encode(new PlanStoredVariantsS2C(kind == 1 ? 0 : 1,
                            new PlanRecurrenceChunk(1, 1, 1, 0, 1, new byte[32])), buffer);
                    if (kind == 2) buffer.writeByte(1);
                }
                var decoded = PlanStoredVariantsS2C.decode(buffer);
                assertFalse(decoded.updateRevision() > 0 && decoded.chunk().validFor(1, 1, 1));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
}
