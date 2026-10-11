package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ThroughputNumbersTest {
    @ParameterizedTest
    @CsvSource({"0.004,<0.01", "0.005,0.01", "1,1.00", "999.99,999.99",
            "999.995,1000.00", "1000,~1k", "1234,~1.23k", "999994,~999.99k",
            "999995,~1M", "1000000,~1M", "1000000000,~1B", "1000000000000,~1T",
            "1000000000000000,~1P", "1000000000000000000,~1E",
            "92233720368547760,~92.23P", "1844674407370955300,~1.84E",
            "9.99994e20,~999.99E", "9.99995e20,~1e21", "1e21,~1e21",
            "1.234e21,~1.23e21", "9.999e21,~1e22", "1.7976931348623157e308,~1.8e308"})
    void compactBoundaries(double value, String expected) {
        assertEquals(expected, ThroughputNumbers.compact(value));
        assertEquals(expected, ThroughputNumbers.hover(value, true));
    }

    @Test
    void invalidRatesAreUnknownInEveryMode() {
        for (double value : new double[] {0, -0.0, -1, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertEquals("?", ThroughputNumbers.compact(value));
            assertEquals("?", ThroughputNumbers.full(value));
            assertEquals("?", ThroughputNumbers.hover(value, true));
            assertEquals("?", ThroughputNumbers.hover(value, false));
        }
    }

    @Test
    void fullRetainsStoredPrecisionWithoutExponentOrFixedRounding() {
        for (double value : new double[] {Double.MIN_VALUE, 0.004, 0.123456789, 1,
                999.995, 1e18, 1e21, Double.MAX_VALUE}) {
            assertEquals(BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(),
                    ThroughputNumbers.full(value));
        }
        assertEquals("<0.01", ThroughputNumbers.compact(Double.MIN_VALUE));
        assertEquals("0.00", ThroughputNumbers.hover(0.004, false));
        assertEquals("1234.00", ThroughputNumbers.hover(1234, false));
    }
}
