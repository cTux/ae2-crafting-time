package com.ctux.ae2craftingtime.core;

public final class CraftingRowState {
    private static final java.util.Set<String> BADGE_KEYS = java.util.Set.of(
            "text.ae2craftingtime.ttc", "text.ae2craftingtime.ttc_delayed", "text.ae2craftingtime.waiting",
            "text.ae2craftingtime.no_space", "text.ae2craftingtime.no_provider", "text.ae2craftingtime.no_power");

    public static boolean isBadge(String translationKey) {
        return BADGE_KEYS.contains(translationKey);
    }

    private CraftingRowState() {
    }

    public static boolean noSpace(boolean cantStoreItems, long stored, long active, long pending) {
        return cantStoreItems && stored > 0 && active == 0 && pending == 0;
    }

    public static CraftingBlockReason blockReason(long pending, CraftingBlockReason reason) {
        return pending > 0 ? reason : null;
    }
}
