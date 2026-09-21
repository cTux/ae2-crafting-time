package com.ctux.ae2craftingtime.mc1201.net;

import com.ctux.ae2craftingtime.core.PlanRecurrenceChunk;
import net.minecraft.network.FriendlyByteBuf;

public record PlanStoredVariantsS2C(long updateRevision, PlanRecurrenceChunk chunk) {
    public static void encode(PlanStoredVariantsS2C packet, FriendlyByteBuf buffer) {
        PlanStoredVariantsCodec.write(buffer, packet.updateRevision, packet.chunk);
    }
    public static PlanStoredVariantsS2C decode(FriendlyByteBuf buffer) {
        var decoded = PlanStoredVariantsCodec.read(buffer);
        return new PlanStoredVariantsS2C(decoded.updateRevision(), decoded.chunk());
    }
    public void handle() {
        com.ctux.ae2craftingtime.mc1201.PlanStoredVariantsClient.receive(chunk, updateRevision);
    }
}
