package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class CpuTtcRequestHandlerTest {
    @Test
    void resolvesOnlyCpusThatAreBothInTheAuthoritativeMenuAndCurrentGridMembership() {
        var values = CpuTtcRequestHandler.resolveListed(List.of(1, 2, 3, 4),
                Map.of("listed-live", 1, "listed-foreign", 2, "unlisted-live", 3, "idle", 4),
                List.of("listed-live", "listed-foreign", "idle"),
                List.of("listed-live", "unlisted-live", "idle"),
                cpu -> !cpu.equals("idle"), cpu -> OptionalLong.of(cpu.length()));

        assertEquals(OptionalLong.of("listed-live".length()), values.get(0).seconds());
        assertEquals(OptionalLong.empty(), values.get(1).seconds());
        assertEquals(OptionalLong.empty(), values.get(2).seconds());
        assertEquals(OptionalLong.empty(), values.get(3).seconds());
    }
}
