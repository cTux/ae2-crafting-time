package com.ctux.ae2craftingtime.testdriver;

final class TestDriverLifecycleGuard {
    private boolean active;

    boolean enter() {
        if (active) return false;
        active = true;
        return true;
    }

    void exit() {
        active = false;
    }
}
