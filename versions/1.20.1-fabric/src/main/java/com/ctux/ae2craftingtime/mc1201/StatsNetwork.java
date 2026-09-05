package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.mc1201.net.ProviderHighlightS2C;
import com.ctux.ae2craftingtime.mc1201.net.ProviderLocateC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsChatC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsRequestC2S;
import com.ctux.ae2craftingtime.mc1201.net.StatsSnapshotS2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class StatsNetwork {
    private static final ResourceLocation REQUEST_ID = new ResourceLocation(Ae2CraftingTime.MOD_ID,
            "stats_request_v2");
    private static final ResourceLocation SNAPSHOT_ID = new ResourceLocation(Ae2CraftingTime.MOD_ID,
            "stats_snapshot_v8");
    private static final ResourceLocation CHAT_ID = new ResourceLocation(Ae2CraftingTime.MOD_ID, "stats_chat_v2");
    private static final ResourceLocation HIGHLIGHT_ID = new ResourceLocation(Ae2CraftingTime.MOD_ID,
            "provider_highlight_v4");
    private static final ResourceLocation LOCATE_ID = new ResourceLocation(Ae2CraftingTime.MOD_ID,
            "provider_locate_v1");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_ID,
                (server, player, handler, buffer, responseSender) -> {
                    var packet = StatsRequestC2S.decode(buffer);
                    server.execute(() -> packet.handle(player));
                });
        ServerPlayNetworking.registerGlobalReceiver(CHAT_ID,
                (server, player, handler, buffer, responseSender) -> {
                    var packet = StatsChatC2S.decode(buffer);
                    server.execute(() -> packet.handle(player));
                });
        ServerPlayNetworking.registerGlobalReceiver(LOCATE_ID,
                (server, player, handler, buffer, responseSender) -> {
                    var packet = ProviderLocateC2S.decode(buffer);
                    server.execute(() -> packet.handle(player));
                });
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(SNAPSHOT_ID,
                (client, handler, buffer, responseSender) -> {
                    var packet = StatsSnapshotS2C.decode(buffer);
                    client.execute(packet::handle);
                });
        ClientPlayNetworking.registerGlobalReceiver(HIGHLIGHT_ID,
                (client, handler, buffer, responseSender) -> {
                    var packet = ProviderHighlightS2C.decode(buffer);
                    client.execute(packet::handle);
                });
    }

    public static void sendToServer(StatsRequestC2S packet) {
        ClientPlayNetworking.send(REQUEST_ID, encode(packet));
    }

    public static void sendToServer(StatsChatC2S packet) {
        ClientPlayNetworking.send(CHAT_ID, encode(packet));
    }

    public static void sendToServer(ProviderLocateC2S packet) {
        ClientPlayNetworking.send(LOCATE_ID, encode(packet));
    }

    public static void sendTo(ServerPlayer player, StatsSnapshotS2C packet) {
        ServerPlayNetworking.send(player, SNAPSHOT_ID, encode(packet));
    }

    public static void sendTo(ServerPlayer player, ProviderHighlightS2C packet) {
        ServerPlayNetworking.send(player, HIGHLIGHT_ID, encode(packet));
    }

    private static FriendlyByteBuf encode(StatsRequestC2S packet) {
        var buffer = PacketByteBufs.create();
        StatsRequestC2S.encode(packet, buffer);
        return buffer;
    }

    private static FriendlyByteBuf encode(StatsSnapshotS2C packet) {
        var buffer = PacketByteBufs.create();
        StatsSnapshotS2C.encode(packet, buffer);
        return buffer;
    }

    private static FriendlyByteBuf encode(StatsChatC2S packet) {
        var buffer = PacketByteBufs.create();
        StatsChatC2S.encode(packet, buffer);
        return buffer;
    }

    private static FriendlyByteBuf encode(ProviderHighlightS2C packet) {
        var buffer = PacketByteBufs.create();
        ProviderHighlightS2C.encode(packet, buffer);
        return buffer;
    }

    private static FriendlyByteBuf encode(ProviderLocateC2S packet) {
        var buffer = PacketByteBufs.create();
        ProviderLocateC2S.encode(packet, buffer);
        return buffer;
    }

    private StatsNetwork() {
    }
}
