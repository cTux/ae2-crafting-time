package com.ctux.ae2craftingtime.neoeco;

import static org.junit.jupiter.api.Assertions.*;

import com.ctux.ae2craftingtime.core.TtcEstimate;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class NeoEcoStatusPayloadTest {
    @Test
    void addsOnlyAnOptionalRowField() {
        var row = new CompoundTag();
        row.putBoolean("batched", true);
        assertNull(NeoEcoStatusPayload.read(row));
        var estimate = new TtcEstimate(712.3, true);
        NeoEcoStatusPayload.write(row, estimate);
        assertEquals(estimate, NeoEcoStatusPayload.read(row.copy()));
        assertTrue(row.getBoolean("batched"));
    }

    @Test
    void ignoresIncompleteOrWronglyTypedData() {
        var row = new CompoundTag();
        var ttc = new CompoundTag();
        row.put("ae2craftingtime:status_row", ttc);
        ttc.putDouble("millis", 3);
        assertNull(NeoEcoStatusPayload.read(row));
        ttc.putBoolean("reliable", false);
        ttc.putString("millis", "3");
        assertNull(NeoEcoStatusPayload.read(row));
        ttc.putDouble("millis", Double.NaN);
        assertNull(NeoEcoStatusPayload.read(row));
    }
}
