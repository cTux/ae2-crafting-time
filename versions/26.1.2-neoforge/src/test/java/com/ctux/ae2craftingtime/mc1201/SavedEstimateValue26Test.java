package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.*;

import com.ctux.ae2craftingtime.core.CraftingJobEstimate;
import com.ctux.ae2craftingtime.core.CraftingJobEstimateCodec;
import com.ctux.ae2craftingtime.core.ProfileKey;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.junit.jupiter.api.Test;

class SavedEstimateValue26Test {
    private static byte[] read(CompoundTag tag) {
        return SavedEstimateValue.read(TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag));
    }

    @Test
    void valueJobBoundaryPreservesUnrelatedFieldsAndRejectsLegacyOrMalformedData() {
        var root = new ProfileKey("network", "minecraft:stone");
        var bytes = CraftingJobEstimateCodec.encode(UUID.randomUUID(),
                new CraftingJobEstimate(root, Map.of(root, 3L), Map.of())).orElseThrow();
        var writer = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        writer.putString("cpuName", "Saved CPU");
        var job = writer.child("job");
        job.putLong("remainingAmount", 3);
        SavedEstimateValue.write(job, Optional.of(bytes));
        var tag = writer.buildResult();
        assertEquals("Saved CPU", tag.getStringOr("cpuName", ""));
        assertEquals(3, tag.getCompoundOrEmpty("job").getLongOr("remainingAmount", 0));
        assertEquals(3L, CraftingJobEstimateCodec.decode(read(tag)).orElseThrow()
                .graph().remainingAmounts().get(root));

        var legacy = new CompoundTag();
        legacy.putString("cpuName", "Legacy CPU");
        var legacyJob = new CompoundTag();
        legacyJob.putLong("remainingAmount", 9);
        legacy.put("job", legacyJob);
        assertNull(read(legacy));
        assertNull(read(new CompoundTag()));
        assertEquals(9, legacyJob.getLongOr("remainingAmount", 0));
        legacyJob.putByteArray("ae2craftingtime:estimate", new byte[] {1, 2});
        assertTrue(CraftingJobEstimateCodec.decode(read(legacy)).isEmpty());
        assertEquals("Legacy CPU", legacy.getStringOr("cpuName", ""));
    }

    @Test
    void emptyWriteDoesNotAddAJobFieldAndOversizedReadIsBounded() {
        var writer = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        var job = writer.child("job");
        job.putInt("unrelated", 7);
        SavedEstimateValue.write(job, Optional.empty());
        var tag = writer.buildResult();
        assertFalse(tag.getCompoundOrEmpty("job").contains("ae2craftingtime:estimate"));
        assertEquals(7, tag.getCompoundOrEmpty("job").getIntOr("unrelated", 0));
        tag.getCompoundOrEmpty("job").putByteArray("ae2craftingtime:estimate",
                new byte[CraftingJobEstimateCodec.MAX_BYTES + 1]);
        assertNull(read(tag));
        assertEquals(7, tag.getCompoundOrEmpty("job").getIntOr("unrelated", 0));
    }
}
