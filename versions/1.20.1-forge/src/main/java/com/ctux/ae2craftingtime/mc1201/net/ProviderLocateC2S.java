package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.mc1201.ProviderLocateServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record ProviderLocateC2S(String outputId) {
    public ProviderLocateC2S {
        outputId = PacketLimits.checkedOutputId(outputId);
    }

    public static void encode(ProviderLocateC2S packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.outputId, PacketLimits.MAX_OUTPUT_ID_LENGTH);
    }

    public static ProviderLocateC2S decode(FriendlyByteBuf buffer) {
        return new ProviderLocateC2S(buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH));
    }

    public static void handle(ProviderLocateC2S packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ProviderLocateServer.locate(player, packet.outputId,
                        (target, highlight) -> com.ctux.ae2craftingtime.mc1201.StatsNetwork.sendTo(target,
                                new ProviderHighlightS2C(highlight.networkId(), highlight.dimensionId(),
                                        highlight.positions(), highlight.outputId(), highlight.durationSeconds(),
                                        highlight.plateOnly())));
            }
        });
        context.setPacketHandled(true);
    }
}
