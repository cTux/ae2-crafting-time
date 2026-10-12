package com.ctux.ae2craftingtime.mc1201.net;

import static org.junit.jupiter.api.Assertions.*;
import com.ctux.ae2craftingtime.core.TtcEstimate;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TtcEstimatePacketTest {
    @Test
    void roundTripsFractionalMillisecondsAndBothConfidenceStates() {
        for (boolean reliable : new boolean[] {true, false}) {
            var value = Optional.of(new TtcEstimate(1.25, reliable));
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                TtcEstimatePacketCodec.write(buffer, value);
                assertEquals(value, TtcEstimatePacketCodec.read(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally { buffer.release(); }
        }
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TtcEstimatePacketCodec.write(buffer, Optional.empty());
            assertTrue(TtcEstimatePacketCodec.read(buffer).isEmpty());
        } finally { buffer.release(); }
    }

    @Test
    void rejectsMalformedDurationsAtTheWireBoundary() {
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -1}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeBoolean(true).writeDouble(invalid).writeBoolean(true);
                assertThrows(IllegalArgumentException.class, () -> TtcEstimatePacketCodec.read(buffer));
            } finally { buffer.release(); }
        }
    }
}
