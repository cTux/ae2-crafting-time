package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctux.ae2craftingtime.core.ProfileStats;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class StatsChatServerTest {
    @Test
    void summaryRetainsIntegralAmountsAboveDoublePrecision() {
        long amount = 9_007_199_254_740_993L;
        var stats = new ProfileStats(1, 20, 0.123456789, 2.46913578, 20, ProfileUnit.ITEM);
        var summary = StatsChatServer.summary("minecraft:stone", amount, stats);
        var contents = (TranslatableContents) summary.getContents();
        assertEquals(amount, contents.getArgs()[1]);
        assertEquals("9007199254740993", contents.getArgs()[1].toString());
        var details = (TranslatableContents) summary.getSiblings().get(1).getContents();
        assertEquals("0.123456789", details.getArgs()[1]);
        assertEquals("2.46913578", details.getArgs()[3]);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(ProfileUnit.class)
    void bothStoredRatesKeepTheirFullPrecisionAndResourceUnits(ProfileUnit unit) {
        for (boolean samples : new boolean[] {false, true}) {
            var stats = new ProfileStats(2, 95, 0.123456789, 1234567.89, 100, unit, true, 2, 4,
                    samples ? List.of(90L, 100L) : List.of(), samples ? List.of(9L, 1L) : List.of());
            var contents = (TranslatableContents) StatsChatServer.details(stats).getContents();
            int start = samples ? 5 : 1;
            assertEquals("0.123456789", contents.getArgs()[start]);
            assertEquals("1234567.89", contents.getArgs()[start + 2]);
            assertEquals(unit.translationKey(), ((TranslatableContents)
                    ((Component) contents.getArgs()[start + 1]).getContents()).getKey());
            assertEquals(unit.translationKey(), ((TranslatableContents)
                    ((Component) contents.getArgs()[start + 3]).getContents()).getKey());
        }
    }
    @Test
    void detailsUseNormalizedPerUnitTiming() {
        var stats = new ProfileStats(2, 95, 0.1, 2, 100, ProfileUnit.ITEM, true, 2, 4,
                List.of(90L, 100L), List.of(9L, 1L));

        var contents = (TranslatableContents) StatsChatServer.details(stats).getContents();

        assertEquals("text.ae2craftingtime.chat.details", contents.getKey());
        assertEquals(9, contents.getArgs().length);
        assertEquals("55", contents.getArgs()[2]);
        assertEquals("100", contents.getArgs()[4]);
        assertEquals("text.ae2craftingtime.unit.item.singular",
                ((TranslatableContents) ((Component) contents.getArgs()[1]).getContents()).getKey());
    }

    @Test
    void invalidDetailsFallBackToRateAndCount() {
        var stats = new ProfileStats(1, 20, 1, 20, 20, ProfileUnit.MANA);

        var contents = (TranslatableContents) StatsChatServer.details(stats).getContents();

        assertEquals("text.ae2craftingtime.chat.details.rate", contents.getKey());
        assertEquals(5, contents.getArgs().length);
    }
}
