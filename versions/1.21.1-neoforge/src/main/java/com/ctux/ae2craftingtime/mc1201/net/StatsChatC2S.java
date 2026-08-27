package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.StatsChatAction;
import com.ctux.ae2craftingtime.mc1201.StatsChatServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StatsChatC2S(String outputId, long amount, StatsChatAction action) implements CustomPacketPayload {
    public StatsChatC2S {
        outputId = PacketLimits.checkedOutputId(outputId);
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    public static final Type<StatsChatC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "stats_chat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatsChatC2S> STREAM_CODEC = StreamCodec.ofMember(
            StatsChatC2S::encode, StatsChatC2S::decode);

    @Override
    public Type<StatsChatC2S> type() {
        return TYPE;
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

    public static void handle(StatsChatC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                StatsChatServer.handle(player, packet.outputId, packet.amount, packet.action);
            }
        });
    }
}
