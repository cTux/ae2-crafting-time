package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class GridNetworkIdsTest {
    @Test
    void networkIdUsesLowestControllerAnchor() {
        var id = GridNetworkIds.fromControllers("minecraft:overworld", Arrays.asList(
                null,
                new BlockPos(20, 64, 20),
                new BlockPos(5, 70, 5),
                new BlockPos(5, 65, 9),
                new BlockPos(5, 65, 7)));

        assertEquals("minecraft:overworld|5,65,7", id);
    }

    @Test
    void networkIdStaysEmptyWithoutDimensionOrControllerAnchor() {
        var anchor = List.of(new BlockPos(5, 65, 7));

        assertEquals("", GridNetworkIds.fromControllers(null, anchor));
        assertEquals("", GridNetworkIds.fromControllers(" ", anchor));
        assertEquals("", GridNetworkIds.fromControllers("minecraft:overworld", List.of()));
    }
}
