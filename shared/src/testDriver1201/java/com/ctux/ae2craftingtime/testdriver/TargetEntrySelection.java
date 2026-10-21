package com.ctux.ae2craftingtime.testdriver;

import java.util.function.Function;
import java.util.function.Predicate;

/** Selects a craftable terminal entry, preserving exact component identity when supplied. */
final class TargetEntrySelection {
    private TargetEntrySelection() {}

    static <E, K> E firstCraftable(Iterable<E> entries, Function<E, K> keyOf,
            Predicate<E> craftable, K exactKey, String outputId, Function<K, String> idOf) {
        for (var entry : entries) {
            if (!craftable.test(entry)) continue;
            var key = keyOf.apply(entry);
            if (exactKey != null ? exactKey.equals(key) : outputId.equals(idOf.apply(key))) return entry;
        }
        return null;
    }
}
