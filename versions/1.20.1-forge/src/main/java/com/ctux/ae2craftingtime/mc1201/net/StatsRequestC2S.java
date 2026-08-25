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
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record StatsRequestC2S(List<String> keys, boolean reset) {
    public StatsRequestC2S(List<String> keys) {
        this(keys, false);
    }

    public static void encode(StatsRequestC2S packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.reset);
        buffer.writeVarInt(packet.keys.size());
        for (var key : packet.keys) {
            buffer.writeUtf(key);
        }
    }

    public static StatsRequestC2S decode(FriendlyByteBuf buffer) {
        var reset = buffer.readBoolean();
        var size = buffer.readVarInt();
        var keys = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            keys.add(buffer.readUtf());
        }
        return new StatsRequestC2S(keys, reset);
    }

    public static void handle(StatsRequestC2S packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            var entries = new ArrayList<StatsEntry>();
            var grid = currentGrid(player);
            var networkId = ProfilerBridge.networkId(grid);
            for (var key : packet.keys) {
                var profileKey = new ProfileKey(networkId, key);
                if (packet.reset) {
                    ProfilerBridge.clearStats(profileKey);
                    continue;
                }
                ProfilerBridge.entry(profileKey, new ProfileKey(key)).ifPresent(entries::add);
            }
            StatsNetwork.sendTo(player, new StatsSnapshotS2C(packet.keys, entries, networkAmounts(grid, packet.keys)));
        });
        context.setPacketHandled(true);
    }

    private static Map<String, Long> networkAmounts(IGrid grid, List<String> keys) {
        var amounts = new HashMap<String, Long>();
        if (grid == null) {
            return amounts;
        }
        keys.forEach(key -> amounts.put(key, 0L));
        for (var entry : grid.getStorageService().getInventory().getAvailableStacks()) {
            var id = entry.getKey().getId().toString();
            if (keys.contains(id)) {
                amounts.merge(id, entry.getLongValue(), Long::sum);
            }
        }
        return amounts;
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
