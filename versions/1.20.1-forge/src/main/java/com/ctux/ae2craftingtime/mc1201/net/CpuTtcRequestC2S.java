package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.CpuTtcRequestHandler;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CpuTtcRequestC2S(CpuTtcPacketCodec.Request request) {
    public static void encode(CpuTtcRequestC2S packet, FriendlyByteBuf buffer) {
        CpuTtcPacketCodec.writeRequest(buffer, packet.request);
    }

    public static CpuTtcRequestC2S decode(FriendlyByteBuf buffer) {
        return new CpuTtcRequestC2S(CpuTtcPacketCodec.readRequest(buffer));
    }

    public static void handle(CpuTtcRequestC2S packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            var response = CpuTtcRequestHandler.collect(player, packet.request);
            if (response != null) StatsNetwork.sendTo(player, new CpuTtcSnapshotS2C(
                    new CpuTtcPacketCodec.Snapshot(response.session(), response.sequence(), response.entries())));
        });
        context.setPacketHandled(true);
    }
}
