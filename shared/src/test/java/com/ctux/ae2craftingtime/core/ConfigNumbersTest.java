package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigNumbersTest {
    @Test
    void nonfiniteAndMalformedValuesKeepTheFallback() {
        for (var value : List.of("NaN", "Infinity", "-Infinity", "1e309", "-1e309", "not-a-number")) {
            assertEquals(7.5, ConfigNumbers.parseDouble(value, 7.5, 1.0, 1000.0));
        }
        assertEquals(4.0, ConfigNumbers.parseDouble("NaN", 4.0, 1.0, 1000.0));
    }

    @Test
    void finiteValuesAreKeptOrClampedToTheBounds() {
        assertEquals(1.0, ConfigNumbers.parseDouble("0.5", 4.0, 1.0, 1000.0));
        assertEquals(1.0, ConfigNumbers.parseDouble("1.0", 4.0, 1.0, 1000.0));
        assertEquals(4.25, ConfigNumbers.parseDouble("4.25", 4.0, 1.0, 1000.0));
        assertEquals(1000.0, ConfigNumbers.parseDouble("1000.0", 4.0, 1.0, 1000.0));
        assertEquals(1000.0, ConfigNumbers.parseDouble("1000.5", 4.0, 1.0, 1000.0));
    }
}
