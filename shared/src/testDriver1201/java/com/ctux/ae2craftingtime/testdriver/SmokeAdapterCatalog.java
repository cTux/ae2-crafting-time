package com.ctux.ae2craftingtime.testdriver;

import com.ctux.ae2craftingtime.core.IntegrationCatalog;
import java.util.LinkedHashMap;
import java.util.Map;

/** Host preflight reads the exact catalogue packaged with the candidate artifacts. */
public final class SmokeAdapterCatalog {
    private SmokeAdapterCatalog() {}

    static Map<String, String> newest(String target) {
        var result = new LinkedHashMap<String, String>();
        for (var candidate : IntegrationCatalog.CANDIDATES) {
            if (candidate.targets().contains(target)) {
                result.putIfAbsent(candidate.dependency(), candidate.variant());
            }
        }
        return result;
    }

    public static void main(String[] arguments) {
        if (arguments.length != 1) throw new IllegalArgumentException("Expected one target");
        newest(arguments[0]).forEach((dependency, variant) -> System.out.println(dependency + "\t" + variant));
    }
}
