package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CoreValueTypesTest {
    @Test
    void profileKeyNormalizesNetworkAndRejectsBlankOutput() {
        assertEquals("", new ProfileKey(null, "minecraft:stone").networkId());
        assertThrows(IllegalArgumentException.class, () -> new ProfileKey("network", null));
        assertThrows(IllegalArgumentException.class, () -> new ProfileKey("network", " "));
    }

    @Test
    void statsEntryConstructorsNormalizeMissingDiagnostics() {
        var key = new ProfileKey("minecraft:stone");
        var stats = new ProfileStats(1, 1, 1, 1, 1, ProfileUnit.ITEM);

        assertTrue(new StatsEntry(key, stats).accuracy().isEmpty());
        assertTrue(new StatsEntry(key, stats, Optional.empty()).stall().isEmpty());
        var normalized = new StatsEntry(key, stats, null, null);
        assertTrue(normalized.accuracy().isEmpty());
        assertTrue(normalized.stall().isEmpty());
    }

    @Test
    void persistedCollectionsAreDefensiveCopies() {
        var samples = new ArrayList<>(List.of(new PersistedCraftSample(1, 1)));
        var output = new PersistedOutputSamples(new ProfileKey("minecraft:stone"), ProfileUnit.ITEM, samples);
        samples.clear();

        assertEquals(1, output.samples().size());
        assertThrows(UnsupportedOperationException.class, () -> output.samples().clear());
    }

    @Test
    void exposesBothChatActions() {
        assertEquals(List.of(StatsChatAction.SHOW, StatsChatAction.RESET), List.of(StatsChatAction.values()));
    }
}
