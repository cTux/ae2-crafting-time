package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.StatsChatC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsRequestC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsSnapshotS2C;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class StatsNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("8");
        registrar.playToServer(StatsRequestC2S.TYPE, StatsRequestC2S.STREAM_CODEC, StatsRequestC2S::handle);
        registrar.playToServer(StatsChatC2S.TYPE, StatsChatC2S.STREAM_CODEC, StatsChatC2S::handle);
        registrar.playToClient(StatsSnapshotS2C.TYPE, StatsSnapshotS2C.STREAM_CODEC, StatsSnapshotS2C::handle);
    }

    public static void sendTo(ServerPlayer player, StatsSnapshotS2C packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToServer(StatsChatC2S packet) {
        PacketDistributor.sendToServer(packet);
    }

    private StatsNetwork() {
    }
}
