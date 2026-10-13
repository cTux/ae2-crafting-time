package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CpuListScaleOneObservationTest {
    @Test
    void fullHeightStatusRequiresActuallyDrawnTextInsideTheCell() {
        var cell = new Rect(100, 50, 203, 54);
        var semantic = new UiSnapshot.ObservedText("text.ae2craftingtime.status.amounts", "C4", List.of(), null);
        var drawn = new UiSnapshot.ObservedText("text.ae2craftingtime.status.amounts", "C4", List.of(),
                new Rect(270, 68, 12, 9));
        var row = new UiSnapshot.Row("minecraft:glass", 4, 0, cell, List.of(semantic));
        var empty = snapshot(row, List.of());
        assertFalse(CpuListTtcScenario.fullHeightStatusText(empty));
        assertTrue(CpuListTtcScenario.fullHeightStatusText(snapshot(row, List.of(drawn))));
        assertFalse(CpuListTtcScenario.fullHeightStatusText(snapshot(row, List.of(
                new UiSnapshot.ObservedText(drawn.key(), drawn.rendered(), List.of(), new Rect(270, 68, 12, 6))))));
        assertFalse(CpuListTtcScenario.fullHeightStatusText(snapshot(row, List.of(
                new UiSnapshot.ObservedText(drawn.key(), drawn.rendered(), List.of(), new Rect(310, 68, 12, 9))))));
    }

    private static UiSnapshot snapshot(UiSnapshot.Row row, List<UiSnapshot.ObservedText> drawn) {
        return new UiSnapshot("CraftingStatusScreen", "CraftingStatusMenu", new Rect(90, 40, 238, 184),
                2558, 1093, 1, 1, 0, List.of(row), drawn, List.of(), List.of(), List.of(), List.of());
    }
}
