package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.ctux.ae2craftingtime.core.PersistedCraftSample;
import com.ctux.ae2craftingtime.core.PersistedOutputSamples;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileUnit;

class SuiteFixtureTest {
    @Test
    void restoredHistoriesIgnoreOnlyOuterOutputOrder() {
        var first = new PersistedOutputSamples(new ProfileKey("test:first"), ProfileUnit.ITEM,
                List.of(new PersistedCraftSample(1, 20), new PersistedCraftSample(2, 30)));
        var second = new PersistedOutputSamples(new ProfileKey("test:second"), ProfileUnit.MILLIBUCKET,
                List.of(new PersistedCraftSample(3, 40)));
        var original = List.of(first, second);

        assertTrue(SuiteFixture.sameSampleHistories(original, List.of(second, first)));
        assertFalse(SuiteFixture.sameSampleHistories(original, List.of(first)));
        assertFalse(SuiteFixture.sameSampleHistories(original, List.of(first, first)));
        assertFalse(SuiteFixture.sameSampleHistories(original, List.of(first,
                new PersistedOutputSamples(second.key(), ProfileUnit.ITEM, second.samples()))));
        assertFalse(SuiteFixture.sameSampleHistories(original, List.of(second,
                new PersistedOutputSamples(first.key(), first.unit(),
                        List.of(first.samples().get(1), first.samples().get(0))))));
        assertFalse(SuiteFixture.sameSampleHistories(original, List.of(second,
                new PersistedOutputSamples(first.key(), first.unit(),
                        List.of(new PersistedCraftSample(1, 21), first.samples().get(1))))));
    }
}
