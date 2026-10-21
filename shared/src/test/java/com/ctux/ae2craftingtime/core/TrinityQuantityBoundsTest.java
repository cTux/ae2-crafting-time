package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.Map;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class TrinityQuantityBoundsTest {
    private record Key(String id, int perUnit, String variant) { }

    private static boolean fits(Map<Key, BigInteger> amounts) {
        return TrinityQuantityBounds.fits(amounts, Key::id, Key::perUnit);
    }

    @Test void acceptsLargeFluidWhenFinalMillibucketsFit() {
        assertTrue(fits(Map.of(new Key("minecraft:water", 1000, "a"), BigInteger.valueOf(Long.MAX_VALUE))));
        assertTrue(fits(Map.of(new Key("minecraft:water", 81000, "a"), BigInteger.valueOf(Long.MAX_VALUE))));
    }

    @Test void rejectsRawLedgerOverflowAndTrueNormalizedOverflow() {
        assertFalse(fits(Map.of(new Key("minecraft:water", 81000, "a"),
                BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))));
        assertFalse(fits(Map.of(new Key("minecraft:water", 2, "a"), BigInteger.valueOf(Long.MAX_VALUE))));
        assertFalse(fits(Map.of(new Key("minecraft:water", 81000, "a"), BigInteger.ONE)));
        assertFalse(fits(Map.of(new Key("minecraft:stone", 1, "a"), BigInteger.valueOf(-1))));
    }

    @Test void distinctVariantsCannotOverflowTheSharedProfileIdentity() {
        assertFalse(fits(Map.of(new Key("minecraft:written_book", 1, "a"), BigInteger.valueOf(Long.MAX_VALUE),
                new Key("minecraft:written_book", 1, "b"), BigInteger.ONE)));
        assertTrue(fits(Map.of(new Key("minecraft:written_book", 1, "a"), BigInteger.ONE,
                new Key("minecraft:written_book", 1, "b"), BigInteger.ONE)));
        var projected = TrinityQuantityBounds.project(Map.of(
                new Key("minecraft:written_book", 1, "a"), BigInteger.ONE,
                new Key("minecraft:written_book", 1, "b"), BigInteger.valueOf(2)), Key::id, Key::perUnit)
                .orElseThrow();
        assertEquals(3L, projected.normalizedByOutput().get("minecraft:written_book"));
        assertEquals(Map.of(), TrinityQuantityBounds.project(Map.<Key, BigInteger>of(), Key::id, Key::perUnit)
                .orElseThrow().normalizedByOutput());
        assertEquals(0L, TrinityQuantityBounds.project(Map.of(new Key("minecraft:stone", 1, "a"),
                BigInteger.ZERO), Key::id, Key::perUnit).orElseThrow()
                .normalizedByOutput().get("minecraft:stone"));
        assertTrue(fits(Map.of(new Key("minecraft:water", 81000, "a"), BigInteger.valueOf(Long.MAX_VALUE),
                new Key("minecraft:water", 81000, "b"), BigInteger.valueOf(81))),
                "separate native AE keys need no shared raw long, only a shared normalized profiler row");
    }

    @Test void malformedNativeKeysAndAmountsFailClosed() {
        var values = new HashMap<Key, BigInteger>();
        values.put(null, BigInteger.ONE);
        assertFalse(fits(values));
        values.clear();
        values.put(new Key("minecraft:stone", 1, "a"), null);
        assertFalse(fits(values));
        assertFalse(fits(Map.of(new Key(null, 1, "a"), BigInteger.ONE)));
        assertFalse(fits(Map.of(new Key("", 1, "a"), BigInteger.ONE)));
        assertFalse(fits(Map.of(new Key("minecraft:stone", 0, "a"), BigInteger.ONE)));
    }
}
