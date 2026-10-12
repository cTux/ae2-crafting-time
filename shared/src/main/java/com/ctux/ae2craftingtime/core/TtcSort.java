package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TtcSort {
    public static <T> List<T> copySorted(List<T> entries, Function<T, Optional<TtcEstimate>> estimate, Comparator<T> fallback,
            boolean descending) {
        var sorted = new ArrayList<>(entries);
        sorted.sort((left, right) -> compare(left, right, estimate, fallback, descending));
        return sorted;
    }

    public static <T> List<T> copyPrioritizedSorted(List<T> entries, Predicate<T> priority,
            Function<T, Optional<TtcEstimate>> estimate, Comparator<T> fallback, boolean sortByTtc, boolean descending) {
        var sorted = new ArrayList<>(entries);
        sorted.sort((left, right) -> {
            var priorityResult = Boolean.compare(priority.test(right), priority.test(left));
            return priorityResult != 0 || !sortByTtc ? priorityResult
                    : compare(left, right, estimate, fallback, descending);
        });
        return sorted;
    }

    private static <T> int compare(T left, T right, Function<T, Optional<TtcEstimate>> estimate, Comparator<T> fallback,
            boolean descending) {
        var leftSeconds = estimate.apply(left);
        var rightSeconds = estimate.apply(right);
        if (leftSeconds.isPresent() && rightSeconds.isPresent()) {
            var result = Double.compare(leftSeconds.get().durationMs(), rightSeconds.get().durationMs());
            if (result != 0) {
                return descending ? -result : result;
            }
            return fallback.compare(left, right);
        }
        if (leftSeconds.isPresent()) {
            return -1;
        }
        if (rightSeconds.isPresent()) {
            return 1;
        }
        return 0;
    }

    private TtcSort() {
    }
}
