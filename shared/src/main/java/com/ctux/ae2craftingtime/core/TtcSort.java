package com.ctux.ae2craftingtime.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.Function;

public final class TtcSort {
    public static <T> List<T> copySorted(List<T> entries, Function<T, OptionalLong> seconds, Comparator<T> fallback,
            boolean descending) {
        var sorted = new ArrayList<>(entries);
        sorted.sort((left, right) -> compare(left, right, seconds, fallback, descending));
        return sorted;
    }

    private static <T> int compare(T left, T right, Function<T, OptionalLong> seconds, Comparator<T> fallback,
            boolean descending) {
        var leftSeconds = seconds.apply(left);
        var rightSeconds = seconds.apply(right);
        if (leftSeconds.isPresent() && rightSeconds.isPresent()) {
            var result = Long.compare(leftSeconds.getAsLong(), rightSeconds.getAsLong());
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
