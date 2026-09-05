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

    @Test
    void storedDimensionSurvivesSnapshotRestore() {
        var key = new ProfileKey("minecraft:overworld|0,0,0", "minecraft:iron_ingot");
        var owner = UUID.randomUUID();
        ProviderLocateRecords.noteStart(key, owner, "minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "Iron");
        var snapshot = ProviderLocateRecords.snapshotStarts();
        assertEquals(1, snapshot.size());
        assertEquals("minecraft:overworld", snapshot.get(0).dimensionId());

        ProviderLocateRecords.clearAll();
        ProviderLocateRecords.restoreStarts(snapshot);
        var restored = ProviderLocateRecords.startFor(key).orElseThrow();
        assertEquals("minecraft:overworld", restored.dimensionId());
        assertEquals(List.of(new BlockPos(1, 2, 3)), restored.positions());
    }

    @Test
    void identicalOutputsOnDifferentNetworksStayIndependent() {
        var keyA = new ProfileKey("minecraft:overworld|1,2,3", "minecraft:iron_ingot");
        var keyB = new ProfileKey("minecraft:overworld|9,9,9", "minecraft:iron_ingot");
        var owner = UUID.randomUUID();
        ProviderLocateRecords.noteStart(keyA, owner, "minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "Iron");
        ProviderLocateRecords.noteStart(keyB, owner, "minecraft:overworld", List.of(new BlockPos(4, 5, 6)),
                "Iron");
        assertEquals(2, ProviderLocateRecords.snapshotStarts().size());

        ProviderLocateRecords.removeStarts(List.of(keyA));
        var remaining = ProviderLocateRecords.snapshotStarts();
        assertEquals(1, remaining.size());
        assertEquals(keyB, remaining.get(0).key());
    }
}
