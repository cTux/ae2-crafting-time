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

class SavedEstimateTag1201Test {
    @Test
    void compoundTagJobBoundaryPreservesNativeAndAddonFields() {
        var root = new ProfileKey("network", "minecraft:stone");
        var bytes = CraftingJobEstimateCodec.encode(UUID.randomUUID(),
                new CraftingJobEstimate(root, Map.of(root, 3L), Map.of())).orElseThrow();
        var data = new CompoundTag();
        data.putString("cpuName", "Saved CPU");
        var job = new CompoundTag();
        job.putLong("remainingAmount", 3);
        data.put("job", job);
        SavedEstimateTag.write(data, Optional.of(bytes));
        assertEquals(3, data.getCompound("job").getLong("remainingAmount"));
        assertEquals("Saved CPU", data.getString("cpuName"));
        assertEquals(3L, CraftingJobEstimateCodec.decode(SavedEstimateTag.read(data)).orElseThrow()
                .graph().remainingAmounts().get(root));
        SavedEstimateTag.write(data, Optional.empty());
        assertFalse(data.getCompound("job").contains("ae2craftingtime:estimate"));
        assertEquals(3, data.getCompound("job").getLong("remainingAmount"));
        SavedEstimateTag.write(data, Optional.of(bytes));
        SavedEstimateTag.discard(data.getCompound("job"));
        assertFalse(data.getCompound("job").contains("ae2craftingtime:estimate"));
        assertEquals(3, data.getCompound("job").getLong("remainingAmount"));
    }

    @Test
    void delayedAddonJobKeepsItsOriginalTagUntilIdentityIsReady() {
        var root = new ProfileKey("network", "minecraft:stone");
        var id = UUID.randomUUID();
        var data = new CompoundTag();
        var job = new CompoundTag();
        job.putString("link", id.toString());
        job.putLong("remainingAmount", 3);
        data.put("job", job);
        SavedEstimateTag.write(data, CraftingJobEstimateCodec.encode(id,
                new CraftingJobEstimate(root, Map.of(root, 3L), Map.of())));
        var original = data.copy();
        var server = new Object();
        var lifecycle = new com.ctux.ae2craftingtime.core.CraftingJobRecovery.Lifecycle();
        lifecycle.begin(server);
        var recovery = new com.ctux.ae2craftingtime.core.CraftingJobRecovery(lifecycle);
        recovery.read(SavedEstimateTag.read(data), com.ctux.ae2craftingtime.core.OptionFeature.AE2_LIGHTNING_TECH);
        assertTrue(recovery.preservePendingTag(lifecycle.server(), true));
        assertEquals(original, data);
        assertTrue(recovery.attach(lifecycle.server(), null, null, null, true).isEmpty());
        lifecycle.ready(server);
        var restored = recovery.attach(server, id, root.outputId(), root.networkId(), true).orElseThrow();
        restored.complete(root, 1);
        SavedEstimateTag.write(data, CraftingJobEstimateCodec.encode(id, restored));
        assertEquals(2L, CraftingJobEstimateCodec.decode(SavedEstimateTag.read(data)).orElseThrow()
                .graph().remainingAmounts().get(root));
        assertEquals(id.toString(), job.getString("link"));
        assertEquals(3, job.getLong("remainingAmount"));
        recovery.read(SavedEstimateTag.read(data), com.ctux.ae2craftingtime.core.OptionFeature.AE2_LIGHTNING_TECH);
        lifecycle.disable(com.ctux.ae2craftingtime.core.OptionFeature.AE2_LIGHTNING_TECH);
        assertFalse(recovery.preservePendingTag(server, true));
        SavedEstimateTag.discard(job);
        assertEquals(id.toString(), job.getString("link"));
        assertEquals(3, job.getLong("remainingAmount"));
        assertNull(SavedEstimateTag.read(data));
    }

    @Test
    void noJobLegacyWrongTypeAndMalformedEstimateAreOptional() {
        var data = new CompoundTag();
        data.putString("cpuName", "Saved CPU");
        assertNull(SavedEstimateTag.read(data));
        SavedEstimateTag.write(data, Optional.of(new byte[] {1}));
        assertFalse(data.contains("job"));
        var job = new CompoundTag();
        job.putInt("unrelated", 7);
        data.put("job", job);
        assertTrue(CraftingJobEstimateCodec.decode(SavedEstimateTag.read(data)).isEmpty());
        job.putString("ae2craftingtime:estimate", "wrong type");
        assertTrue(CraftingJobEstimateCodec.decode(SavedEstimateTag.read(data)).isEmpty());
        job.putByteArray("ae2craftingtime:estimate", new byte[] {1, 2, 3});
        assertTrue(CraftingJobEstimateCodec.decode(SavedEstimateTag.read(data)).isEmpty());
        job.putByteArray("ae2craftingtime:estimate", new byte[CraftingJobEstimateCodec.MAX_BYTES + 1]);
        assertNull(SavedEstimateTag.read(data));
        assertEquals(7, job.getInt("unrelated"));
        assertEquals("Saved CPU", data.getString("cpuName"));
    }
}
