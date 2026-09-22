package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DisplayKeySelectionTest {
    @Test
    void missingAndAmbiguousAreDifferent() {
        assertEquals(new DisplayKeySelection<>(false, null), DisplayKeySelection.from(null));
        assertEquals(new DisplayKeySelection<>(false, null), DisplayKeySelection.from(List.of()));
        assertEquals(new DisplayKeySelection<>(false, null), DisplayKeySelection.from(Arrays.asList((String) null)));
        assertEquals(new DisplayKeySelection<>(true, "fluid"),
                DisplayKeySelection.from(Arrays.asList(null, "fluid")));
        assertEquals(new DisplayKeySelection<>(true, "fluid"), DisplayKeySelection.from(List.of("fluid", "fluid")));
        assertEquals(new DisplayKeySelection<>(true, null), DisplayKeySelection.from(List.of("item", "fluid")));
        assertEquals("saved", DisplayKeySelection.<String>from(List.of()).orFallback("saved"));
        assertEquals(null, DisplayKeySelection.from(List.of("item", "fluid")).orFallback("saved"));
    }

    @Test
    void resourceTypeAndVariantArePartOfDistinctIdentity() {
        record Key(String type, String id, int variant) { }
        var fluid = new Key("fluid", "mod:same_id", 0);
        var item = new Key("item", "mod:same_id", 0);
        var variant = new Key("fluid", "mod:same_id", 1);
        assertEquals(fluid, DisplayKeySelection.from(List.of(fluid, fluid)).orFallback(item));
        assertEquals(null, DisplayKeySelection.from(List.of(fluid, item)).orFallback(fluid));
        assertEquals(null, DisplayKeySelection.from(List.of(fluid, variant)).orFallback(fluid));
    }
}
