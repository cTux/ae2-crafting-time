package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightS2C;
import com.ctux.ae2craftingtime.mc1201.net.ProviderLocateC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsChatC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsRequestC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsSnapshotS2C;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class StatsNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        IntegrationLog.required("network-registration", () -> registerPayloads(event));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("15");
        registrar.playToServer(StatsRequestC2S.TYPE, StatsRequestC2S.STREAM_CODEC, StatsRequestC2S::handle);
        registrar.playToServer(StatsChatC2S.TYPE, StatsChatC2S.STREAM_CODEC, StatsChatC2S::handle);
        registrar.playToClient(StatsSnapshotS2C.TYPE, StatsSnapshotS2C.STREAM_CODEC, StatsSnapshotS2C::handle);
        registrar.playToClient(ProviderHighlightS2C.TYPE, ProviderHighlightS2C.STREAM_CODEC,
                ProviderHighlightS2C::handle);
        registrar.playToServer(ProviderLocateC2S.TYPE, ProviderLocateC2S.STREAM_CODEC, ProviderLocateC2S::handle);
    }

    public static void sendTo(ServerPlayer player, StatsSnapshotS2C packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendTo(ServerPlayer player, ProviderHighlightS2C packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(StatsChatC2S packet) {
        PacketDistributor.sendToServer(packet);
    }

    public static void sendToServer(ProviderLocateC2S packet) {
        PacketDistributor.sendToServer(packet);
    }

    private StatsNetwork() {
    }
}
