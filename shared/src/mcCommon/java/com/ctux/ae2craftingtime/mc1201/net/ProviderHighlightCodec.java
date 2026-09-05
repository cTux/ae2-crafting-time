package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PacketLimits;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public final class ProviderHighlightCodec {
    public record Highlight(String networkId, String dimensionId, List<BlockPos> positions, String outputId,
            int durationSeconds, boolean plateOnly) {
        public Highlight {
            networkId = networkId == null ? "" : networkId;
            positions = positions == null ? List.of() : List.copyOf(positions);
            outputId = outputId == null ? "" : outputId;
        }

        public Highlight(String dimensionId, List<BlockPos> positions, String outputId, int durationSeconds,
                boolean plateOnly) {
            this("", dimensionId, positions, outputId, durationSeconds, plateOnly);
        }

        public Highlight(String dimensionId, List<BlockPos> positions, String outputId, int durationSeconds) {
            this("", dimensionId, positions, outputId, durationSeconds, false);
        }

        public Highlight(String networkId, String dimensionId, List<BlockPos> positions, String outputId,
                int durationSeconds) {
            this(networkId, dimensionId, positions, outputId, durationSeconds, false);
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
        if (highlight.outputId().length() > PacketLimits.MAX_OUTPUT_ID_LENGTH) {
            throw new IllegalArgumentException("output id too long");
        }
        buffer.writeUtf(highlight.outputId());
        buffer.writeVarInt(Math.max(0, highlight.durationSeconds()));
        buffer.writeBoolean(highlight.plateOnly());
        var network = highlight.networkId() == null ? "" : highlight.networkId();
        if (network.length() > PacketLimits.MAX_OUTPUT_ID_LENGTH) {
            throw new IllegalArgumentException("network id too long");
        }
        buffer.writeUtf(network);
    }

    public static Highlight read(FriendlyByteBuf buffer) {
        var dimension = buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH);
        var count = PacketLimits.checkedSize(buffer.readVarInt(), PacketLimits.MAX_HIGHLIGHT_POSITIONS, "positions");
        var positions = new ArrayList<BlockPos>(count);
        for (var i = 0; i < count; i++) {
            positions.add(buffer.readBlockPos());
        }
        var outputId = buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH);
        var durationSeconds = Math.max(0, buffer.readVarInt());
        // Packets written before the plate-only flag carry no trailing bytes.
        if (buffer.readableBytes() <= 0) {
            return new Highlight("", dimension, positions, outputId, durationSeconds, false);
        }
        var plateOnly = buffer.readBoolean();
        // Packets written before the network id carry no further bytes.
        var networkId = buffer.readableBytes() > 0 ? buffer.readUtf(PacketLimits.MAX_OUTPUT_ID_LENGTH) : "";
        return new Highlight(networkId, dimension, positions, outputId, durationSeconds, plateOnly);
    }

    private ProviderHighlightCodec() {
    }
}
