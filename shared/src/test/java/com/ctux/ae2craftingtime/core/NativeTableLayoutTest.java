package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NativeTableLayoutTest {
    @Test
    void oneColumnFitsTheOriginalFootprintAndTracksScrollAndBorders() {
        var plan = NativeTableLayout.of(1, 3, 67, 22, 5, true);
        assertEquals(new NativeTableLayout(1, 203, 54, 2, 1f), plan);
        assertEquals(0, plan.badgeVerticalPadding());
        // A later badge cannot paint over the preceding nine-pixel glyph row.
        assertTrue(10 - plan.badgeVerticalPadding() >= 9);
        assertTrue(5 * 9 + 4 + 2 * plan.badgeVerticalPadding() <= plan.cellHeight());
        assertEquals(0, plan.entryAt(0, 0, 0));
        assertEquals(2, plan.entryAt(202, 31, 2));
        assertEquals(3, plan.entryAt(0, 55, 2));
        assertEquals(-1, plan.entryAt(203, 0, 0));
        assertEquals(-1, plan.entryAt(0, 54, 0));
        assertEquals(-1, plan.entryAt(0, 110, 0));
        assertEquals(-1, plan.entryAt(-1, 0, 0));
        assertEquals(-1, plan.entryAt(0, -1, 0));
        assertEquals(-1, plan.entryAt(0, 0, -1));
        assertEquals(0, plan.scrollableRows(0));
        assertEquals(0, plan.scrollableRows(2));
        assertEquals(3, plan.scrollableRows(5));
        assertEquals(Integer.MAX_VALUE - 2, plan.scrollableRows(Integer.MAX_VALUE));
        assertEquals(new NativeTableLayout(1, 203, 54, 2, 1f),
                NativeTableLayout.of(1, 3, 67, 22, 6, true));
        assertEquals(new NativeTableLayout(2, 1, 1, 1, .5f),
                NativeTableLayout.of(1, 2, 1, 1, 1, true));
    }

    @Test
    void otherScalesAndDisabledModeRetainNativeIndexing() {
        var nativeLayout = new NativeTableLayout(3, 67, 22, 5, .5f);
        assertEquals(2, nativeLayout.badgeVerticalPadding());
        assertEquals(nativeLayout, NativeTableLayout.of(2, 3, 67, 22, 5, true));
        assertEquals(nativeLayout, NativeTableLayout.of(1, 3, 67, 22, 5, false));
        assertEquals(5, nativeLayout.entryAt(136, 23, 0));
        assertEquals(20, nativeLayout.entryAt(136, 23, 5));
        assertEquals(-1, nativeLayout.entryAt(204, 23, 0));
        assertEquals(-1, nativeLayout.entryAt(0, 115, 0));
        assertEquals(0, nativeLayout.scrollableRows(15));
        assertEquals(1, nativeLayout.scrollableRows(16));
        assertThrows(IllegalArgumentException.class, () -> NativeTableLayout.of(1, 0, 67, 22, 5, true));
        assertThrows(IllegalArgumentException.class, () -> NativeTableLayout.of(1, 3, 0, 22, 5, true));
        assertThrows(IllegalArgumentException.class, () -> NativeTableLayout.of(1, 3, 67, 0, 5, true));
        assertThrows(IllegalArgumentException.class, () -> NativeTableLayout.of(1, 3, 67, 22, 0, true));
    }

    @Test
    void presentationRequiresRendererAndMatchingConsumer() {
        PresentationHooks.tableRenderer(false);
        PresentationHooks.planInput(false);
        PresentationHooks.statusInput(false);
        PresentationHooks.cpuCard(false);
        assertFalse(PresentationHooks.table(false));
        assertFalse(PresentationHooks.table(true));
        assertFalse(PresentationHooks.cpu());
        PresentationHooks.tableRenderer(true);
        assertFalse(PresentationHooks.table(false));
        PresentationHooks.planInput(true);
        assertTrue(PresentationHooks.table(false));
        assertFalse(PresentationHooks.table(true));
        PresentationHooks.statusInput(true);
        assertTrue(PresentationHooks.table(true));
        PresentationHooks.cpuCard(true);
        assertTrue(PresentationHooks.cpu());
        PresentationHooks.tableRenderer(false);
        assertFalse(PresentationHooks.table(false));
        assertFalse(PresentationHooks.table(true));
    }
}
