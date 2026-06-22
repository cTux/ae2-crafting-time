package com.ctux.ae2craftingtime.mc1201.net;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.me.crafting.CraftingCPUMenu;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record StatsRequestC2S(List<String> keys) implements CustomPacketPayload {
    public static final Type<StatsRequestC2S> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("ae2craftingtime", "stats_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatsRequestC2S> STREAM_CODEC = StreamCodec.ofMember(
            StatsRequestC2S::encode,
            StatsRequestC2S::decode);

    @Override
    public Type<StatsRequestC2S> type() {
        return TYPE;
    }

    public static void encode(StatsRequestC2S packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.keys.size());
        for (var key : packet.keys) {
            buffer.writeUtf(key);
        }
    }

    public static StatsRequestC2S decode(FriendlyByteBuf buffer) {
        var size = buffer.readVarInt();
        var keys = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            keys.add(buffer.readUtf());
        }
        return new StatsRequestC2S(keys);
    }

    public static void handle(StatsRequestC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            var entries = new ArrayList<StatsEntry>();
            var networkId = ProfilerBridge.networkId(currentGrid(player));
            for (var key : packet.keys) {
                ProfilerBridge.stats(new ProfileKey(networkId, key))
                        .ifPresent(stats -> entries.add(new StatsEntry(new ProfileKey(key), stats)));
            }
            StatsNetwork.sendTo(player, new StatsSnapshotS2C(entries));
        });
    }

    private static IGrid currentGrid(ServerPlayer player) {
        if (player.containerMenu instanceof CraftingCPUMenu menu) {
            return craftingCpuGrid(menu);
        }
        if (player.containerMenu instanceof AEBaseMenu menu && menu.getTarget() instanceof IActionHost host) {
            var node = host.getActionableNode();
            return node == null ? null : node.getGrid();
        }
        return null;
    }

    private static IGrid craftingCpuGrid(CraftingCPUMenu menu) {
        try {
            var method = CraftingCPUMenu.class.getDeclaredMethod("getGrid");
            method.setAccessible(true);
            return (IGrid) method.invoke(menu);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
