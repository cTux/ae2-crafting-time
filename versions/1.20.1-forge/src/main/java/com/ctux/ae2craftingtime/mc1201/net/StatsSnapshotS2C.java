package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public record StatsSnapshotS2C(List<String> requestedKeys, List<StatsEntry> entries,
        Map<String, Long> networkAmounts, Map<String, Long> waitingTicks, Set<String> missingProviders, long cpuContext) {
    public StatsSnapshotS2C(List<StatsEntry> entries) {
        this(entries.stream().map(entry -> entry.key().outputId()).toList(), entries, Map.of(), Map.of(), Set.of(), -1);
    }

    public static void encode(StatsSnapshotS2C packet, FriendlyByteBuf buffer) {
        StatsPacketCodec.writeSnapshot(buffer,
                new StatsPacketCodec.Snapshot(packet.requestedKeys, packet.entries, packet.networkAmounts,
                        packet.waitingTicks, packet.missingProviders, packet.cpuContext));
    }

    public static StatsSnapshotS2C decode(FriendlyByteBuf buffer) {
        var snapshot = StatsPacketCodec.readSnapshot(buffer);
        return new StatsSnapshotS2C(snapshot.requestedKeys(), snapshot.entries(), snapshot.networkAmounts(),
                snapshot.waitingTicks(), snapshot.missingProviders(), snapshot.cpuContext());
    }

    public static void handle(StatsSnapshotS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ClientStats.CACHE.replace(packet.requestedKeys.stream().map(ProfileKey::new).toList(), packet.entries);
            ClientStats.replaceNetworkAmounts(packet.requestedKeys, packet.networkAmounts);
            ClientStats.replaceWaitingTicks(packet.requestedKeys, packet.waitingTicks);
            ClientStats.replaceMissingProviders(packet.requestedKeys, packet.missingProviders, packet.cpuContext);
        });
        context.setPacketHandled(true);
    }
}
