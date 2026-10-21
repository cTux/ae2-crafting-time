package com.ctux.ae2craftingtime.testdriver;

import java.math.BigInteger;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Function;

/** Independent raw native-plan quantity used only by the smoke observer. */
public final class NativePlanExpectation {
    public static <K> OptionalLong forOutput(Map<K, BigInteger> planned, String outputId, Function<K, String> id) {
        BigInteger expected = BigInteger.ZERO;
        for (var entry : planned.entrySet()) {
            if (outputId.equals(id.apply(entry.getKey()))) expected = expected.add(entry.getValue());
        }
        try { return OptionalLong.of(expected.longValueExact()); }
        catch (ArithmeticException overflow) { return OptionalLong.empty(); }
    }

    private NativePlanExpectation() { }
}
