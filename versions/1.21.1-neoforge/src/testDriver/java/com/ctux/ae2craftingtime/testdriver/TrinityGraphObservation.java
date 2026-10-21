package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.core.CraftingJobEstimate;
import com.ctux.ae2craftingtime.core.ProfileKey;

/** Read-only driver evidence from the actual restored estimate after a native accepted output. */
public final class TrinityGraphObservation {
    private static Object scope;
    private static ProfileKey root;
    private static long remaining = -1;

    public static synchronized void capture(Object observedScope, CraftingJobEstimate estimate) {
        estimate.snapshot().ifPresent(snapshot -> {
            scope = observedScope;
            root = snapshot.root();
            remaining = snapshot.remainingAmounts().getOrDefault(root, -1L);
        });
    }

    public static synchronized boolean advanced(Object expectedScope, ProfileKey expectedRoot) {
        return scope == expectedScope && expectedRoot.equals(root) && remaining == 0;
    }
}
