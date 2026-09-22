package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;

/** Builds changed replacement chunks without packet-controlled allocation. */
public final class PlanStoredVariantUpdateState {
    private int entryCount;
    private byte[][] sent = new byte[0][];
    private long revision;
    private boolean exhausted;

    public void reset(int trustedEntryCount) {
        entryCount = trustedEntryCount;
        sent = new byte[trustedEntryCount == 0 ? 0 : 1 + (trustedEntryCount - 1) / 256][];
        revision = 0;
        exhausted = false;
    }

    public List<Update> replace(Set<Integer> rows) {
        if (exhausted) return List.of();
        // Reserve the last revision for clearing every outstanding warning.
        if (revision >= Long.MAX_VALUE - 1) {
            exhausted = true;
            revision = Long.MAX_VALUE;
            var terminal = new ArrayList<Update>();
            for (int index = 0; index < sent.length; index++) {
                if (sent[index] != null && !BitSet.valueOf(sent[index]).isEmpty()) {
                    sent[index] = new byte[32];
                    terminal.add(new Update(index * 256, Math.min(256, entryCount - index * 256),
                            sent[index], revision));
                }
            }
            return List.copyOf(terminal);
        }
        var changed = new ArrayList<Update>();
        for (int index = 0; index < sent.length; index++) {
            int offset = index * 256;
            int count = Math.min(256, entryCount - offset);
            var bits = new BitSet(count);
            for (int row = 0; row < count; row++) if (rows.contains(offset + row)) bits.set(row);
            var mask = new byte[32];
            var encoded = bits.toByteArray();
            System.arraycopy(encoded, 0, mask, 0, encoded.length);
            if (sent[index] == null && bits.isEmpty() || Arrays.equals(sent[index], mask)) continue;
            changed.add(new Update(offset, count, mask, 0));
        }
        if (changed.isEmpty()) return List.of();
        revision++;
        var result = new ArrayList<Update>(changed.size());
        for (var update : changed) {
            sent[update.offset() / 256] = update.mask();
            result.add(new Update(update.offset(), update.rowCount(), update.mask(), revision));
        }
        return List.copyOf(result);
    }

    public boolean exhausted() { return exhausted; }
    public record Update(int offset, int rowCount, byte[] mask, long revision) {}
}
