package com.ctux.ae2craftingtime.core;

import java.util.Collection;

/** A missing live candidate permits a saved fallback; a conflict does not. */
public record DisplayKeySelection<T>(boolean observed, T key) {
    public T orFallback(T saved) {
        return observed ? key : saved;
    }

    public static <T> DisplayKeySelection<T> from(Collection<T> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return new DisplayKeySelection<>(false, null);
        }
        T selected = null;
        for (var candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (selected != null && !selected.equals(candidate)) {
                return new DisplayKeySelection<>(true, null);
            }
            selected = candidate;
        }
        return new DisplayKeySelection<>(selected != null, selected);
    }
}
