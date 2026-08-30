package com.ctux.ae2craftingtime.testdriver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class SortObservation {
    public static boolean valid(List<String> ae2Order, List<String> ascending, List<String> descending,
            List<String> ascendingKnown, List<String> descendingKnown) {
        if (ae2Order.isEmpty() || ascendingKnown.isEmpty() || ascending.size() != descending.size()
                || ascendingKnown.size() > ascending.size() || descendingKnown.size() > descending.size()
                || !new HashSet<>(ascending).equals(new HashSet<>(descending))
                || !new HashSet<>(ascending).equals(new HashSet<>(ae2Order))) {
            return false;
        }
        var reversed = new ArrayList<>(ascendingKnown);
        java.util.Collections.reverse(reversed);
        var known = new HashSet<>(ascendingKnown);
        var ascendingUnknown = ascending.stream().filter(id -> !known.contains(id)).toList();
        var descendingUnknown = descending.stream().filter(id -> !known.contains(id)).toList();
        return reversed.equals(descendingKnown) && ascending.subList(0, ascendingKnown.size()).equals(ascendingKnown)
                && descending.subList(0, descendingKnown.size()).equals(descendingKnown)
                && ascendingUnknown.equals(descendingUnknown);
    }

    private SortObservation() {
    }
}
