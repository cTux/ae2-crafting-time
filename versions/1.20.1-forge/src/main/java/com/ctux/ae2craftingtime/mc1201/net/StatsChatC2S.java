package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PlayerMessageRateLimit;
import com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record StatsChatC2S(List<String> messages) {
    private static final PlayerMessageRateLimit RATE_LIMIT = new PlayerMessageRateLimit();

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

    public static void handle(StatsChatC2S packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null || !Ae2CraftingTimeConfig.SHOW_CHAT_MESSAGES.get()) {
                return;
            }
            if (!RATE_LIMIT.allow(player.getUUID(), System.currentTimeMillis())) {
                return;
            }
            player.getServer().getPlayerList().broadcastChatMessage(
                    PlayerChatMessage.unsigned(player.getUUID(), message(packet.messages)),
                    player,
                    ChatType.bind(ChatType.CHAT, player));
        });
        context.setPacketHandled(true);
    }

    static Component component(List<String> messages) {
        return Component.literal(message(messages));
    }

    private static String message(List<String> messages) {
        return String.join(" | ", messages);
    }
}
