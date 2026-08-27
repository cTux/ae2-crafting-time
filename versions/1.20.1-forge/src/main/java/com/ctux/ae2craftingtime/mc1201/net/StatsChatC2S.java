package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.StatsChatAction;
import com.ctux.ae2craftingtime.mc1201.StatsChatServer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record StatsChatC2S(String outputId, long amount, StatsChatAction action) {
    public StatsChatC2S {
        outputId = PacketLimits.checkedOutputId(outputId);
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    public static void encode(StatsChatC2S packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.outputId, PacketLimits.MAX_OUTPUT_ID_LENGTH);
        buffer.writeVarLong(packet.amount);
        buffer.writeEnum(packet.action);
    }

    public static StatsChatC2S decode(FriendlyByteBuf buffer) {
        return new StatsChatC2S(buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH), buffer.readVarLong(),
                buffer.readEnum(StatsChatAction.class));
    }

    public static void handle(StatsChatC2S packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                StatsChatServer.handle(player, packet.outputId, packet.amount, packet.action);
            }
        });
        context.setPacketHandled(true);
    }
}
