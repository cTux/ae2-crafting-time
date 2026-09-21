package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PlanSummaryRevisionTest {
    @Test void neverWrapsOrResumesAnExhaustedMenu() throws Exception {
        var revision = new PlanSummaryRevision();
        assertEquals(0, revision.value());
        assertEquals(1, revision.advance());
        var field = PlanSummaryRevision.class.getDeclaredField("value");
        field.setAccessible(true);
        field.setLong(revision, Long.MAX_VALUE - 1);
        assertEquals(Long.MAX_VALUE, revision.advance());
        assertEquals(0, revision.advance());
        assertEquals(0, revision.value());
        assertEquals(0, revision.advance());
    }
}
