package com.ctux.ae2craftingtime.neoeco;

import com.ctux.ae2craftingtime.core.NeoEcoStatusRows;
import com.ctux.ae2craftingtime.core.TtcEstimate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class NeoEcoStatusPayload {
    private static final String TTC = "ae2craftingtime:status_row";

    private NeoEcoStatusPayload() {}

    public static void write(CompoundTag row, TtcEstimate estimate) {
        var tag = new CompoundTag();
        tag.putDouble("millis", estimate.durationMs());
        tag.putBoolean("reliable", estimate.reliable());
        row.put(TTC, tag);
    }

    public static TtcEstimate read(CompoundTag row) {
        var tag = row.getCompound(TTC);
        if (!tag.contains("millis", Tag.TAG_DOUBLE) || !tag.contains("reliable", Tag.TAG_BYTE)) return null;
        return NeoEcoStatusRows.decode(tag.getDouble("millis"), tag.getBoolean("reliable"));
    }
}
