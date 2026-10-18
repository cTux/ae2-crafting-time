package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.CraftingJobEstimateCodec;
import com.mojang.serialization.Codec;
import java.nio.ByteBuffer;
import java.util.Optional;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Optional saved-job field for the 26.x ValueInput/ValueOutput API. */
public final class SavedEstimateValue {
    private static final String KEY = "ae2craftingtime:estimate";

    public static byte[] read(ValueInput data) {
        var stored = data.child("job").flatMap(job -> job.read(KEY, Codec.BYTE_BUFFER)).orElse(null);
        if (stored == null || stored.remaining() > CraftingJobEstimateCodec.MAX_BYTES) return null;
        var bytes = new byte[stored.remaining()];
        stored.duplicate().get(bytes);
        return bytes;
    }

    public static void write(ValueOutput job, Optional<byte[]> bytes) {
        bytes.ifPresent(value -> job.store(KEY, Codec.BYTE_BUFFER, ByteBuffer.wrap(value)));
    }

    private SavedEstimateValue() { }
}
