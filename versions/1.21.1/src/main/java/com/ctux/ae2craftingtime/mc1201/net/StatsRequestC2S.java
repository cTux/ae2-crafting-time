package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatsEntry;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;
import com.ctux.ae2craftingtime.mc1201.StatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

public record StatsRequestC2S(List<String> keys) {
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

    public static void handle(StatsRequestC2S packet, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            var entries = new ArrayList<StatsEntry>();
            for (var key : packet.keys) {
                ProfilerBridge.stats(new ProfileKey(key)).ifPresent(stats -> entries.add(new StatsEntry(new ProfileKey(key), stats)));
            }
            StatsNetwork.sendTo(player, new StatsSnapshotS2C(entries));
        });
        context.setPacketHandled(true);
    }
}
