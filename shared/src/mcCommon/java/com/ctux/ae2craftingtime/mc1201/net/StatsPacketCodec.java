package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.core.TtcAccuracyStats;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StatsPacketCodec {
    private StatsPacketCodec() {
    }

    public static void writeKeys(FriendlyByteBuf buffer, List<String> keys) {
        buffer.writeVarInt(keys.size());
        keys.forEach(key -> buffer.writeUtf(key, PacketLimits.MAX_OUTPUT_ID_LENGTH));
    }

    public static List<String> readKeys(FriendlyByteBuf buffer, String label) {
        var size = PacketLimits.checkedSize(buffer.readVarInt(), PacketLimits.MAX_KEYS, label);
        var keys = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            keys.add(buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH));
        }
        return keys;
    }

    public static void writeSnapshot(FriendlyByteBuf buffer, Snapshot snapshot) {
        writeKeys(buffer, snapshot.requestedKeys());
        buffer.writeVarInt(snapshot.networkAmounts().size());
        snapshot.networkAmounts().forEach((key, amount) -> {
            buffer.writeUtf(key, PacketLimits.MAX_OUTPUT_ID_LENGTH);
            buffer.writeVarLong(amount);
        });
        buffer.writeVarInt(snapshot.entries().size());
        for (var entry : snapshot.entries()) {
            var stats = entry.stats();
            buffer.writeUtf(entry.key().outputId(), PacketLimits.MAX_OUTPUT_ID_LENGTH);
            buffer.writeEnum(stats.unit());
            buffer.writeVarInt(stats.sampleCount());
            buffer.writeDouble(stats.averageDurationTicks());
            buffer.writeDouble(stats.amountPerTick());
            buffer.writeDouble(stats.amountPerSecond());
            buffer.writeVarLong(stats.lastDurationTicks());
            buffer.writeBoolean(stats.reliableEstimate());
            buffer.writeVarInt(stats.usedSampleCount());
            buffer.writeDouble(stats.outlierMultiplier());
            buffer.writeVarInt(stats.sampleDurationTicks().size());
            stats.sampleDurationTicks().forEach(buffer::writeVarLong);
            stats.sampleAmounts().forEach(buffer::writeVarLong);
            buffer.writeBoolean(entry.accuracy().isPresent());
            entry.accuracy().ifPresent(value -> TtcAccuracyPacketCodec.write(buffer, value));
            buffer.writeBoolean(entry.stall().isPresent());
            entry.stall().ifPresent(value -> StallDiagnosticPacketCodec.write(buffer, value));
        }
    }

    public static Snapshot readSnapshot(FriendlyByteBuf buffer) {
        var requestedKeys = readKeys(buffer, "requested keys");
        var amountSize = PacketLimits.checkedSize(buffer.readVarInt(), PacketLimits.MAX_KEYS, "network amounts");
        var networkAmounts = new HashMap<String, Long>(amountSize);
        for (int i = 0; i < amountSize; i++) {
            networkAmounts.put(buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH), buffer.readVarLong());
        }
        var size = PacketLimits.checkedSize(buffer.readVarInt(), PacketLimits.MAX_KEYS, "entries");
        var entries = new ArrayList<StatsEntry>(size);
        for (int i = 0; i < size; i++) {
            var key = new ProfileKey(buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH));
            var unit = buffer.readEnum(ProfileUnit.class);
            var sampleCount = buffer.readVarInt();
            var averageDurationTicks = buffer.readDouble();
            var amountPerTick = buffer.readDouble();
            var amountPerSecond = buffer.readDouble();
            var lastDurationTicks = buffer.readVarLong();
            var reliableEstimate = buffer.readBoolean();
            var usedSampleCount = buffer.readVarInt();
            var outlierMultiplier = buffer.readDouble();
            var durationCount = PacketLimits.checkedSize(buffer.readVarInt(), PacketLimits.MAX_SAMPLES, "samples");
            var durations = new ArrayList<Long>(durationCount);
            for (int j = 0; j < durationCount; j++) {
                durations.add(buffer.readVarLong());
            }
            var amounts = new ArrayList<Long>(durationCount);
            for (int j = 0; j < durationCount; j++) {
                amounts.add(buffer.readVarLong());
            }
            var accuracy = buffer.readBoolean()
                    ? Optional.of(TtcAccuracyPacketCodec.read(buffer))
                    : Optional.<TtcAccuracyStats>empty();
            var stall = buffer.readBoolean()
                    ? Optional.of(StallDiagnosticPacketCodec.read(buffer))
                    : Optional.<com.ctux.ae2craftingtime.core.StallDiagnostic>empty();
            entries.add(new StatsEntry(key, new ProfileStats(sampleCount, averageDurationTicks, amountPerTick,
                    amountPerSecond, lastDurationTicks, unit, reliableEstimate, usedSampleCount, outlierMultiplier,
                    durations, amounts), accuracy, stall));
        }
        return new Snapshot(requestedKeys, entries, networkAmounts);
    }

    public record Snapshot(List<String> requestedKeys, List<StatsEntry> entries, Map<String, Long> networkAmounts) {
    }
}
