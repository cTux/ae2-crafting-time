package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.ServerOptionsRuntime;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ServerOptionsUpdateC2S(byte[] bytes) {
    public static void encode(ServerOptionsUpdateC2S packet, FriendlyByteBuf buffer) {
        buffer.writeByteArray(packet.bytes);
    }

    public static ServerOptionsUpdateC2S decode(FriendlyByteBuf buffer) {
        return new ServerOptionsUpdateC2S(buffer.readByteArray(64));
    }

    public static void handle(ServerOptionsUpdateC2S packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null) ServerOptionsRuntime.accept(sender, packet.bytes);
        });
        context.setPacketHandled(true);
    }
}
