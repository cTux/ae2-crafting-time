package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.mc1201.ProviderLocateServer;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ProviderLocateC2S(String outputId) implements CustomPacketPayload {
    public ProviderLocateC2S {
        outputId = PacketLimits.checkedOutputId(outputId);
    }

    public static final Type<ProviderLocateC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ae2craftingtime", "provider_locate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProviderLocateC2S> STREAM_CODEC = StreamCodec.ofMember(
            ProviderLocateC2S::encode, ProviderLocateC2S::decode);

    @Override
    public Type<ProviderLocateC2S> type() {
        return TYPE;
    }

    public static void encode(ProviderLocateC2S packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.outputId, PacketLimits.MAX_OUTPUT_ID_LENGTH);
    }

    public static ProviderLocateC2S decode(FriendlyByteBuf buffer) {
        return new ProviderLocateC2S(buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH));
    }

    public static void handle(ProviderLocateC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ProviderLocateServer.locate(player, packet.outputId,
                        (target, highlight) -> StatsNetwork.sendTo(target, new ProviderHighlightS2C(
                                highlight.networkId(), highlight.dimensionId(), highlight.positions(),
                                highlight.outputId(), highlight.durationSeconds(), highlight.plateOnly())));
            }
        });
    }
}
