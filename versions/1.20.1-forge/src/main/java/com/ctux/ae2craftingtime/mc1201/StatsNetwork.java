package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightS2C;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcRequestC2S;
import com.ctux.ae2craftingtime.mc1201.net.CpuTtcSnapshotS2C;
import com.ctux.ae2craftingtime.mc1201.net.ProviderLocateC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsChatC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsRequestC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsSnapshotS2C;
import com.ctux.ae2craftingtime.mc1201.net.PlanRecurrenceS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@SuppressWarnings({ "deprecation", "removal" })
public final class StatsNetwork {
    private static final String PROTOCOL = "18";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Ae2CraftingTime.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void register() {
        var id = 0;
        CHANNEL.registerMessage(id++, StatsRequestC2S.class, StatsRequestC2S::encode, StatsRequestC2S::decode,
                StatsRequestC2S::handle);
        CHANNEL.registerMessage(id++, StatsSnapshotS2C.class, StatsSnapshotS2C::encode, StatsSnapshotS2C::decode,
                StatsSnapshotS2C::handle);
        CHANNEL.registerMessage(id++, StatsChatC2S.class, StatsChatC2S::encode, StatsChatC2S::decode,
                StatsChatC2S::handle);
        CHANNEL.registerMessage(id++, ProviderHighlightS2C.class, ProviderHighlightS2C::encode,
                ProviderHighlightS2C::decode, ProviderHighlightS2C::handle);
        CHANNEL.registerMessage(id++, ProviderLocateC2S.class, ProviderLocateC2S::encode,
                ProviderLocateC2S::decode, ProviderLocateC2S::handle);
        CHANNEL.registerMessage(id++, CpuTtcRequestC2S.class, CpuTtcRequestC2S::encode,
                CpuTtcRequestC2S::decode, CpuTtcRequestC2S::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CpuTtcSnapshotS2C.class, CpuTtcSnapshotS2C::encode,
                CpuTtcSnapshotS2C::decode, CpuTtcSnapshotS2C::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id, PlanRecurrenceS2C.class, PlanRecurrenceS2C::encode,
                PlanRecurrenceS2C::decode, PlanRecurrenceS2C::handle,
                java.util.Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendTo(ServerPlayer player, StatsSnapshotS2C packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendTo(ServerPlayer player, CpuTtcSnapshotS2C packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    public static void sendTo(ServerPlayer player, PlanRecurrenceS2C packet) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet); }

    public static void sendTo(ServerPlayer player, ProviderHighlightS2C packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(StatsChatC2S packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToServer(ProviderLocateC2S packet) {
        CHANNEL.sendToServer(packet);
    }

    private StatsNetwork() {
    }
}
