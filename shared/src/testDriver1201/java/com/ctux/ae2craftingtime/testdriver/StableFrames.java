package com.ctux.ae2craftingtime.testdriver;

import java.util.Objects;

public final class StableFrames<T> {
    private final int required;
    private T previous;
    private int count;

    public StableFrames(int required) {
        if (required < 1) {
            throw new IllegalArgumentException("required must be positive");
        }
        this.required = required;
    }

    public boolean observe(T value) {
        count = Objects.equals(previous, value) ? count + 1 : 1;
        previous = value;
        return count >= required;
    }

    public void reset() {
        previous = null;
        count = 0;
    }
}
