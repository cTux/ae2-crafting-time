package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record StatsSnapshotS2C(List<StatsEntry> entries) implements CustomPacketPayload {
    public static final Type<StatsSnapshotS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "stats_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatsSnapshotS2C> STREAM_CODEC = StreamCodec.ofMember(
            StatsSnapshotS2C::encode,
            StatsSnapshotS2C::decode);

    @Override
    public Type<StatsSnapshotS2C> type() {
        return TYPE;
    }

    public static void encode(StatsSnapshotS2C packet, FriendlyByteBuf buffer) {
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
        }
    }

    public static StatsSnapshotS2C decode(FriendlyByteBuf buffer) {
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
            entries.add(new StatsEntry(key, new ProfileStats(sampleCount, averageDurationTicks, amountPerTick,
                    amountPerSecond, lastDurationTicks, unit)));
        }
        return new StatsSnapshotS2C(entries);
    }

    public static void handle(StatsSnapshotS2C packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientStats.CACHE.replace(packet.entries));
    }
}
