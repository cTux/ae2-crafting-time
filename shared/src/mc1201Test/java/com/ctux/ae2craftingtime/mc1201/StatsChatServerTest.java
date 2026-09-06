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
    void detailsUseNormalizedPerUnitTiming() {
        var stats = new ProfileStats(2, 95, 0.1, 2, 100, ProfileUnit.ITEM, true, 2, 4,
                List.of(90L, 100L), List.of(9L, 1L));

        var contents = (TranslatableContents) StatsChatServer.details(stats).getContents();

        assertEquals("text.ae2craftingtime.chat.details", contents.getKey());
        assertEquals(7, contents.getArgs().length);
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
        assertEquals(3, contents.getArgs().length);
    }
}
