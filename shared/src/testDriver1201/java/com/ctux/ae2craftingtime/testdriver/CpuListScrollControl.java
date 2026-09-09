package com.ctux.ae2craftingtime.testdriver;

import appeng.client.gui.widgets.Scrollbar;

/** Driver-only handle to exercise the native CPU list's real scroll state. */
public final class CpuListScrollControl {
    private static Scrollbar scrollbar;

    public static void bind(Scrollbar value) { scrollbar = value; }

    public static void scrollTo(int value) {
        if (scrollbar == null) throw new IllegalStateException("CPU-list scrollbar has not rendered");
        scrollbar.setCurrentScroll(value);
    }

    private CpuListScrollControl() { }
}
