package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.mc1201.ProviderLocateServer;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

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

    public void handle(ServerPlayer player) {
        ProviderLocateServer.locate(player, outputId,
                (target, highlight) -> StatsNetwork.sendTo(target, new ProviderHighlightS2C(
                        highlight.dimensionId(), highlight.positions(), highlight.outputId(),
                        highlight.durationSeconds(), highlight.plateOnly())));
    }
}
