package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.List;

public record StallDiagnostic(
        long idleTicks,
        double typicalDurationTicks,
        int activeBatches,
        int usedParallelSlots,
        int totalParallelSlots) {
    public List<Hint> hints(long scheduledAmount) {
        var hints = new ArrayList<Hint>(2);
        if (scheduledAmount > 0 && totalParallelSlots > 0) {
            hints.add(usedParallelSlots >= totalParallelSlots
                    ? Hint.ADD_CRAFTING_CO_PROCESSORS
                    : Hint.ADD_PARALLEL_PROVIDERS);
        }
        hints.add(Hint.SPEED_UP_MACHINE);
        return List.copyOf(hints);
    }

    public enum Hint {
        ADD_PARALLEL_PROVIDERS,
        SPEED_UP_MACHINE,
        ADD_CRAFTING_CO_PROCESSORS
    }
}
