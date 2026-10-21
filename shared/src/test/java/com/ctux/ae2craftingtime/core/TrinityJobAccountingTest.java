package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TrinityJobAccountingTest {
    @Test void committedBatchAndCancellationAreJobOwned() {
        var ledger = new TrinityJobAccounting<String>();
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        ledger.begin(first);
        assertEquals(Map.of("glass", 12L, "bucket", 2L), ledger.start(first,
                Map.of("glass", BigInteger.valueOf(12), "bucket", BigInteger.TWO)));
        assertEquals(5, ledger.complete(first, "glass", BigInteger.valueOf(5)));
        assertEquals(3, ledger.withdraw(first, "glass", BigInteger.valueOf(3)));
        assertEquals(4, ledger.complete(first, "glass", BigInteger.valueOf(10)));
        assertEquals(0, ledger.complete(first, "glass", BigInteger.ONE));
        ledger.begin(second);
        assertEquals(0, ledger.complete(first, "bucket", BigInteger.ONE));
        assertEquals(Map.of("glass", 1L), ledger.start(second, Map.of("glass", BigInteger.ONE)));
        assertEquals(1, ledger.complete(second, "glass", BigInteger.ONE));
        ledger.clear();
        assertFalse(ledger.matches(second));
    }

    @Test void unrepresentableBatchFailsClosedWithoutPartialStarts() {
        var ledger = new TrinityJobAccounting<String>();
        var job = UUID.randomUUID();
        ledger.begin(job);
        assertTrue(ledger.start(job, Map.of("a", BigInteger.ONE,
                "b", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))).isEmpty());
        assertTrue(ledger.unsupported());
        assertFalse(ledger.reject(job));
        assertEquals(0, ledger.complete(job, "a", BigInteger.ONE));
        ledger.begin(UUID.randomUUID());
        assertFalse(ledger.unsupported());
    }

    @Test void aggregateOverflowAndInvalidAmountsRejectTheJob() {
        var ledger = new TrinityJobAccounting<String>();
        var job = UUID.randomUUID();
        ledger.begin(job);
        ledger.start(job, Map.of("a", BigInteger.valueOf(Long.MAX_VALUE)));
        assertTrue(ledger.start(job, Map.of("a", BigInteger.ONE)).isEmpty());
        assertTrue(ledger.unsupported());
        ledger.begin(UUID.randomUUID());
        assertTrue(ledger.start(job, Map.of("a", BigInteger.ONE)).isEmpty());
        assertTrue(ledger.start(job, Map.of()).isEmpty());
    }

    @Test void rejectsCrossVariantPendingBeforePublishingAnotherStart() {
        var ledger = new TrinityJobAccounting<String>();
        var job = UUID.randomUUID();
        ledger.begin(job);
        assertEquals(Map.of("variant-a", Long.MAX_VALUE), ledger.start(job,
                Map.of("variant-a", BigInteger.valueOf(Long.MAX_VALUE)), ignored -> true));
        assertTrue(ledger.start(job, Map.of("variant-b", BigInteger.ONE), pending ->
                BigInteger.valueOf(pending.getOrDefault("variant-a", 0L))
                        .add(BigInteger.valueOf(pending.getOrDefault("variant-b", 0L)))
                        .compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0).isEmpty());
        assertTrue(ledger.unsupported());
    }

    @Test void invalidNativeAmountsAndIdentityCannotPublishTiming() {
        var ledger = new TrinityJobAccounting<String>();
        var job = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> ledger.begin(null));
        ledger.begin(job);
        ledger.begin(job);
        assertFalse(ledger.matches(null));
        assertTrue(ledger.start(null, Map.of("glass", BigInteger.ONE)).isEmpty());
        assertTrue(ledger.start(job, null).isEmpty());
        assertEquals(Map.of(), ledger.start(job, Map.of("glass", BigInteger.ZERO)));
        assertFalse(ledger.unsupported());
        for (int variant = 0; variant < 3; variant++) {
            var values = new HashMap<String, BigInteger>();
            if (variant == 0) values.put(null, BigInteger.ONE);
            if (variant == 1) values.put("glass", null);
            if (variant == 2) values.put("glass", BigInteger.valueOf(-1));
            assertTrue(ledger.start(job, values).isEmpty());
            assertTrue(ledger.unsupported());
            ledger.begin(UUID.randomUUID());
            ledger.begin(job);
        }
    }

    @Test void invalidAcceptedOutputAndWithdrawalNeverCreateCompletion() {
        var ledger = new TrinityJobAccounting<String>();
        var job = UUID.randomUUID();
        ledger.begin(job);
        ledger.start(job, Map.of("glass", BigInteger.TEN));
        assertEquals(0, ledger.complete(null, "glass", BigInteger.ONE));
        assertEquals(0, ledger.complete(job, null, BigInteger.ONE));
        assertEquals(0, ledger.complete(job, "glass", null));
        assertEquals(0, ledger.complete(job, "glass", BigInteger.ZERO));
        assertEquals(0, ledger.complete(job, "glass", BigInteger.valueOf(-1)));
        assertEquals(0, ledger.complete(job, "other", BigInteger.ONE));
        assertFalse(ledger.reject(null));
        assertFalse(ledger.reject(UUID.randomUUID()));
        assertEquals(4, ledger.withdraw(job, "glass", BigInteger.valueOf(4)));
        assertEquals(6, ledger.complete(job, "glass", BigInteger.TEN));
        assertEquals(0, ledger.complete(job, "glass", BigInteger.ONE));
        assertTrue(ledger.reject(job));
        assertFalse(ledger.matches(job));
        ledger.begin(UUID.randomUUID());
        ledger.start(job, Map.of("glass", BigInteger.ONE));
        ledger.begin(job);
        ledger.start(job, Map.of("glass", BigInteger.ONE));
        assertEquals(0, ledger.complete(job, "glass", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)));
        assertTrue(ledger.unsupported());
    }
}
