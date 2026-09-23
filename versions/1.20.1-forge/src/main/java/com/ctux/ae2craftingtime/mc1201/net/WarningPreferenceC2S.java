package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.WarningPreferenceServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record WarningPreferenceC2S(boolean receive) {
    public static void encode(WarningPreferenceC2S packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.receive);
    }

    public static WarningPreferenceC2S decode(FriendlyByteBuf buffer) {
        return new WarningPreferenceC2S(buffer.readBoolean());
    }

    public static void handle(WarningPreferenceC2S packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            var sender = context.getSender();
            if (sender != null) WarningPreferenceServer.set(sender, packet.receive);
        });
        context.setPacketHandled(true);
    }
}
