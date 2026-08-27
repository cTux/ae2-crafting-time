package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.TtcAccuracyStats;
import net.minecraft.network.FriendlyByteBuf;

public final class TtcAccuracyPacketCodec {
    public static void write(FriendlyByteBuf buffer, TtcAccuracyStats stats) {
        buffer.writeVarInt(stats.sampleCount());
        buffer.writeVarInt(stats.fullyCoveredSampleCount());
        buffer.writeDouble(stats.averageCoverage());
        buffer.writeDouble(stats.meanSignedErrorSeconds());
        buffer.writeDouble(stats.meanAbsolutePercentageError());
        buffer.writeDouble(stats.meanActualToPredictedRatio());
        buffer.writeVarLong(stats.lastPredictedSeconds());
        buffer.writeDouble(stats.lastActualWallSeconds());
        buffer.writeDouble(stats.lastActualTickSeconds());
        buffer.writeVarInt(stats.lastKnownRows());
        buffer.writeVarInt(stats.lastTotalRows());
    }

    public static TtcAccuracyStats read(FriendlyByteBuf buffer) {
        return new TtcAccuracyStats(buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readVarLong(), buffer.readDouble(), buffer.readDouble(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    private TtcAccuracyPacketCodec() {
    }
}
