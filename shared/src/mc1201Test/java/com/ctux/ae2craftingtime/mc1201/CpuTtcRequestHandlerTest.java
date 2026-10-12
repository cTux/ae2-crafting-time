package com.ctux.ae2craftingtime.mc1201;

import com.ctux.ae2craftingtime.core.TtcEstimate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CpuTtcRequestHandlerTest {
    @Test
    void resolvesOnlyCpusThatAreBothInTheAuthoritativeMenuAndCurrentGridMembership() {
        var values = CpuTtcRequestHandler.resolveListed(List.of(1, 2, 3, 4),
                Map.of("listed-live", 1, "listed-foreign", 2, "unlisted-live", 3, "idle", 4),
                List.of("listed-live", "listed-foreign", "idle"),
                List.of("listed-live", "unlisted-live", "idle"),
                cpu -> !cpu.equals("idle"), cpu -> Optional.of(new TtcEstimate(cpu.length(), true)));

        assertEquals(Optional.of(new TtcEstimate("listed-live".length(), true)), values.get(0).estimate());
        assertEquals(Optional.empty(), values.get(1).estimate());
        assertEquals(Optional.empty(), values.get(2).estimate());
        assertEquals(Optional.empty(), values.get(3).estimate());
    }
}
