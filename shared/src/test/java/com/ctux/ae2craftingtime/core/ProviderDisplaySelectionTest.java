package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProviderDisplaySelectionTest {
    @Test
    void selectsFirstRetainedValuePerDimensionAndPosition() {
        var shared = new Position(1, 2, 3);
        var firstOnly = new Position(4, 5, 6);
        var secondOnly = new Position(7, 8, 9);
        var first = new Value("network-a", "first");
        var second = new Value("network-b", "second");
        var otherDimension = new Value("network-c", "third");

        assertEquals(List.of(), ProviderDisplaySelection.firstByPosition(List.of()));
        assertEquals(List.of(new ProviderDisplaySelection.Selected<>(first, "overworld", firstOnly)),
                ProviderDisplaySelection.firstByPosition(List.of(
                        new ProviderDisplaySelection.Candidate<>(first, "overworld", List.of(firstOnly)))));
        assertEquals(List.of(
                new ProviderDisplaySelection.Selected<>(first, "overworld", shared),
                new ProviderDisplaySelection.Selected<>(first, "overworld", firstOnly),
                new ProviderDisplaySelection.Selected<>(second, "overworld", secondOnly),
                new ProviderDisplaySelection.Selected<>(otherDimension, "nether", shared)),
                ProviderDisplaySelection.firstByPosition(List.of(
                        new ProviderDisplaySelection.Candidate<>(first, "overworld",
                                List.of(shared, shared, firstOnly)),
                        new ProviderDisplaySelection.Candidate<>(second, "overworld",
                                List.of(shared, secondOnly)),
                        new ProviderDisplaySelection.Candidate<>(otherDimension, "nether", List.of(shared)))));
    }

    private record Position(int x, int y, int z) {
    }

    private record Value(String network, String output) {
    }
}
