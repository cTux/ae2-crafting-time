package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.core.TtcAccuracyStats;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record StatsSnapshotS2C(List<String> requestedKeys, List<StatsEntry> entries,
        Map<String, Long> networkAmounts) {
    public StatsSnapshotS2C(List<StatsEntry> entries) {
        this(entries.stream().map(entry -> entry.key().outputId()).toList(), entries, Map.of());
    }

    public static void encode(StatsSnapshotS2C packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.requestedKeys.size());
        for (var key : packet.requestedKeys) {
            buffer.writeUtf(key);
        }
        buffer.writeVarInt(packet.networkAmounts.size());
        packet.networkAmounts.forEach((key, amount) -> {
            buffer.writeUtf(key);
            buffer.writeVarLong(amount);
        });
        buffer.writeVarInt(packet.entries.size());
        for (var entry : packet.entries) {
            var stats = entry.stats();
            buffer.writeUtf(entry.key().outputId());
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
            for (var duration : stats.sampleDurationTicks()) {
                buffer.writeVarLong(duration);
            }
            for (var amount : stats.sampleAmounts()) {
                buffer.writeVarLong(amount);
            }
            buffer.writeBoolean(entry.accuracy().isPresent());
            entry.accuracy().ifPresent(accuracy -> TtcAccuracyPacketCodec.write(buffer, accuracy));
        }
    }

    public static StatsSnapshotS2C decode(FriendlyByteBuf buffer) {
        var requestedSize = buffer.readVarInt();
        var requestedKeys = new ArrayList<String>(requestedSize);
        for (int i = 0; i < requestedSize; i++) {
            requestedKeys.add(buffer.readUtf());
        }
        var networkAmountSize = buffer.readVarInt();
        var networkAmounts = new java.util.HashMap<String, Long>(networkAmountSize);
        for (int i = 0; i < networkAmountSize; i++) {
            networkAmounts.put(buffer.readUtf(), buffer.readVarLong());
        }
        var size = buffer.readVarInt();
        var entries = new ArrayList<StatsEntry>(size);
        for (int i = 0; i < size; i++) {
            var key = new ProfileKey(buffer.readUtf());
            var unit = buffer.readEnum(ProfileUnit.class);
            var sampleCount = buffer.readVarInt();
            var averageDurationTicks = buffer.readDouble();
            var amountPerTick = buffer.readDouble();
            var amountPerSecond = buffer.readDouble();
            var lastDurationTicks = buffer.readVarLong();
            var reliableEstimate = buffer.readBoolean();
            var usedSampleCount = buffer.readVarInt();
            var outlierMultiplier = buffer.readDouble();
            var durationCount = buffer.readVarInt();
            var sampleDurationTicks = new ArrayList<Long>(durationCount);
            for (int durationIndex = 0; durationIndex < durationCount; durationIndex++) {
                sampleDurationTicks.add(buffer.readVarLong());
            }
            var sampleAmounts = new ArrayList<Long>(durationCount);
            for (int amountIndex = 0; amountIndex < durationCount; amountIndex++) {
                sampleAmounts.add(buffer.readVarLong());
            }
            var accuracy = buffer.readBoolean() ? java.util.Optional.of(TtcAccuracyPacketCodec.read(buffer))
                    : java.util.Optional.<TtcAccuracyStats>empty();
            entries.add(new StatsEntry(key, new ProfileStats(sampleCount, averageDurationTicks, amountPerTick,
                    amountPerSecond, lastDurationTicks, unit, reliableEstimate, usedSampleCount, outlierMultiplier,
                    sampleDurationTicks, sampleAmounts), accuracy));
        }
        return new StatsSnapshotS2C(requestedKeys, entries, networkAmounts);
    }

    public static void handle(StatsSnapshotS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ClientStats.CACHE.replace(packet.requestedKeys.stream().map(ProfileKey::new).toList(), packet.entries);
            ClientStats.replaceNetworkAmounts(packet.requestedKeys, packet.networkAmounts);
        });
        context.setPacketHandled(true);
    }
}
