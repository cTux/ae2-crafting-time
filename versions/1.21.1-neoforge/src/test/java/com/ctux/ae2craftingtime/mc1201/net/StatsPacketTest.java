package com.ctux.ae2craftingtime.mc1201.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StatsEntry;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class StatsPacketTest {
    @Test
    void requestRoundTripsVisibleOutputKeys() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        var packet = new StatsRequestC2S(List.of("minecraft:iron_plate", "ae2:printed_silicon"));

        StatsRequestC2S.encode(packet, buffer);

        assertEquals(packet, StatsRequestC2S.decode(buffer));
    }

    @Test
    void resetRequestRoundTrips() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        var packet = new StatsRequestC2S(List.of("minecraft:iron_plate"), true);

        StatsRequestC2S.encode(packet, buffer);

        assertEquals(packet, StatsRequestC2S.decode(buffer));
    }

    @Test
    void snapshotRoundTripsServerCalculatedStats() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        var packet = new StatsSnapshotS2C(List.of(new StatsEntry(
                new ProfileKey("minecraft:water"),
                new ProfileStats(3, 40.5, 25.0, 500.0, 42, ProfileUnit.MILLIBUCKET, false,
                        2, 4.0, List.of(10L, 20L, 42L)))));

        StatsSnapshotS2C.encode(packet, buffer);

        assertEquals(packet, StatsSnapshotS2C.decode(buffer));
    }

    @Test
    void chatRoundTripsTwoCompactMessages() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        var packet = new StatsChatC2S(List.of("Iron Ingot x1509: ~3:33", "10 samples", "ignored"));

        StatsChatC2S.encode(packet, buffer);

        assertEquals(new StatsChatC2S(List.of("Iron Ingot x1509: ~3:33", "10 samples")),
                StatsChatC2S.decode(buffer));
        assertEquals("Iron Ingot x1509: ~3:33 | 10 samples", StatsChatC2S.component(packet.messages()).getString());
    }
}
