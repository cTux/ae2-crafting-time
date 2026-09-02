package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CraftingRowStateTest {
    @ParameterizedTest
    @CsvSource({
            "true, 1, 0, 0, true",
            "false, 1, 0, 0, false",
            "true, 0, 0, 0, false",
            "true, -1, 0, 0, false",
            "true, 1, 1, 0, false",
            "true, 1, 0, 1, false",
            "true, 1, -1, 0, false",
            "true, 1, 0, -1, false",
            "true, 9223372036854775807, 0, 0, true"
    })
    void requiresRejectedStorageAndOnlyStoredItems(boolean rejected, long stored, long active, long pending,
            boolean expected) {
        assertEquals(expected, CraftingRowState.noSpace(rejected, stored, active, pending));
    }
}
