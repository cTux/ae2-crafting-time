package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CpuTtcDisplayOrderTest {
    @Test
    void sortsCpuGroupsStablyAndBypassesTtcForAe2OrMissingChannel() {
        var raw = List.of(row(1, false, null, "idle"), row(2, true, null, "unknown"),
                row(3, true, 5L, "first tie"), row(4, true, 5L, "second tie"), row(5, true, 2L, "short"));
        var state = new CpuTtcDisplayOrder.State();
        assertEquals(List.of(1, 2, 3, 4, 5), serials(state.display(raw, Row::serial, Row::busy,
                Row::secondsValue, 1, 0, true)));
        assertEquals(List.of(1, 2, 3, 4, 5), serials(state.display(raw, Row::serial, Row::busy,
                Row::secondsValue, 1, 2, false)));
        assertEquals(List.of(5, 3, 4, 2, 1), serials(state.display(raw, Row::serial, Row::busy,
                Row::secondsValue, 1, 1, true)));
        assertEquals(List.of(3, 4, 5, 2, 1), serials(state.display(raw, Row::serial, Row::busy,
                Row::secondsValue, 1, 2, true)));
    }

    @Test
    void invalidatesOnRevisionModeChannelAndRawOrderWhileReturningFreshRows() {
        var state = new CpuTtcDisplayOrder.State();
        var raw = List.of(row(1, true, 1L, "old"), row(2, true, 2L, "two"));
        assertEquals(List.of(2, 1), serials(state.display(raw, Row::serial, Row::busy,
                Row::secondsValue, 1, 2, true)));
        var renamed = List.of(row(1, true, 3L, "new"), row(2, true, 2L, "two"));
        var unchangedRevision = state.display(renamed, Row::serial, Row::busy, Row::secondsValue, 1, 2, true);
        assertEquals(List.of(2, 1), serials(unchangedRevision));
        assertEquals("new", unchangedRevision.get(1).name());
        assertEquals(List.of(1, 2), serials(state.display(renamed, Row::serial, Row::busy,
                Row::secondsValue, 2, 2, true)));
        assertEquals(List.of(2, 1), serials(state.display(List.of(renamed.get(1), renamed.get(0)), Row::serial, Row::busy,
                Row::secondsValue, 2, 0, true)));
        assertEquals(List.of(1, 2), serials(state.display(renamed, Row::serial, Row::busy,
                Row::secondsValue, 2, 2, false)));
        assertEquals(List.of(1, 2), serials(state.display(renamed, Row::serial, Row::busy,
                Row::secondsValue, 2, 2, true)));
        assertEquals(List.of(1, 2), serials(state.display(List.of(renamed.get(1), renamed.get(0)), Row::serial,
                Row::busy, Row::secondsValue, 2, 2, true)));
        assertEquals(List.of(1), serials(state.display(List.of(row(1, true, 1L, "first"),
                row(1, true, 1L, "latest")), Row::serial, Row::busy, Row::secondsValue, 3, 0, true)));
        state.clear();
        assertEquals(List.of(2, 1), serials(state.display(raw, Row::serial, Row::busy,
                Row::secondsValue, 1, 2, true)));
    }

    @Test
    void validatesInputsAndCentralizesStaleHitAndDrawScrollRules() {
        var state = new CpuTtcDisplayOrder.State();
        assertThrows(IllegalArgumentException.class,
                () -> state.display(null, Row::serial, Row::busy, Row::secondsValue, 0, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> state.display(List.of(), null, Row::busy, Row::secondsValue, 0, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> state.display(List.of(), Row::serial, null, Row::secondsValue, 0, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> state.display(List.of(), Row::serial, Row::busy, null, 0, 0, true));
        assertThrows(IllegalArgumentException.class,
                () -> state.display(List.of(), Row::serial, Row::busy, Row::secondsValue, 0, -1, true));
        assertThrows(IllegalArgumentException.class,
                () -> state.display(List.of(), Row::serial, Row::busy, Row::secondsValue, 0, 3, true));

        var drawn = new CpuTtcCache.CpuView(1, "job", 2, 10);
        assertTrue(CpuTtcDisplayOrder.hitCurrent(drawn, new CpuTtcCache.CpuView(1, "job", 2, 10)));
        assertFalse(CpuTtcDisplayOrder.hitCurrent(null, drawn));
        assertFalse(CpuTtcDisplayOrder.hitCurrent(drawn, null));
        assertFalse(CpuTtcDisplayOrder.hitCurrent(drawn, new CpuTtcCache.CpuView(2, "job", 2, 10)));
        assertFalse(CpuTtcDisplayOrder.hitCurrent(drawn, new CpuTtcCache.CpuView(1, "other", 2, 10)));
        assertFalse(CpuTtcDisplayOrder.hitCurrent(drawn, new CpuTtcCache.CpuView(1, "job", 3, 10)));
        assertFalse(CpuTtcDisplayOrder.hitCurrent(drawn, new CpuTtcCache.CpuView(1, "job", 2, 9)));
        assertEquals(-1, CpuTtcDisplayOrder.inputScroll(-1));
        assertEquals(0, CpuTtcDisplayOrder.inputScroll(0));
        assertEquals(4, CpuTtcDisplayOrder.inputScroll(4));
    }

    @Test
    void emptyAndAllUnknownRowsRetainStableAe2OrderWithinBusyGroups() {
        var state = new CpuTtcDisplayOrder.State();
        assertTrue(state.display(List.<Row>of(), Row::serial, Row::busy, Row::secondsValue, 1, 2, true).isEmpty());
        var raw = List.of(row(1, false, null, "idle first"), row(2, true, null, "busy first"),
                row(3, true, null, "busy second"), row(4, false, null, "idle second"));
        for (var mode : List.of(1, 2)) {
            assertEquals(List.of(2, 3, 1, 4), serials(state.display(raw, Row::serial, Row::busy,
                    Row::secondsValue, 2, mode, true)));
        }
        assertEquals(List.of(1, 2, 3, 4), serials(raw));
    }

    private static List<Integer> serials(List<Row> rows) {
        return rows.stream().map(Row::serial).toList();
    }

    private static Row row(int serial, boolean busy, Long seconds, String name) {
        return new Row(serial, busy, seconds, name);
    }

    private record Row(int serial, boolean busy, Long seconds, String name) {
        OptionalLong secondsValue() {
            return seconds == null ? OptionalLong.empty() : OptionalLong.of(seconds);
        }
    }
}
