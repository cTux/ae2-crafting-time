package com.ctux.ae2craftingtime.mc1201.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StallDiagnostic;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.core.TtcAccuracyStats;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
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
        var packet = new StatsSnapshotS2C(List.of("minecraft:water", "minecraft:lava"), List.of(
                new StatsEntry(new ProfileKey("minecraft:water"),
                        new ProfileStats(3, 40.5, 25.0, 500.0, 42, ProfileUnit.MILLIBUCKET, false,
                                2, 4.0, List.of(10L, 20L, 42L), List.of(250L, 500L, 1000L)),
                        java.util.Optional.of(new TtcAccuracyStats(4, 3, 0.9, 2.5, 12.5, 1.1,
                                30, 33.0, 31.0, 4, 4)),
                        java.util.Optional.of(new StallDiagnostic(960, 240, 1, 1, 4))),
                new StatsEntry(new ProfileKey("minecraft:lava"),
                        new ProfileStats(1, 20, 50, 1000, 20, ProfileUnit.MILLIBUCKET))),
                Map.of("minecraft:water", 8_000L, "minecraft:lava", 0L));

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
