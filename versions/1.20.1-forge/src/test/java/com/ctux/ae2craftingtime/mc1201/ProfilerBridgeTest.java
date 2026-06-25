package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class ProfilerBridgeTest {
    @Test
    void networkIdUsesLowestControllerAnchor() {
        var id = GridNetworkIds.fromControllers("minecraft:overworld", List.of(
                new BlockPos(20, 64, 20),
                new BlockPos(5, 70, 5),
                new BlockPos(5, 65, 7)));

        assertEquals("minecraft:overworld|5,65,7", id);
    }

    @Test
    void networkIdStaysEmptyWithoutControllerAnchor() {
        assertEquals("", GridNetworkIds.fromControllers("minecraft:overworld", List.of()));
    }
}
