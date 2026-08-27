package com.ctux.ae2craftingtime.mc1201.net;

import appeng.api.networking.IGrid;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.PacketLimits;
import com.ctux.ae2craftingtime.core.PlayerRequestRateLimit;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import com.ctux.ae2craftingtime.mc1201.StatsRequestContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public record StatsRequestC2S(List<String> keys) implements CustomPacketPayload {
    private static final PlayerRequestRateLimit RATE_LIMIT = new PlayerRequestRateLimit();

    public StatsRequestC2S {
        keys = PacketLimits.checkedKeys(keys);
    }

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
            buffer.writeUtf(key, PacketLimits.MAX_OUTPUT_ID_LENGTH);
        }
    }

    public static StatsRequestC2S decode(FriendlyByteBuf buffer) {
        var size = PacketLimits.checkedSize(buffer.readVarInt(), PacketLimits.MAX_KEYS, "keys");
        var keys = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            keys.add(buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH));
        }
        return new StatsRequestC2S(keys);
    }

    public static void handle(StatsRequestC2S packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !RATE_LIMIT.allow(player.getUUID(), packet.keys.size(), System.currentTimeMillis())) {
                return;
            }

            var entries = new ArrayList<StatsEntry>();
            var requestContext = StatsRequestContext.current(player);
            var grid = requestContext.grid();
            var networkId = ProfilerBridge.networkId(grid);
            for (var key : packet.keys) {
                var profileKey = new ProfileKey(networkId, key);
                ProfilerBridge.entry(profileKey, new ProfileKey(key), requestContext.craftingCpu(),
                        player.level().getGameTime()).ifPresent(entries::add);
            }
            StatsNetwork.sendTo(player, new StatsSnapshotS2C(packet.keys, entries, networkAmounts(grid, packet.keys)));
        });
    }

    private static Map<String, Long> networkAmounts(IGrid grid, List<String> keys) {
        var amounts = new HashMap<String, Long>();
        if (grid == null) {
            return amounts;
        }
        var requested = new HashSet<>(keys);
        keys.forEach(key -> amounts.put(key, 0L));
        for (var entry : grid.getStorageService().getInventory().getAvailableStacks()) {
            var id = entry.getKey().getId().toString();
            if (requested.contains(id)) {
                amounts.merge(id, entry.getLongValue(), Long::sum);
            }
        }
        return amounts;
    }

}
