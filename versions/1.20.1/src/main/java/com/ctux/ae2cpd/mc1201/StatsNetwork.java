package com.ctux.ae2cpd.mc1201;

import com.ctux.ae2cpd.mc1201.net.StatsRequestC2S;
import com.ctux.ae2cpd.mc1201.net.StatsSnapshotS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class StatsNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Ae2CraftPerformanceDebugger.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void register() {
        var id = 0;
        CHANNEL.registerMessage(id++, StatsRequestC2S.class, StatsRequestC2S::encode, StatsRequestC2S::decode,
                StatsRequestC2S::handle);
        CHANNEL.registerMessage(id, StatsSnapshotS2C.class, StatsSnapshotS2C::encode, StatsSnapshotS2C::decode,
                StatsSnapshotS2C::handle);
    }

    public static void sendTo(ServerPlayer player, StatsSnapshotS2C packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private StatsNetwork() {
    }
}
