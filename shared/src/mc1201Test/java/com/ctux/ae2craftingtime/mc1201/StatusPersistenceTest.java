package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctux.ae2craftingtime.core.PersistedOutputStatus;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.StatusKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

class StatusPersistenceTest {
    @Test
    void statusesRoundTrip() {
        var raw = List.of(
                new PersistedOutputStatus(new ProfileKey("net", "minecraft:iron_plate"), StatusKind.DELAYED, 300,
                        20.5, 100),
                new PersistedOutputStatus(new ProfileKey("net", "minecraft:copper_plate"), StatusKind.WAITING, 0, 0,
                        120),
                new PersistedOutputStatus(new ProfileKey("net", "minecraft:gear"), StatusKind.NO_PROVIDER, 0, 0, 0),
                new PersistedOutputStatus(new ProfileKey("net", "minecraft:stick"), StatusKind.NO_POWER, 0, 0, 0));
        assertEquals(raw, PersistedStatusTag.readStatuses(PersistedStatusTag.writeStatuses(raw)));
    }

    @Test
    void statusesReadSkipsInvalidEntries() {
        var tags = new ListTag();
        tags.add(entry("net", "minecraft:iron_plate", "delayed", 10, 5.0, 1));
        var missingKey = new CompoundTag();
        missingKey.putString("networkId", "net");
        missingKey.putString("kind", "delayed");
        tags.add(missingKey);
        tags.add(entry("net", "not an id", "delayed", 0, 0, 0));
        tags.add(entry("net", "minecraft:stone", "exploded", 0, 0, 0));
        tags.add(new CompoundTag());

        var read = PersistedStatusTag.readStatuses(tags);
        assertEquals(1, read.size());
        assertEquals(new ProfileKey("net", "minecraft:iron_plate"), read.get(0).key());
        assertEquals(StatusKind.DELAYED, read.get(0).kind());
        assertTrue(PersistedStatusTag.readStatuses(new ListTag()).isEmpty());

        var junk = new ListTag();
        junk.add(StringTag.valueOf("junk"));
        assertTrue(PersistedStatusTag.readStatuses(junk).isEmpty());
    }

    @Test
    void statusesWriteSkipsNullsAndCapsEntries() {
        var many = new ArrayList<PersistedOutputStatus>();
        for (var i = 0; i < 260; i++) {
            many.add(new PersistedOutputStatus(new ProfileKey("net", "minecraft:stone"), StatusKind.WAITING, 0, 0,
                    i));
        }
        many.add(null);
        var tags = PersistedStatusTag.writeStatuses(many);
        assertEquals(256, tags.size());
        assertEquals(256, PersistedStatusTag.readStatuses(tags).size());
    }

    private static CompoundTag entry(String networkId, String key, String kind, long idle, double typical,
            long acceptedAt) {
        var tag = new CompoundTag();
        tag.putString("networkId", networkId);
        tag.putString("key", key);
        tag.putString("kind", kind);
        tag.putLong("idleTicks", idle);
        tag.putDouble("typicalTicks", typical);
        tag.putLong("acceptedAtTick", acceptedAt);
        return tag;
    }
}
