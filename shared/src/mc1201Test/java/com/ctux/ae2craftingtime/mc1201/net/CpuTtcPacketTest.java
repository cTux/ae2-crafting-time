package com.ctux.ae2craftingtime.mc1201.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ctux.ae2craftingtime.core.CpuTtcCache;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

class CpuTtcPacketTest {
    @Test
    void roundTripsEmptyAndMaximumRequests() {
        for (var serials : List.of(List.<Integer>of(),
                java.util.stream.IntStream.rangeClosed(1, CpuTtcCache.MAX_CPUS).boxed().toList())) {
            var packet = new CpuTtcPacketCodec.Request(4, 5, 6, serials);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            CpuTtcPacketCodec.writeRequest(buffer, packet);
            assertEquals(packet, CpuTtcPacketCodec.readRequest(buffer));
        }
    }

    @Test
    void roundTripsPresentAndMissingSnapshotEntries() {
        var packet = new CpuTtcPacketCodec.Snapshot(5, 6, List.of(
                new CpuTtcCache.Entry(1, OptionalLong.of(9)),
                new CpuTtcCache.Entry(2, OptionalLong.empty())));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        CpuTtcPacketCodec.writeSnapshot(buffer, packet);
        assertEquals(packet, CpuTtcPacketCodec.readSnapshot(buffer));
        for (var entries : List.of(List.<CpuTtcCache.Entry>of(),
                java.util.stream.IntStream.rangeClosed(1, CpuTtcCache.MAX_CPUS)
                        .mapToObj(serial -> new CpuTtcCache.Entry(serial, OptionalLong.of(Long.MAX_VALUE))).toList())) {
            var boundary = new CpuTtcPacketCodec.Snapshot(0, Long.MAX_VALUE, entries);
            var boundaryBuffer = new FriendlyByteBuf(Unpooled.buffer());
            CpuTtcPacketCodec.writeSnapshot(boundaryBuffer, boundary);
            assertEquals(boundary, CpuTtcPacketCodec.readSnapshot(boundaryBuffer));
        }
    }

    @Test
    void rejectsInvalidContextsCountsSerialsSecondsAndTruncation() {
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Request(-1, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Request(0, -1, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Request(0, 0, -1, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Request(0, 0, 0, List.of(1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Request(0, 0, 0, List.of(0)));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Request(0, 0, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Request(0, 0, 0, java.util.Arrays.asList((Integer) null)));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Request(0, 0, 0,
                        java.util.stream.IntStream.rangeClosed(1, 33).boxed().toList()));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Snapshot(-1, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Snapshot(0, -1, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Snapshot(0, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new CpuTtcPacketCodec.Snapshot(0, 0,
                        java.util.stream.IntStream.rangeClosed(1, 33)
                                .mapToObj(serial -> new CpuTtcCache.Entry(serial, OptionalLong.empty())).toList()));
        assertThrows(IllegalArgumentException.class, () -> new CpuTtcPacketCodec.Snapshot(0, 0, List.of(
                new CpuTtcCache.Entry(1, OptionalLong.empty()),
                new CpuTtcCache.Entry(1, OptionalLong.of(2)))));

        var oversized = new FriendlyByteBuf(Unpooled.buffer());
        oversized.writeVarInt(0);
        oversized.writeVarLong(0);
        oversized.writeVarLong(0);
        oversized.writeVarInt(CpuTtcCache.MAX_CPUS + 1);
        assertThrows(IllegalArgumentException.class, () -> CpuTtcPacketCodec.readRequest(oversized));

        var negativeRequestCount = new FriendlyByteBuf(Unpooled.buffer());
        negativeRequestCount.writeVarInt(0);
        negativeRequestCount.writeVarLong(0);
        negativeRequestCount.writeVarLong(0);
        negativeRequestCount.writeVarInt(-1);
        assertThrows(IllegalArgumentException.class, () -> CpuTtcPacketCodec.readRequest(negativeRequestCount));

        var oversizedSnapshot = new FriendlyByteBuf(Unpooled.buffer());
        oversizedSnapshot.writeVarLong(0);
        oversizedSnapshot.writeVarLong(0);
        oversizedSnapshot.writeVarInt(CpuTtcCache.MAX_CPUS + 1);
        assertThrows(IllegalArgumentException.class, () -> CpuTtcPacketCodec.readSnapshot(oversizedSnapshot));

        var negativeSnapshotCount = new FriendlyByteBuf(Unpooled.buffer());
        negativeSnapshotCount.writeVarLong(0);
        negativeSnapshotCount.writeVarLong(0);
        negativeSnapshotCount.writeVarInt(-1);
        assertThrows(IllegalArgumentException.class, () -> CpuTtcPacketCodec.readSnapshot(negativeSnapshotCount));

        var negativeSeconds = new FriendlyByteBuf(Unpooled.buffer());
        negativeSeconds.writeVarLong(0);
        negativeSeconds.writeVarLong(0);
        negativeSeconds.writeVarInt(1);
        negativeSeconds.writeVarInt(1);
        negativeSeconds.writeBoolean(true);
        negativeSeconds.writeVarLong(-1);
        assertThrows(IllegalArgumentException.class, () -> CpuTtcPacketCodec.readSnapshot(negativeSeconds));
        assertThrows(RuntimeException.class,
                () -> CpuTtcPacketCodec.readRequest(new FriendlyByteBuf(Unpooled.buffer())));
        var truncatedRequest = new FriendlyByteBuf(Unpooled.buffer());
        truncatedRequest.writeVarInt(0);
        truncatedRequest.writeVarLong(0);
        truncatedRequest.writeVarLong(0);
        truncatedRequest.writeVarInt(1);
        assertThrows(RuntimeException.class, () -> CpuTtcPacketCodec.readRequest(truncatedRequest));
        var truncatedSnapshot = new FriendlyByteBuf(Unpooled.buffer());
        truncatedSnapshot.writeVarLong(0);
        truncatedSnapshot.writeVarLong(0);
        truncatedSnapshot.writeVarInt(1);
        truncatedSnapshot.writeVarInt(1);
        assertThrows(RuntimeException.class, () -> CpuTtcPacketCodec.readSnapshot(truncatedSnapshot));
    }
}
