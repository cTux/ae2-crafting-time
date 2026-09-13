package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import net.minecraft.network.FriendlyByteBuf;

public final class PlanRecurrenceCodec {
    public static void write(FriendlyByteBuf buffer, PlanRecurrenceChunk chunk) {
        buffer.writeVarInt(chunk.containerId());
        buffer.writeVarLong(chunk.revision());
        buffer.writeVarInt(chunk.entryCount());
        buffer.writeVarInt(chunk.offset());
        buffer.writeVarInt(chunk.rowCount());
        buffer.writeBytes(chunk.mask());
    }

    public static PlanRecurrenceChunk read(FriendlyByteBuf buffer) {
        try {
            var container = buffer.readVarInt();
            var revision = buffer.readVarLong();
            var entries = buffer.readVarInt();
            var offset = buffer.readVarInt();
            var rows = buffer.readVarInt();
            var mask = new byte[PlanRecurrenceChunk.MASK_BYTES];
            buffer.readBytes(mask);
            if (!buffer.isReadable()) return new PlanRecurrenceChunk(container, revision, entries, offset, rows, mask);
        } catch (RuntimeException malformed) {
            buffer.skipBytes(buffer.readableBytes());
            return new PlanRecurrenceChunk(-1, 0, 0, 0, 0, new byte[PlanRecurrenceChunk.MASK_BYTES]);
        }
        buffer.skipBytes(buffer.readableBytes());
        return new PlanRecurrenceChunk(-1, 0, 0, 0, 0, new byte[PlanRecurrenceChunk.MASK_BYTES]);
    }

    private PlanRecurrenceCodec() {}
}

