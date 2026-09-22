package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import appeng.api.stacks.AEFluidKey;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ProviderDisplayKeyTagTest {
    @Test
    void typedFluidRoundTripsAndBadOrMissingDataLosesOnlyTheIcon() {
        assumeTrue(ProviderDisplayKeyTestContext.nativeKeysAvailable(), "native registries require a running loader");
        var key = AEFluidKey.of(Fluids.WATER);
        var context = ProviderDisplayKeyTestContext.persistence();
        var tag = ProviderDisplayKeyTag.write(key, context);
        assertEquals(key, ProviderDisplayKeyTag.read(tag, context));
        assertNull(ProviderDisplayKeyTag.read(null, NbtOps.INSTANCE));
        assertNull(ProviderDisplayKeyTag.read(new CompoundTag(), NbtOps.INSTANCE));
        assertNull(ProviderDisplayKeyTag.write(key, JsonOps.INSTANCE));
    }

    @Test
    void savedDataCodecPreservesTypedProvidersWithRegistryOps() {
        assumeTrue(ProviderDisplayKeyTestContext.nativeKeysAvailable(), "native registries require a running loader");
        var registries = net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(
                net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        var start = new ProviderLocateRecords.StoredStart(new com.ctux.ae2craftingtime.core.ProfileKey("net", "minecraft:water"),
                java.util.UUID.randomUUID(), "minecraft:overworld", java.util.List.of(new net.minecraft.core.BlockPos(1, 2, 3)),
                "Water", AEFluidKey.of(Fluids.WATER));
        var data = new Ae2CraftingTimeSavedData();
        data.replaceProviderStarts(java.util.List.of(start));
        var encoded = Ae2CraftingTimeSavedData.TYPE.codec().encodeStart(ops, data).result().orElseThrow();
        var decoded = Ae2CraftingTimeSavedData.TYPE.codec().parse(ops, encoded).result().orElseThrow();
        assertEquals(java.util.List.of(start), decoded.providerStarts());
    }
}
