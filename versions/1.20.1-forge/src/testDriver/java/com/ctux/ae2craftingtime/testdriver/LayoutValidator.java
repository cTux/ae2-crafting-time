package com.ctux.ae2craftingtime.testdriver;

import java.util.ArrayList;
import java.util.List;

public final class LayoutValidator {
    public static List<String> validate(UiSnapshot snapshot) {
        var failures = new ArrayList<String>();
        for (var text : snapshot.text()) {
            check("text " + text.key(), text.bounds(), snapshot, failures);
        }
        for (var badge : snapshot.badges()) {
            check("badge", badge, snapshot, failures);
        }
        return List.copyOf(failures);
    }

    private static void check(String name, Rect candidate, UiSnapshot snapshot, List<String> failures) {
        if (!candidate.inside(snapshot.gui())) {
            failures.add(name + " outside GUI");
        }
        if (snapshot.itemCells().stream().anyMatch(candidate::overlaps)) {
            failures.add(name + " overlaps item cell");
        }
        if (snapshot.widgets().stream().map(UiSnapshot.Widget::bounds).anyMatch(candidate::overlaps)) {
            failures.add(name + " overlaps widget");
        }
    }

    private LayoutValidator() {
    }
}
