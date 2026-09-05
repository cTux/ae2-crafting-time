package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProviderResyncTest {
    @Test
    void dimensionFromNetworkIdSplitsControllerSuffix() {
        assertEquals("minecraft:overworld",
                ProfilerBridge.dimensionFromNetworkId("minecraft:overworld|1,2,3"));
        assertEquals("minecraft:the_nether", ProfilerBridge.dimensionFromNetworkId("minecraft:the_nether"));
        assertEquals("", ProfilerBridge.dimensionFromNetworkId(""));
        assertEquals("", ProfilerBridge.dimensionFromNetworkId(null));
    }
}
