package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.StatsRequestC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsSnapshotS2C;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class StatsNetwork {
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(Ae2CraftingTime.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    public static void register() {
        var id = 0;
        CHANNEL.messageBuilder(StatsRequestC2S.class, id++)
                .direction(PacketFlow.SERVERBOUND)
                .encoder(StatsRequestC2S::encode)
                .decoder(StatsRequestC2S::decode)
                .consumerMainThread(StatsRequestC2S::handle)
                .add();
        CHANNEL.messageBuilder(StatsSnapshotS2C.class, id)
                .direction(PacketFlow.CLIENTBOUND)
                .encoder(StatsSnapshotS2C::encode)
                .decoder(StatsSnapshotS2C::decode)
                .consumerMainThread(StatsSnapshotS2C::handle)
                .add();
    }

    public static void sendTo(ServerPlayer player, StatsSnapshotS2C packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    private StatsNetwork() {
    }
}
