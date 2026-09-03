package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class PacketLimitsTest {
    @Test
    void snapshotFlagsMustBeValidRequestedKeys() {
        var requested = List.of("minecraft:stone");
        assertEquals(java.util.Set.of(), PacketLimits.checkedSubset(requested, List.of()));
        assertEquals(java.util.Set.of("minecraft:stone"), PacketLimits.checkedSubset(requested, requested));
        assertThrows(IllegalArgumentException.class,
                () -> PacketLimits.checkedSubset(requested, List.of("minecraft:dirt")));
        assertThrows(IllegalArgumentException.class,
                () -> PacketLimits.checkedSubset(requested, List.of("invalid")));
    }

    @Test
    void acceptsValidSizesAndOutputIds() {
        assertEquals(0, PacketLimits.checkedSize(0, 1, "values"));
        assertEquals(1, PacketLimits.checkedSize(1, 1, "values"));
        assertEquals("mod-name:path/to.output_1", PacketLimits.checkedOutputId("mod-name:path/to.output_1"));
    }

    @Test
    void rejectsInvalidSizes() {
        assertThrows(IllegalArgumentException.class, () -> PacketLimits.checkedSize(-1, 1, "values"));
        assertThrows(IllegalArgumentException.class, () -> PacketLimits.checkedSize(2, 1, "values"));
        assertThrows(IllegalArgumentException.class,
                () -> PacketLimits.checkedKeys(new ArrayList<>(java.util.Collections.nCopies(
                        PacketLimits.MAX_KEYS + 1, "minecraft:stone"))));
    }

    @Test
    void rejectsInvalidOutputIdsAndReturnsAnImmutableCopy() {
        assertThrows(IllegalArgumentException.class,
                () -> PacketLimits.checkedKeys(java.util.Arrays.asList("minecraft:stone", null)));
        assertThrows(IllegalArgumentException.class, () -> PacketLimits.checkedOutputId("MissingNamespace"));
        assertThrows(IllegalArgumentException.class,
                () -> PacketLimits.checkedOutputId("a:" + "x".repeat(PacketLimits.MAX_OUTPUT_ID_LENGTH)));

        var checked = PacketLimits.checkedKeys(List.of("minecraft:stone"));
        assertThrows(UnsupportedOperationException.class, () -> checked.add("minecraft:dirt"));
    }
}
