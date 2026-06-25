package com.ctux.ae2craftingtime.core;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NetworkScopedStatsSupportTest {
    @Test
    void profilerBridgeDoesNotUseLegacyOutputOnlyFallback() throws IOException {
        var bridge = Files.readString(Path.of(
                "src/mc1201/java/com/ctux/ae2craftingtime/mc1201/ProfilerBridge.java"));

        assertFalse(bridge.contains("return PROFILER.stats(new ProfileKey(key.outputId()));"));
        assertFalse(bridge.contains("PROFILER.clearSamples(new ProfileKey(key.outputId()))"));
    }
}
