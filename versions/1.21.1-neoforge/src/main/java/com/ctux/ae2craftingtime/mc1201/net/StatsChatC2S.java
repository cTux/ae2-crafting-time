package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PlayerMessageRateLimit;
import com.ctux.ae2craftingtime.mc1201.Ae2CraftingTimeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record StatsChatC2S(List<String> messages) implements CustomPacketPayload {
    private static final PlayerMessageRateLimit RATE_LIMIT = new PlayerMessageRateLimit();

    public StatsChatC2S {
        messages = List.copyOf(messages.subList(0, Math.min(2, messages.size())));
    }

    public static final Type<StatsChatC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "stats_chat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatsChatC2S> STREAM_CODEC = StreamCodec.ofMember(
            StatsChatC2S::encode,
            StatsChatC2S::decode);

    @Override
    public Type<StatsChatC2S> type() {
        return TYPE;
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

    public static void handle(StatsChatC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.getServer() == null
                    || !Ae2CraftingTimeConfig.SHOW_CHAT_MESSAGES.get()) {
                return;
            }
            if (!RATE_LIMIT.allow(player.getUUID(), System.currentTimeMillis())) {
                return;
            }
            player.getServer().getPlayerList().broadcastSystemMessage(component(packet.messages), true);
        });
    }

    static Component component(List<String> messages) {
        var component = Component.empty();
        for (var message : messages) {
            if (!component.getString().isEmpty()) {
                component.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            }
            component.append(component(message, messages.size() == 1));
        }
        return component;
    }

    private static Component component(String message, boolean notice) {
        if (notice) {
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
