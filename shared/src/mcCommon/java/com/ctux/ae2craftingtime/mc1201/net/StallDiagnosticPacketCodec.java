package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.StallDiagnostic;
import net.minecraft.network.FriendlyByteBuf;

public final class StallDiagnosticPacketCodec {
    public static void write(FriendlyByteBuf buffer, StallDiagnostic diagnostic) {
        buffer.writeVarLong(diagnostic.idleTicks());
        buffer.writeDouble(diagnostic.typicalDurationTicks());
        buffer.writeVarInt(diagnostic.activeBatches());
        buffer.writeVarInt(diagnostic.usedParallelSlots());
        buffer.writeVarInt(diagnostic.totalParallelSlots());
    }

    public static StallDiagnostic read(FriendlyByteBuf buffer) {
        return new StallDiagnostic(buffer.readVarLong(), buffer.readDouble(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt());
    }

    private StallDiagnosticPacketCodec() {
    }
}
