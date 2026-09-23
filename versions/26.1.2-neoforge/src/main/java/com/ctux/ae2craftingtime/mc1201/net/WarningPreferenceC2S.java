package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.WarningPreferenceServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WarningPreferenceC2S(boolean receive) implements CustomPacketPayload {
    public static final Type<WarningPreferenceC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ae2craftingtime", "warning_preference"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarningPreferenceC2S> STREAM_CODEC = StreamCodec.ofMember(
            WarningPreferenceC2S::encode, WarningPreferenceC2S::decode);

    @Override
    public Type<WarningPreferenceC2S> type() { return TYPE; }

    public static void encode(WarningPreferenceC2S packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.receive);
    }

    public static WarningPreferenceC2S decode(FriendlyByteBuf buffer) {
        return new WarningPreferenceC2S(buffer.readBoolean());
    }

    public static void handle(WarningPreferenceC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) WarningPreferenceServer.set(sender, packet.receive);
        });
    }
}
