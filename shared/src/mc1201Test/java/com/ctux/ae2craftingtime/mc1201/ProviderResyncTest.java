package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.ProfileKey;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProviderResyncTest {
    @AfterEach
    void clearStarts() {
        ProviderLocateRecords.clearAll();
    }

    @Test
    void dimensionFromNetworkIdSplitsControllerSuffix() {
        assertEquals("minecraft:overworld",
                ProfilerBridge.dimensionFromNetworkId("minecraft:overworld|1,2,3"));
        assertEquals("minecraft:the_nether", ProfilerBridge.dimensionFromNetworkId("minecraft:the_nether"));
        assertEquals("", ProfilerBridge.dimensionFromNetworkId(""));
        assertEquals("", ProfilerBridge.dimensionFromNetworkId(null));
    }

    @Test
    void snapshotExcludesEmptyPositions() {
        var key = new ProfileKey("minecraft:overworld|0,0,0", "minecraft:iron_ingot");
        ProviderLocateRecords.noteStart(key, UUID.randomUUID(), List.of(), "Iron");
        assertTrue(ProviderLocateRecords.snapshotStarts().isEmpty());
    }

    @Test
    void removeStartsForgetsFinishedKeys() {
        var key = new ProfileKey("minecraft:overworld|0,0,0", "minecraft:iron_ingot");
        ProviderLocateRecords.noteStart(key, UUID.randomUUID(), List.of(new BlockPos(1, 2, 3)), "Iron");
        assertEquals(1, ProviderLocateRecords.snapshotStarts().size());
        ProviderLocateRecords.removeStarts(List.of(key));
        assertTrue(ProviderLocateRecords.snapshotStarts().isEmpty());
    }
}
