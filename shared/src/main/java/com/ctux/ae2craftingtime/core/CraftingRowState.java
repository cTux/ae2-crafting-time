package com.ctux.ae2craftingtime.core;

public final class CraftingRowState {
    private CraftingRowState() {
    }

    public static boolean noSpace(boolean cantStoreItems, long stored, long active, long pending) {
        return cantStoreItems && stored > 0 && active == 0 && pending == 0;
    }
}
