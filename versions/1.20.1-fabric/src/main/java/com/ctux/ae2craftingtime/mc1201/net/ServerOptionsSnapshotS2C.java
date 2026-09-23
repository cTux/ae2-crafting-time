package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ClientServerOptions;
import net.minecraft.network.FriendlyByteBuf;

public record ServerOptionsSnapshotS2C(byte[] bytes) {
    public static void encode(ServerOptionsSnapshotS2C packet, FriendlyByteBuf buffer) {
        buffer.writeByteArray(packet.bytes);
    }

    public static ServerOptionsSnapshotS2C decode(FriendlyByteBuf buffer) {
        return new ServerOptionsSnapshotS2C(buffer.readByteArray(64));
    }

    public void handle() { ClientServerOptions.receive(bytes); }
}
