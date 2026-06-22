package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppliedMekanisticsSupportTest {
    @Test
    void profilerNormalizesBucketStyleKeysWithoutLoadingAppMek() throws IOException {
        var bridge = Files.readString(Path.of(
                "src/mc1201/java/com/ctux/ae2craftingtime/mc1201/ProfilerBridge.java"));

        assertTrue(bridge.contains("AeKeyAmounts.normalize(key, amount)"));
        assertTrue(bridge.contains("key.getAmountPerUnit() > 1"));
        assertFalse(bridge.contains("AEKeyType.fluids()"));
        assertFalse(bridge.contains("me.ramidzkh.mekae2"));
    }
}
