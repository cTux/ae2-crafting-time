package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ProviderHighlightClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record ProviderHighlightS2C(String dimensionId, List<BlockPos> positions, String outputId,
        int durationSeconds) {
    public static void encode(ProviderHighlightS2C packet, FriendlyByteBuf buffer) {
        ProviderHighlightCodec.write(buffer, new ProviderHighlightCodec.Highlight(packet.dimensionId,
                packet.positions, packet.outputId, packet.durationSeconds));
    }

    public static ProviderHighlightS2C decode(FriendlyByteBuf buffer) {
        var highlight = ProviderHighlightCodec.read(buffer);
        return new ProviderHighlightS2C(highlight.dimensionId(), highlight.positions(), highlight.outputId(),
                highlight.durationSeconds());
    }

    public void handle() {
        if (durationSeconds <= 0 || positions == null || positions.isEmpty()) {
            ProviderHighlightClient.clearFor(outputId);
        } else {
            ProviderHighlightClient.show(dimensionId, positions, durationSeconds, outputId);
        }
    }
}
