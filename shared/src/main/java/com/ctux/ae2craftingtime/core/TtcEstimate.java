package com.ctux.ae2craftingtime.core;

/** Unrounded simulation time; display floors never change estimates. */
public record TtcEstimate(double durationMs, boolean reliable) {
    public TtcEstimate {
        if (!Double.isFinite(durationMs) || durationMs < 0) {
            throw new IllegalArgumentException("invalid TTC milliseconds");
        }
    }

    public boolean instant() {
        return reliable && durationMs > 0 && durationMs <= 5;
    }
}
