package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CraftingRowStateTest {
    @ParameterizedTest
    @CsvSource({"ttc,true", "ttc_delayed,true", "waiting,true", "no_space,true", "no_provider,true", "no_power,true",
            "no_provider.explanation,false", "details_hint,false", "unknown,false"})
    void onlyCompactStatusLinesReceiveBadges(String suffix, boolean expected) {
        assertEquals(expected, CraftingRowState.isBadge("text.ae2craftingtime." + suffix));
    }

    @ParameterizedTest
    @CsvSource({"1,true,true", "1,false,false", "0,true,false", "-1,true,false",
            "9223372036854775807,true,true"})
    void missingProviderRequiresPendingWorkRegardlessOfActiveBatches(long pending, boolean missing,
            boolean expected) {
        for (var reason : CraftingBlockReason.values()) {
            assertEquals(expected ? reason : null, CraftingRowState.blockReason(pending, missing ? reason : null));
        }
    }

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
