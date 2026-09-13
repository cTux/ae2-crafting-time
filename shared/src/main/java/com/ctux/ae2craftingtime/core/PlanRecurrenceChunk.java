package com.ctux.ae2craftingtime.core;

import java.util.BitSet;

public record PlanRecurrenceChunk(int containerId, long revision, int entryCount, int offset,
        int rowCount, byte[] mask) {
    public static final int ROWS_PER_CHUNK = PacketLimits.MAX_KEYS;
    public static final int MASK_BYTES = ROWS_PER_CHUNK / Byte.SIZE;

    public PlanRecurrenceChunk {
        mask = mask.clone();
    }

    @Override
    public byte[] mask() {
        return mask.clone();
    }

    public boolean validFor(int actualContainerId, long actualRevision, int actualEntryCount) {
        if (containerId != actualContainerId || revision <= 0 || revision != actualRevision
                || entryCount != actualEntryCount || offset < 0 || offset % ROWS_PER_CHUNK != 0
                || rowCount <= 0 || rowCount > ROWS_PER_CHUNK || mask.length != MASK_BYTES
                || offset > entryCount || rowCount != Math.min(ROWS_PER_CHUNK, entryCount - offset)) {
            return false;
        }
        return BitSet.valueOf(mask).nextSetBit(rowCount) < 0;
    }

    public BitSet rows() {
        return BitSet.valueOf(mask);
    }
}
