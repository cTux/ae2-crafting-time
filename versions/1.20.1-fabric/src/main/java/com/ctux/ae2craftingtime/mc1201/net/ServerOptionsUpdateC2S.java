package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ServerOptionsRuntime;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record ServerOptionsUpdateC2S(byte[] bytes) {
    public static void encode(ServerOptionsUpdateC2S packet, FriendlyByteBuf buffer) {
        buffer.writeByteArray(packet.bytes);
    }

    public static ServerOptionsUpdateC2S decode(FriendlyByteBuf buffer) {
        return new ServerOptionsUpdateC2S(buffer.readByteArray(64));
    }

    public void handle(ServerPlayer sender) { ServerOptionsRuntime.accept(sender, bytes); }
}
