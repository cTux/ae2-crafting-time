package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.TtcEstimate;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

public final class TtcEstimatePacketCodec {
    public static void write(FriendlyByteBuf buffer, Optional<TtcEstimate> estimate) {
        buffer.writeBoolean(estimate.isPresent());
        estimate.ifPresent(value -> {
            buffer.writeDouble(value.durationMs());
            buffer.writeBoolean(value.reliable());
        });
    }

    public static Optional<TtcEstimate> read(FriendlyByteBuf buffer) {
        return buffer.readBoolean()
                ? Optional.of(new TtcEstimate(buffer.readDouble(), buffer.readBoolean())) : Optional.empty();
    }

    private TtcEstimatePacketCodec() { }
}
