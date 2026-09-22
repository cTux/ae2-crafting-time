package com.ctux.ae2craftingtime.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Compares exact item identities against their registered item identities. */
public final class PlanStoredVariantDetector {
    public record Stored<K, P>(K exact, P primary, long amount) {}
    public record Missing<K, P>(K exact, P primary, long amount) {}

    public static <K, P> Set<Integer> detect(List<Missing<K, P>> rows, List<Stored<K, P>> available) {
        var exact = new HashSet<K>();
        var primary = new HashSet<P>();
        for (var stack : available) {
            if (stack.amount() > 0) {
                exact.add(stack.exact());
                primary.add(stack.primary());
            }
        }
        var result = new HashSet<Integer>();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            if (row != null && row.amount() > 0 && primary.contains(row.primary()) && !exact.contains(row.exact())) {
                result.add(i);
            }
        }
        return Set.copyOf(result);
    }

    private PlanStoredVariantDetector() {}
}
