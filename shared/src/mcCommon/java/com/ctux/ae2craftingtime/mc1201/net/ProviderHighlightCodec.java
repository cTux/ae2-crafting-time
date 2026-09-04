package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PacketLimits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public final class ProviderHighlightCodec {
    public record Highlight(String dimensionId, List<BlockPos> positions, int durationSeconds) {
        public Highlight {
            positions = positions == null ? List.of() : List.copyOf(positions);
        }
    }

    public static void write(FriendlyByteBuf buffer, Highlight highlight) {
        var dimension = highlight.dimensionId() == null ? "" : highlight.dimensionId();
        if (dimension.length() > PacketLimits.MAX_OUTPUT_ID_LENGTH) {
            throw new IllegalArgumentException("dimension id too long");
        }
        buffer.writeUtf(dimension);
        var positions = highlight.positions().size() > PacketLimits.MAX_HIGHLIGHT_POSITIONS
                ? highlight.positions().subList(0, PacketLimits.MAX_HIGHLIGHT_POSITIONS)
                : highlight.positions();
        buffer.writeVarInt(positions.size());
        for (var pos : positions) {
            buffer.writeBlockPos(pos);
        }
        buffer.writeVarInt(Math.max(0, highlight.durationSeconds()));
    }

    public static Highlight read(FriendlyByteBuf buffer) {
        var dimension = buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH);
        var count = PacketLimits.checkedSize(buffer.readVarInt(), PacketLimits.MAX_HIGHLIGHT_POSITIONS, "positions");
        var positions = new ArrayList<BlockPos>(count);
        for (var i = 0; i < count; i++) {
            positions.add(buffer.readBlockPos());
        }
        return new Highlight(dimension, positions, Math.max(0, buffer.readVarInt()));
    }

    private ProviderHighlightCodec() {
    }
}
