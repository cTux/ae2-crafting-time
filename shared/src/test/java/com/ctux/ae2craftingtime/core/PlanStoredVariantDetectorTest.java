package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanStoredVariantDetectorTest {
    @Test void detectsOnlyPositiveMissingNearMatchesWithoutExactStock() {
        var rows = List.of(
                new PlanStoredVariantDetector.Missing<>("cell:new", "cell", 3),
                new PlanStoredVariantDetector.Missing<>("tank:new", "tank", 1),
                new PlanStoredVariantDetector.Missing<>("cell:old", "cell", 4),
                new PlanStoredVariantDetector.Missing<>("cell:zero", "cell", 0));
        var available = List.of(
                new PlanStoredVariantDetector.Stored<>("cell:old", "cell", 1),
                new PlanStoredVariantDetector.Stored<>("cell:other", "cell", 2),
                new PlanStoredVariantDetector.Stored<>("tank:old", "tank", 0));
        assertEquals(Set.of(0), PlanStoredVariantDetector.detect(rows, available));
        assertEquals(Set.of(), PlanStoredVariantDetector.detect(rows, List.of()));
        assertEquals(Set.of(), PlanStoredVariantDetector.detect(rows,
                List.of(new PlanStoredVariantDetector.Stored<>("cell:new", "cell", 1),
                        new PlanStoredVariantDetector.Stored<>("cell:old", "cell", 1))));
        assertEquals(Set.of(), PlanStoredVariantDetector.detect(rows,
                List.of(new PlanStoredVariantDetector.Stored<>("cell:old", "cell", -1),
                        new PlanStoredVariantDetector.Stored<>("cell:other", "cell", 0))));
        assertEquals(Set.of(), PlanStoredVariantDetector.detect(rows,
                List.of(new PlanStoredVariantDetector.Stored<>("same visible name", "different registered item", 1))));
        assertEquals(Set.of(), PlanStoredVariantDetector.detect(List.of(), available));
        assertEquals(Set.of(1), PlanStoredVariantDetector.detect(
                java.util.Arrays.asList(null, rows.get(0)), available));
    }
}
