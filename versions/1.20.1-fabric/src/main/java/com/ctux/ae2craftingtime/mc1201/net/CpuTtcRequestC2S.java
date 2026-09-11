package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.CpuTtcRequestHandler;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record CpuTtcRequestC2S(CpuTtcPacketCodec.Request request) {
    public static void encode(CpuTtcRequestC2S packet, FriendlyByteBuf buffer) {
        CpuTtcPacketCodec.writeRequest(buffer, packet.request);
    }

    public static CpuTtcRequestC2S decode(FriendlyByteBuf buffer) {
        return new CpuTtcRequestC2S(CpuTtcPacketCodec.readRequest(buffer));
    }

    public void handle(ServerPlayer player) {
        var response = CpuTtcRequestHandler.collect(player, request);
        if (response != null) StatsNetwork.sendTo(player, new CpuTtcSnapshotS2C(
                new CpuTtcPacketCodec.Snapshot(response.session(), response.sequence(), response.entries())));
    }
}
