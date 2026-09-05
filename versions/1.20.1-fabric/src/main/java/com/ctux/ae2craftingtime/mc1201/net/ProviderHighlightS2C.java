package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ProviderHighlightClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record ProviderHighlightS2C(String networkId, String dimensionId, List<BlockPos> positions, String outputId,
        int durationSeconds, boolean plateOnly) {
    public ProviderHighlightS2C(String dimensionId, List<BlockPos> positions, String outputId, int durationSeconds,
            boolean plateOnly) {
        this("", dimensionId, positions, outputId, durationSeconds, plateOnly);
    }

    public static void encode(ProviderHighlightS2C packet, FriendlyByteBuf buffer) {
        ProviderHighlightCodec.write(buffer, new ProviderHighlightCodec.Highlight(packet.networkId,
                packet.dimensionId, packet.positions, packet.outputId, packet.durationSeconds, packet.plateOnly));
    }

    public static ProviderHighlightS2C decode(FriendlyByteBuf buffer) {
        var highlight = ProviderHighlightCodec.read(buffer);
        return new ProviderHighlightS2C(highlight.networkId(), highlight.dimensionId(), highlight.positions(),
                highlight.outputId(), highlight.durationSeconds(), highlight.plateOnly());
    }

    public void handle() {
        if (durationSeconds <= 0 || positions == null || positions.isEmpty()) {
            ProviderHighlightClient.clearFor(networkId, outputId);
        } else if (plateOnly) {
            ProviderHighlightClient.showPlate(networkId, dimensionId, positions, outputId);
        } else {
            ProviderHighlightClient.show(networkId, dimensionId, positions, durationSeconds, outputId);
        }
    }
}
