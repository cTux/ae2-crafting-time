package com.ctux.ae2craftingtime.core;

import java.util.function.BiConsumer;

/** Per-native-summary, per-chunk ordering guard for replacement masks. */
public final class PlanStoredVariantClientState {
    private long[] watermarks = new long[0];

    public void reset(int trustedEntryCount) {
        watermarks = new long[trustedEntryCount == 0 ? 0 : 1 + (trustedEntryCount - 1) / 256];
    }

    public boolean apply(PlanRecurrenceChunk chunk, long updateRevision, int containerId,
            long summaryRevision, int trustedEntryCount, BiConsumer<Integer, Boolean> replace) {
        if (updateRevision <= 0 || !chunk.validFor(containerId, summaryRevision, trustedEntryCount)) return false;
        var index = chunk.offset() / 256;
        if (index >= watermarks.length || updateRevision <= watermarks[index]) return false;
        watermarks[index] = updateRevision;
        var bits = chunk.rows();
        for (int row = 0; row < chunk.rowCount(); row++) replace.accept(chunk.offset() + row, bits.get(row));
        return true;
    }
}
