package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TtcTextTest {
    @Test
    void statsLinesLeaveOutValuesAlreadyShownByTheTooltip() {
        var stats = new ProfileStats(4, 646.5, 0.01, 0.18, 109, ProfileUnit.ITEM);

        assertEquals(4, TtcText.statsLines(stats, Optional.empty()).size());
    }
}
