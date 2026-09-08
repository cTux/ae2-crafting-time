package com.ctux.ae2craftingtime.testdriver.mixin;

import com.ctux.ae2craftingtime.core.ProfileKey;
import com.ctux.ae2craftingtime.mc1201.ClientStats;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ClientStats.class, remap = false)
public interface ClientStatsAccessor {
    @Accessor("NETWORK_AMOUNTS")
    static Map<ProfileKey, Long> ae2craftingtime_test_driver$networkAmounts() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
