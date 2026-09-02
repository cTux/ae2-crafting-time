package com.ctux.ae2craftingtime.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProfileAmountsTest {
    @Test
    void manaKeepsRawPrecisionAndOtherKeysKeepTheirUnits() {
        assertEquals(ProfileUnit.MANA, ProfileAmounts.unit("botania:mana", 1_000_000));
        for (var amount : new long[] { 0, 1, 999, 1_000, 1_000_000, Long.MAX_VALUE }) {
            assertEquals(amount, ProfileAmounts.normalize("botania:mana", 1_000_000, amount));
        }
        assertEquals(ProfileUnit.ITEM, ProfileAmounts.unit("minecraft:stone", 1));
        assertEquals(ProfileUnit.ITEM, ProfileAmounts.unit("other:unscaled", 0));
        assertEquals(ProfileUnit.MILLIBUCKET, ProfileAmounts.unit("minecraft:water", 1000));
        assertEquals(3, ProfileAmounts.normalize("minecraft:stone", 1, 3));
        assertEquals(3, ProfileAmounts.normalize("other:unscaled", 0, 3));
        assertEquals(81, ProfileAmounts.normalize("minecraft:water", 1000, 81));
        assertEquals(1000, ProfileAmounts.normalize("minecraft:water", 81_000, 81_000));
    }

    @Test
    void legacyManaMigrationIsBoundedAndIdempotent() {
        var key = new ProfileKey("grid", "botania:mana");
        var legacy = new PersistedOutputSamples(key, ProfileUnit.MILLIBUCKET, List.of(
                new PersistedCraftSample(1, 20),
                new PersistedCraftSample(Long.MAX_VALUE / 1000, 40),
                new PersistedCraftSample(Long.MAX_VALUE / 1000 + 1, 60),
                new PersistedCraftSample(0, 80),
                new PersistedCraftSample(-1, 100)));
        var converted = ProfileAmounts.migrate(legacy);
        assertEquals(new PersistedOutputSamples(key, ProfileUnit.MANA, List.of(
                new PersistedCraftSample(1000, 20),
                new PersistedCraftSample(Long.MAX_VALUE / 1000 * 1000, 40))), converted);
        assertSame(converted, ProfileAmounts.migrate(converted));
        var water = new PersistedOutputSamples(new ProfileKey("grid", "minecraft:water"),
                ProfileUnit.MILLIBUCKET, List.of(new PersistedCraftSample(1000, 20)));
        assertSame(water, ProfileAmounts.migrate(water));
        var profiler = new CraftProfiler(10);
        profiler.loadSamples(List.of(legacy));
        assertEquals(List.of(converted), profiler.snapshotSamples());
    }

    @Test
    void unitsUseTheSameTranslationKeysInChatAndTooltips() {
        assertEquals("text.ae2craftingtime.unit.item", ProfileUnit.ITEM.translationKey());
        assertEquals("text.ae2craftingtime.unit.millibucket", ProfileUnit.MILLIBUCKET.translationKey());
        assertEquals("text.ae2craftingtime.unit.mana", ProfileUnit.MANA.translationKey());
    }
}
