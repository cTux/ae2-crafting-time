package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ProviderHighlightClient;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record ProviderHighlightS2C(String dimensionId, List<BlockPos> positions, String outputId,
        int durationSeconds)
        implements CustomPacketPayload {
    public static final Type<ProviderHighlightS2C> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "provider_highlight"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProviderHighlightS2C> STREAM_CODEC = StreamCodec.ofMember(
            ProviderHighlightS2C::encode,
            ProviderHighlightS2C::decode);

    @Override
    public Type<ProviderHighlightS2C> type() {
        return TYPE;
    }

    public static void encode(ProviderHighlightS2C packet, FriendlyByteBuf buffer) {
        ProviderHighlightCodec.write(buffer, new ProviderHighlightCodec.Highlight(packet.dimensionId,
                packet.positions, packet.outputId, packet.durationSeconds));
    }

    public static ProviderHighlightS2C decode(FriendlyByteBuf buffer) {
        var highlight = ProviderHighlightCodec.read(buffer);
        return new ProviderHighlightS2C(highlight.dimensionId(), highlight.positions(), highlight.outputId(),
                highlight.durationSeconds());
    }

    public static void handle(ProviderHighlightS2C packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.durationSeconds <= 0 || packet.positions == null || packet.positions.isEmpty()) {
                ProviderHighlightClient.clearFor(packet.outputId);
            } else {
                ProviderHighlightClient.show(packet.dimensionId, packet.positions, packet.durationSeconds,
                        packet.outputId);
            }
        });
    }
}
