package com.ctux.ae2craftingtime.mc1201.net;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public record StatsChatC2S(List<String> messages) {
    public StatsChatC2S {
        messages = List.copyOf(messages.subList(0, Math.min(2, messages.size())));
    }

    public static void encode(StatsChatC2S packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.messages.size());
        for (var message : packet.messages) {
            buffer.writeUtf(message);
        }
    }

    public static StatsChatC2S decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var messages = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            messages.add(buffer.readUtf());
        }
        return new StatsChatC2S(messages);
    }

    public void handle(ServerPlayer player) {
        for (var message : messages) {
            player.server.getPlayerList().broadcastSystemMessage(component(message), false);
        }
    }

    private static Component component(String message) {
        if (message.startsWith("No cached") || message.startsWith("Reset")) {
            return Component.literal(message).withStyle(ChatFormatting.YELLOW);
        }

        var colon = message.indexOf(": ");
        if (colon >= 0) {
            return Component.literal(message.substring(0, colon)).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(message.substring(colon + 2)).withStyle(ChatFormatting.AQUA));
        }
        return Component.literal(message).withStyle(ChatFormatting.GRAY);
    }
}
