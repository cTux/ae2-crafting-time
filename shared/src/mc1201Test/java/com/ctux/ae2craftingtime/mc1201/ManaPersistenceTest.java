package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.CraftProfiler;
import com.ctux.ae2craftingtime.core.PersistedCraftSample;
import com.ctux.ae2craftingtime.core.PersistedOutputSamples;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManaPersistenceTest {
    @Test
    void rawManaRoundTripsAndLegacyNbtMigratesOnlyOnce() {
        var key = new ProfileKey("grid", "botania:mana");
        var raw = List.of(new PersistedOutputSamples(key, ProfileUnit.MANA,
                List.of(new PersistedCraftSample(1, 20))));
        assertEquals(raw, PersistedSamplesTag.readOutputs(PersistedSamplesTag.writeOutputs(raw)));
        var legacy = List.of(new PersistedOutputSamples(key, ProfileUnit.MILLIBUCKET,
                List.of(new PersistedCraftSample(1, 20))));
        var profiler = new CraftProfiler(10);
        profiler.loadSamples(PersistedSamplesTag.readOutputs(PersistedSamplesTag.writeOutputs(legacy)));
        var converted = List.of(new PersistedOutputSamples(key, ProfileUnit.MANA,
                List.of(new PersistedCraftSample(1000, 20))));
        assertEquals(converted, profiler.snapshotSamples());
        profiler.loadSamples(PersistedSamplesTag.readOutputs(PersistedSamplesTag.writeOutputs(converted)));
        assertEquals(converted, profiler.snapshotSamples());
    }
}
