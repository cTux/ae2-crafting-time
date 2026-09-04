package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ProviderHighlightClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record ProviderHighlightS2C(String dimensionId, List<BlockPos> positions, int durationSeconds) {
    public static void encode(ProviderHighlightS2C packet, FriendlyByteBuf buffer) {
        ProviderHighlightCodec.write(buffer, new ProviderHighlightCodec.Highlight(packet.dimensionId,
                packet.positions, packet.durationSeconds));
    }

    public static ProviderHighlightS2C decode(FriendlyByteBuf buffer) {
        var highlight = ProviderHighlightCodec.read(buffer);
        return new ProviderHighlightS2C(highlight.dimensionId(), highlight.positions(),
                highlight.durationSeconds());
    }

    public static void handle(ProviderHighlightS2C packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ProviderHighlightClient.show(packet.dimensionId, packet.positions,
                packet.durationSeconds));
        context.setPacketHandled(true);
    }
}
