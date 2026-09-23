package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ServerOptionsRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerOptionsUpdateC2S(byte[] bytes) implements CustomPacketPayload {
    public static final Type<ServerOptionsUpdateC2S> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath("ae2craftingtime", "server_options_update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerOptionsUpdateC2S> STREAM_CODEC = StreamCodec.ofMember(
            ServerOptionsUpdateC2S::encode, ServerOptionsUpdateC2S::decode);

    @Override public Type<ServerOptionsUpdateC2S> type() { return TYPE; }
    public static void encode(ServerOptionsUpdateC2S packet, FriendlyByteBuf buffer) { buffer.writeByteArray(packet.bytes); }
    public static ServerOptionsUpdateC2S decode(FriendlyByteBuf buffer) {
        return new ServerOptionsUpdateC2S(buffer.readByteArray(64));
    }
    public static void handle(ServerOptionsUpdateC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) ServerOptionsRuntime.accept(sender, packet.bytes);
        });
    }
}
