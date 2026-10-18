package com.ctux.ae2craftingtime.mc1201;

import java.util.Optional;
import com.ctux.ae2craftingtime.core.CraftingJobEstimateCodec;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;

/** Optional job-owned estimate field; leaves every other save field to the CPU. */
public final class SavedEstimateTag {
    private static final String KEY = "ae2craftingtime:estimate";

    public static byte[] read(CompoundTag data) {
        if (!data.contains("job", 10) || !(data.getCompound("job").get(KEY) instanceof ByteArrayTag bytes)
                || bytes.getAsByteArray().length > CraftingJobEstimateCodec.MAX_BYTES) return null;
        return bytes.getAsByteArray();
    }

    public static void write(CompoundTag data, Optional<byte[]> bytes) {
        if (!data.contains("job", 10)) return;
        var job = data.getCompound("job");
        if (bytes.isPresent()) job.putByteArray(KEY, bytes.get());
        else job.remove(KEY);
    }

    public static void discard(CompoundTag job) { job.remove(KEY); }

    private SavedEstimateTag() { }
}
