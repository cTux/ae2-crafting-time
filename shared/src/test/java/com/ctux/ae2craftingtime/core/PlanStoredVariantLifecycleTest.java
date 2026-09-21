package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.*;
import static com.ctux.ae2craftingtime.core.PlanStoredVariantLifecycle.Action.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanStoredVariantLifecycleTest {
    @Test void coalescesRelevantNotificationsAndSuspendsAnOldPlanAcrossGridChanges() {
        var state = new PlanStoredVariantLifecycle();
        var plan = new Object(); var grid = new Object();
        assertEquals(INSTALL, state.broadcast(plan, grid, true, Set.of("item")));
        assertTrue(state.observes());
        assertEquals(NONE, state.broadcast(plan, grid, true, Set.of()));
        state.changed("other");
        assertEquals(NONE, state.broadcast(plan, grid, true, Set.of()));
        state.changed("item"); state.changed("item");
        assertEquals(REFRESH, state.broadcast(plan, grid, true, Set.of()));
        assertEquals(NONE, state.broadcast(plan, grid, true, Set.of()));
        assertEquals(CLEAR, state.broadcast(plan, null, true, Set.of()));
        assertFalse(state.observes());
        state.changed("item");
        assertEquals(NONE, state.broadcast(plan, grid, true, Set.of()));
        assertEquals(INSTALL, state.broadcast(new Object(), grid, true, Set.of("item")));
        assertEquals(INSTALL, state.broadcast(plan, grid, true, Set.of()));
        assertFalse(state.observes());
        assertEquals(CLEAR, state.broadcast(plan, new Object(), true, Set.of()));
        assertEquals(CLOSE, state.broadcast(plan, grid, false, Set.of("item")));
        assertFalse(state.observes());
        assertEquals(CLOSE, state.broadcast(null, grid, true, Set.of()));
        assertEquals(INSTALL, state.broadcast(plan, null, true, Set.of("item")));
        assertEquals(NONE, state.broadcast(plan, grid, true, Set.of("item")));
        state.close(); state.close();
        assertEquals(INSTALL, state.broadcast(plan, grid, true, Set.of("item")));
    }

    @Test void rendersOnlyEnabledFlaggedMissingItems() {
        assertTrue(PlanStoredVariantLifecycle.show(true, 1, true, true));
        assertFalse(PlanStoredVariantLifecycle.show(false, 1, true, true));
        assertFalse(PlanStoredVariantLifecycle.show(true, 0, true, true));
        assertFalse(PlanStoredVariantLifecycle.show(true, 1, false, true));
        assertFalse(PlanStoredVariantLifecycle.show(true, 1, true, false));
    }
}
