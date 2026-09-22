package com.ctux.ae2craftingtime.core;

/** A menu identity counter never wraps or resumes after exhaustion. */
public final class PlanSummaryRevision {
    private long value;
    private boolean exhausted;

    public long advance() {
        if (exhausted) return 0;
        if (value == Long.MAX_VALUE) {
            exhausted = true;
            value = 0;
            return 0;
        }
        return ++value;
    }

    public long value() { return value; }
}
