package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.*;

import com.ctux.ae2craftingtime.core.CraftingJobEstimate;
import com.ctux.ae2craftingtime.core.CraftingJobEstimateCodec;
import com.ctux.ae2craftingtime.core.ProfileKey;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class SavedEstimateTag1211Test {
    @Test
    void registryAwareCompoundTagJobBoundaryPreservesUnrelatedFields() {
        var root = new ProfileKey("network", "minecraft:stone");
        var bytes = CraftingJobEstimateCodec.encode(UUID.randomUUID(),
                new CraftingJobEstimate(root, Map.of(root, 3L), Map.of())).orElseThrow();
        var data = new CompoundTag();
        data.putString("cpuName", "Saved CPU");
        var job = new CompoundTag();
        job.putLong("remainingAmount", 3);
        data.put("job", job);
        SavedEstimateTag.write(data, Optional.of(bytes));
        assertEquals(3L, CraftingJobEstimateCodec.decode(SavedEstimateTag.read(data)).orElseThrow()
                .graph().remainingAmounts().get(root));
        assertEquals(3, job.getLong("remainingAmount"));
        assertEquals("Saved CPU", data.getString("cpuName"));
        SavedEstimateTag.write(data, Optional.empty());
        assertFalse(job.contains("ae2craftingtime:estimate"));
        assertEquals(3, job.getLong("remainingAmount"));
        job.putByteArray("ae2craftingtime:estimate", new byte[] {1, 2});
        assertTrue(CraftingJobEstimateCodec.decode(SavedEstimateTag.read(data)).isEmpty());
        job.putByteArray("ae2craftingtime:estimate", new byte[CraftingJobEstimateCodec.MAX_BYTES + 1]);
        assertNull(SavedEstimateTag.read(data));
        var absent = new CompoundTag();
        SavedEstimateTag.write(absent, Optional.of(bytes));
        assertNull(SavedEstimateTag.read(absent));
        assertFalse(absent.contains("job"));
    }
}
