package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.CpuTtcClient;
import net.minecraft.network.FriendlyByteBuf;

public record CpuTtcSnapshotS2C(CpuTtcPacketCodec.Snapshot snapshot) {
    public static void encode(CpuTtcSnapshotS2C packet, FriendlyByteBuf buffer) {
        CpuTtcPacketCodec.writeSnapshot(buffer, packet.snapshot);
    }

    public static CpuTtcSnapshotS2C decode(FriendlyByteBuf buffer) {
        return new CpuTtcSnapshotS2C(CpuTtcPacketCodec.readSnapshot(buffer));
    }

    public void handle() {
        CpuTtcClient.receive(snapshot);
    }
}
