package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ctux.ae2craftingtime.core.PersistedCraftSample;
import com.ctux.ae2craftingtime.core.PersistedOutputSamples;
import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.core.ProfileUnit;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class Ae2CraftingTimeSavedDataTest {
    @Test
    void usesRequestedWorldDataFileId() {
        assertEquals("ae2-crafting-time", Ae2CraftingTimeSavedData.FILE_ID);
    }

    @Test
    void savedDataRoundTripsSamples() {
        var data = new Ae2CraftingTimeSavedData();
        data.replaceFrom(List.of(new PersistedOutputSamples(
                new ProfileKey("minecraft:iron_plate"),
                ProfileUnit.ITEM,
                List.of(new PersistedCraftSample(2, 20)))));

        var tag = data.save(new CompoundTag(), null);
        var loaded = Ae2CraftingTimeSavedData.load(tag, null);

        assertEquals(data.samples(), loaded.samples());
    }
}
