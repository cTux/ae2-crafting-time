package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.CpuTtcCache;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;

public final class CpuTtcPacketCodec {
    public static void writeRequest(FriendlyByteBuf buffer, Request request) {
        buffer.writeVarInt(request.containerId());
        buffer.writeVarLong(request.session());
        buffer.writeVarLong(request.sequence());
        buffer.writeVarInt(request.serials().size());
        request.serials().forEach(buffer::writeVarInt);
    }

    public static Request readRequest(FriendlyByteBuf buffer) {
        var container = buffer.readVarInt();
        var session = buffer.readVarLong();
        var sequence = buffer.readVarLong();
        return new Request(container, session, sequence, readSerials(buffer));
    }

    public static void writeSnapshot(FriendlyByteBuf buffer, Snapshot snapshot) {
        buffer.writeVarLong(snapshot.session());
        buffer.writeVarLong(snapshot.sequence());
        buffer.writeVarInt(snapshot.entries().size());
        for (var entry : snapshot.entries()) {
            buffer.writeVarInt(entry.serial());
            buffer.writeBoolean(entry.seconds().isPresent());
            entry.seconds().ifPresent(buffer::writeVarLong);
        }
    }

    public static Snapshot readSnapshot(FriendlyByteBuf buffer) {
        var session = buffer.readVarLong();
        var sequence = buffer.readVarLong();
        var count = checkedCount(buffer.readVarInt());
        var entries = new ArrayList<CpuTtcCache.Entry>(count);
        for (var i = 0; i < count; i++) {
            var serial = buffer.readVarInt();
            var seconds = buffer.readBoolean() ? OptionalLong.of(buffer.readVarLong()) : OptionalLong.empty();
            entries.add(new CpuTtcCache.Entry(serial, seconds));
        }
        return new Snapshot(session, sequence, entries);
    }

    private static List<Integer> readSerials(FriendlyByteBuf buffer) {
        var count = checkedCount(buffer.readVarInt());
        var serials = new ArrayList<Integer>(count);
        for (var i = 0; i < count; i++) {
            serials.add(buffer.readVarInt());
        }
        return serials;
    }

    private static int checkedCount(int count) {
        if (count < 0 || count > CpuTtcCache.MAX_CPUS) {
            throw new IllegalArgumentException("CPU TTC entry count is out of bounds");
        }
        return count;
    }

    private static List<Integer> checkedSerials(List<Integer> serials) {
        if (serials == null || serials.size() > CpuTtcCache.MAX_CPUS || new HashSet<>(serials).size() != serials.size()
                || serials.stream().anyMatch(serial -> serial == null || serial <= 0)) {
            throw new IllegalArgumentException("invalid CPU serials");
        }
        return List.copyOf(serials);
    }

    public record Request(int containerId, long session, long sequence, List<Integer> serials) {
        public Request {
            if (containerId < 0 || session < 0 || sequence < 0) {
                throw new IllegalArgumentException("invalid CPU TTC request context");
            }
            serials = checkedSerials(serials);
        }
    }

    public record Snapshot(long session, long sequence, List<CpuTtcCache.Entry> entries) {
        public Snapshot {
            if (entries == null || session < 0 || sequence < 0 || entries.size() > CpuTtcCache.MAX_CPUS
                    || entries.stream().map(CpuTtcCache.Entry::serial).distinct().count() != entries.size()) {
                throw new IllegalArgumentException("invalid CPU TTC snapshot");
            }
            entries = List.copyOf(entries);
        }
    }

    private CpuTtcPacketCodec() {
    }
}
