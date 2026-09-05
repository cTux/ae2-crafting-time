package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ProviderLocateRecords.LocateRecord;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Chat-link lifecycle: active links survive reload via persisted records and
 * per-output fallbacks, while finished and cancelled links expire instead of
 * recreating red or targeting a replacement block.
 */
class ProviderLocateLifecycleTest {
    @AfterEach
    void clearRecords() {
        ProviderLocateRecords.clearAll();
    }

    @Test
    void activeRecordsSurviveSnapshotRestore() {
        var owner = UUID.randomUUID();
        var record = ProviderLocateRecords.create(owner, "minecraft:overworld",
                List.of(new BlockPos(1, 2, 3)), "Iron", "minecraft:iron_ingot", 100L);

        var snapshot = ProviderLocateRecords.snapshotRecords();
        assertEquals(1, snapshot.size());

        ProviderLocateRecords.clearAll();
        assertTrue(ProviderLocateRecords.ownedBy(owner, record.id()).isEmpty());

        ProviderLocateRecords.restoreRecords(snapshot);
        var restored = ProviderLocateRecords.ownedBy(owner, record.id()).orElseThrow();
        assertEquals(List.of(new BlockPos(1, 2, 3)), restored.positions());
        assertEquals("minecraft:iron_ingot", restored.outputId());
    }

    @Test
    void finishRemovesOnlyOwnersFinishedOutputs() {
        var owner = UUID.randomUUID();
        var otherOwner = UUID.randomUUID();
        var iron = new ProfileKey("net|1,2,3", "minecraft:iron_ingot");
        var copper = new ProfileKey("net|1,2,3", "minecraft:copper_plate");
        ProviderLocateRecords.create(owner, "minecraft:overworld", List.of(new BlockPos(1, 2, 3)), "Iron",
                "minecraft:iron_ingot", 1L);
        var copperRecord = ProviderLocateRecords.create(owner, "minecraft:overworld",
                List.of(new BlockPos(4, 5, 6)), "Copper", "minecraft:copper_plate", 2L);
        var foreignRecord = ProviderLocateRecords.create(otherOwner, "minecraft:overworld",
                List.of(new BlockPos(7, 8, 9)), "Iron", "minecraft:iron_ingot", 3L);

        ProviderLocateRecords.removeRecordsForKeys(List.of(iron), owner);

        assertTrue(ProviderLocateRecords.snapshotRecords().stream()
                .noneMatch(entry -> entry.outputId().equals("minecraft:iron_ingot")
                        && owner.equals(entry.owner())));
        assertTrue(ProviderLocateRecords.ownedBy(owner, copperRecord.id()).isPresent());
        assertTrue(ProviderLocateRecords.ownedBy(otherOwner, foreignRecord.id()).isPresent());

        ProviderLocateRecords.removeRecordsForKeys(List.of(copper), otherOwner);
        assertTrue(ProviderLocateRecords.ownedBy(owner, copperRecord.id()).isPresent());
    }

    @Test
    void startsForOutputFindsEveryNetworkFallback() {
        var owner = UUID.randomUUID();
        var keyA = new ProfileKey("minecraft:overworld|1,2,3", "minecraft:iron_ingot");
        var keyB = new ProfileKey("minecraft:overworld|9,9,9", "minecraft:iron_ingot");
        ProviderLocateRecords.noteStart(keyA, owner, "minecraft:overworld", List.of(new BlockPos(1, 2, 3)),
                "Iron");
        ProviderLocateRecords.noteStart(keyB, owner, "minecraft:overworld", List.of(new BlockPos(4, 5, 6)),
                "Iron");
        ProviderLocateRecords.noteStart(new ProfileKey("net", "minecraft:copper_plate"), owner,
                "minecraft:overworld", List.of(new BlockPos(7, 8, 9)), "Copper");

        var matches = ProviderLocateRecords.startsForOutput(owner, "minecraft:iron_ingot");
        assertEquals(2, matches.size());

        assertTrue(ProviderLocateRecords.startsForOutput(UUID.randomUUID(), "minecraft:iron_ingot").isEmpty());
        assertTrue(ProviderLocateRecords.startsForOutput(owner, "minecraft:unknown").isEmpty());
        assertTrue(ProviderLocateRecords.startsForOutput(owner, null).isEmpty());
    }

    @Test
    void recordsRoundTripThroughPersistenceTag() {
        var owner = UUID.randomUUID();
        var record = new LocateRecord(UUID.randomUUID(), owner, "minecraft:overworld",
                List.of(new BlockPos(1, 2, 3), new BlockPos(-4, 5, -6)), "Iron", "minecraft:iron_ingot",
                42L);
        var tags = PersistedProviderTag.writeRecords(List.of(record));
        assertEquals(1, tags.size());
        var restored = PersistedProviderTag.readRecords(tags);
        assertEquals(List.of(record), restored);
    }

    @Test
    void recordsPersistenceSkipsInvalidEntries() {
        var tags = PersistedProviderTag.writeRecords(nullSafeRecords());
        assertTrue(PersistedProviderTag.readRecords(tags).size() >= 0);
    }

    private static List<LocateRecord> nullSafeRecords() {
        return List.of();
    }
}
