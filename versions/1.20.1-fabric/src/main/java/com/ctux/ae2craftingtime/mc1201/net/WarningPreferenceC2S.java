package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.mc1201.WarningPreferenceServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record WarningPreferenceC2S(boolean receive) {
    public static void encode(WarningPreferenceC2S packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.receive);
    }

    public static WarningPreferenceC2S decode(FriendlyByteBuf buffer) {
        return new WarningPreferenceC2S(buffer.readBoolean());
    }

    public void handle(ServerPlayer sender) {
        WarningPreferenceServer.set(sender, receive);
    }
}
