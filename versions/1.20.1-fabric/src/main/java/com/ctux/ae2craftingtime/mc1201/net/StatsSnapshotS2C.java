package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public void handle() {
        ClientStats.CACHE.replace(requestedKeys.stream().map(ProfileKey::new).toList(), entries);
        ClientStats.replaceNetworkAmounts(requestedKeys, networkAmounts);
        ClientStats.replaceWaitingTicks(requestedKeys, waitingTicks);
        ClientStats.replaceMissingProviders(requestedKeys, missingProviders, cpuContext);
    }
}
