package com.ctux.ae2craftingtime.core;

import java.util.Set;

public final class RecurrentMissing {
    public static <P, E> void clearPlan(P plan,
            java.util.function.Function<P, ? extends Iterable<E>> entries,
            java.util.function.Consumer<E> clear) {
        if (plan == null) return;
        entries.apply(plan).forEach(clear);
    }

    public static <T> Set<T> finalKeys(boolean simulation, Set<T> observed, Set<T> positiveMissing) {
        if (!simulation || observed.isEmpty() || positiveMissing.isEmpty()) return Set.of();
        var result = new java.util.HashSet<>(observed);
        result.retainAll(positiveMissing);
        return Set.copyOf(result);
    }

    public static boolean record(boolean rejectedCandidate, boolean eligibleChild, long missingAmount) {
        return rejectedCandidate && !eligibleChild && missingAmount > 0;
    }

    private RecurrentMissing() {
    }
}
