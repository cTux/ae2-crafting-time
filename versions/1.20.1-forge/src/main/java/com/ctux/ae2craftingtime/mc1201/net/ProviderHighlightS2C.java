package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ProviderHighlightClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record ProviderHighlightS2C(String dimensionId, List<BlockPos> positions, String outputId,
        int durationSeconds, boolean plateOnly) {
    public static void encode(ProviderHighlightS2C packet, FriendlyByteBuf buffer) {
        ProviderHighlightCodec.write(buffer, new ProviderHighlightCodec.Highlight(packet.dimensionId,
                packet.positions, packet.outputId, packet.durationSeconds, packet.plateOnly));
    }

    public static ProviderHighlightS2C decode(FriendlyByteBuf buffer) {
        var highlight = ProviderHighlightCodec.read(buffer);
        return new ProviderHighlightS2C(highlight.dimensionId(), highlight.positions(), highlight.outputId(),
                highlight.durationSeconds(), highlight.plateOnly());
    }

    public static void handle(ProviderHighlightS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.durationSeconds <= 0 || packet.positions == null || packet.positions.isEmpty()) {
                ProviderHighlightClient.clearFor(packet.outputId);
            } else if (packet.plateOnly) {
                ProviderHighlightClient.showPlate(packet.dimensionId, packet.positions, packet.outputId);
            } else {
                ProviderHighlightClient.show(packet.dimensionId, packet.positions, packet.durationSeconds,
                        packet.outputId);
            }
        });
        context.setPacketHandled(true);
    }
}
