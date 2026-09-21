package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import net.minecraft.network.FriendlyByteBuf;

public final class PlanStoredVariantsCodec {
    public static void write(FriendlyByteBuf buffer, long updateRevision, PlanRecurrenceChunk chunk) {
        buffer.writeVarLong(updateRevision);
        PlanRecurrenceCodec.write(buffer, chunk);
    }

    public static Decoded read(FriendlyByteBuf buffer) {
        try {
            var updateRevision = buffer.readVarLong();
            return new Decoded(updateRevision, PlanRecurrenceCodec.read(buffer));
        } catch (RuntimeException malformed) {
            buffer.skipBytes(buffer.readableBytes());
            return new Decoded(0, new PlanRecurrenceChunk(-1, 0, 0, 0, 0,
                    new byte[PlanRecurrenceChunk.MASK_BYTES]));
        }
    }

    public record Decoded(long updateRevision, PlanRecurrenceChunk chunk) {}
    private PlanStoredVariantsCodec() {}
}
