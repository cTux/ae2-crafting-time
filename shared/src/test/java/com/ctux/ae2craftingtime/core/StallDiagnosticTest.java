package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StallDiagnosticTest {
    @Test
    void suggestsParallelProvidersWhenDispatchSlotsRemain() {
        var diagnostic = new StallDiagnostic(960, 240, 1, 1, 4);

        assertEquals(List.of(
                StallDiagnostic.Hint.ADD_PARALLEL_PROVIDERS,
                StallDiagnostic.Hint.SPEED_UP_MACHINE), diagnostic.hints(7));
    }

    @Test
    void suggestsCoProcessorsWhenDispatchBudgetIsFull() {
        var diagnostic = new StallDiagnostic(960, 240, 4, 4, 4);

        assertEquals(List.of(
                StallDiagnostic.Hint.ADD_CRAFTING_CO_PROCESSORS,
                StallDiagnostic.Hint.SPEED_UP_MACHINE), diagnostic.hints(7));
    }

    @Test
    void onlySuggestsMachineSpeedWhenNothingIsStillScheduled() {
        var diagnostic = new StallDiagnostic(960, 240, 1, 1, 4);

        assertEquals(List.of(StallDiagnostic.Hint.SPEED_UP_MACHINE), diagnostic.hints(0));
    }

    @Test
    void onlySuggestsMachineSpeedWithoutCapacityData() {
        var diagnostic = new StallDiagnostic(960, 240, 1, 0, 0);

        assertEquals(List.of(StallDiagnostic.Hint.SPEED_UP_MACHINE), diagnostic.hints(7));
    }
}
