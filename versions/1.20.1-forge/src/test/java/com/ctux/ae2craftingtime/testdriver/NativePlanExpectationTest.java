package com.ctux.ae2craftingtime.testdriver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class NativePlanExpectationTest {
    private record Key(String id, String variant) { }

    @Test void exactNativeOutputsAreIndependentOfTheLegacyPlanProjection() {
        var nativePlan = Map.of(new Key("minecraft:glass", "a"), BigInteger.ONE,
                new Key("minecraft:glass", "b"), BigInteger.valueOf(2),
                new Key("minecraft:sand", "a"), BigInteger.valueOf(4));
        assertEquals(OptionalLong.of(3L), NativePlanExpectation.forOutput(nativePlan, "minecraft:glass", Key::id));
        assertEquals(OptionalLong.of(0L), NativePlanExpectation.forOutput(nativePlan, "minecraft:stone", Key::id));
        assertEquals(OptionalLong.empty(), NativePlanExpectation.forOutput(Map.of(
                new Key("minecraft:glass", "a"), BigInteger.valueOf(Long.MAX_VALUE),
                new Key("minecraft:glass", "b"), BigInteger.ONE), "minecraft:glass", Key::id));
    }
}
