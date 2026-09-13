package com.ctux.ae2craftingtime.mc1201.net;

import static org.junit.jupiter.api.Assertions.*;

import java.util.BitSet;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class PlanRecurrencePacketTest {
    @Test void dropsOverlongAndTrailingPayloadsWithoutLosingTheNativePlan() {
        for (boolean trailing : new boolean[] {false, true}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                if (trailing) {
                    PlanRecurrenceS2C.encode(PlanRecurrenceS2C.of(1, 1, 1, 0, 1, new BitSet()), buffer);
                    buffer.writeByte(1);
                } else {
                    for (int i = 0; i < 8; i++) buffer.writeByte(0x80);
                }
                assertFalse(PlanRecurrenceS2C.decode(buffer).chunk().validFor(1, 1, 1));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
    }
    @Test void preservesSetBitsAtEveryChunkBoundary() {
        for (int count : new int[] {1, 256, 257}) {
            for (int offset = 0; offset < count; offset += 256) {
                int rows = Math.min(256, count - offset);
                var bits = new BitSet();
                bits.set(0);
                bits.set(rows - 1);
                var packet = PlanRecurrenceS2C.of(12, 3, count, offset, rows, bits);
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    PlanRecurrenceS2C.encode(packet, buffer);
                    var decoded = PlanRecurrenceS2C.decode(buffer).chunk();
                    assertTrue(decoded.validFor(12, 3, count));
                    assertEquals(bits, decoded.rows());
                    assertEquals(offset, decoded.offset());
                    assertEquals(0, buffer.readableBytes());
                } finally {
                    buffer.release();
                }
            }
        }
    }

    @Test void rejectsTruncatedMask() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(1).writeVarLong(1).writeVarInt(1).writeVarInt(0).writeVarInt(1);
            buffer.writeBytes(new byte[31]);
            assertFalse(PlanRecurrenceS2C.decode(buffer).chunk().validFor(1, 1, 1));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
