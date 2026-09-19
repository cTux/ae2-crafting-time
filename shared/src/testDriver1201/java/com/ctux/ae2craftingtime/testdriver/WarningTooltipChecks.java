package com.ctux.ae2craftingtime.testdriver;

import java.util.ArrayList;
import java.util.List;

final class WarningTooltipChecks {
    private static final List<String> CONTROLS = List.of(
            "text.ae2craftingtime.locate_hint",
            "text.ae2craftingtime.details_hint",
            "text.ae2craftingtime.reset_hint");

    static boolean hasBodyAndControls(List<UiSnapshot.ObservedText> tooltip, List<String> body) {
        var expected = new ArrayList<>(body);
        expected.addAll(CONTROLS);
        var keys = tooltip.stream().map(UiSnapshot.ObservedText::key).toList();
        if (keys.size() < expected.size()
                || !keys.subList(keys.size() - expected.size(), keys.size()).equals(expected)) {
            return false;
        }
        return hasControlKeys(keys);
    }

    static boolean hasControls(List<UiSnapshot.ObservedText> tooltip) {
        return hasControlKeys(tooltip.stream().map(UiSnapshot.ObservedText::key).toList());
    }

    private static boolean hasControlKeys(List<String> keys) {
        if (keys.size() < CONTROLS.size()
                || !keys.subList(keys.size() - CONTROLS.size(), keys.size()).equals(CONTROLS)) {
            return false;
        }
        return CONTROLS.stream().allMatch(control -> keys.stream().filter(control::equals).count() == 1);
    }

    private WarningTooltipChecks() {
    }
}
