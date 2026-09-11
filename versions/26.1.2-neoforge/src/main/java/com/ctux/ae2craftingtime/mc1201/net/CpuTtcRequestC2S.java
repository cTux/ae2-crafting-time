package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.CpuTtcRequestHandler;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CpuTtcRequestC2S(CpuTtcPacketCodec.Request request) implements CustomPacketPayload {
    public static final Type<CpuTtcRequestC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ae2craftingtime", "cpu_ttc_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CpuTtcRequestC2S> STREAM_CODEC = StreamCodec.ofMember(
            CpuTtcRequestC2S::encode, CpuTtcRequestC2S::decode);
    @Override public Type<CpuTtcRequestC2S> type() { return TYPE; }
    public static void encode(CpuTtcRequestC2S packet, FriendlyByteBuf buffer) { CpuTtcPacketCodec.writeRequest(buffer, packet.request); }
    public static CpuTtcRequestC2S decode(FriendlyByteBuf buffer) { return new CpuTtcRequestC2S(CpuTtcPacketCodec.readRequest(buffer)); }
    public static void handle(CpuTtcRequestC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var response = CpuTtcRequestHandler.collect(player, packet.request);
            if (response != null) StatsNetwork.sendTo(player, new CpuTtcSnapshotS2C(
                    new CpuTtcPacketCodec.Snapshot(response.session(), response.sequence(), response.entries())));
        });
    }
}
