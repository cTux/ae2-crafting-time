package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import appeng.api.networking.crafting.ICraftingPlan;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TrinityNativeJobAccessTest {
    @Test void legacyPlansKeepTheirExistingProfilerEntryWithoutReadingTrinityStages() {
        var legacy = (ICraftingPlan) Proxy.newProxyInstance(ICraftingPlan.class.getClassLoader(),
                new Class<?>[] { ICraftingPlan.class }, (proxy, method, arguments) -> {
                    throw new AssertionError("legacy plan must not call a Trinity accessor");
                });
        assertFalse(TrinityNativeJobAccess.isTrinityPlan(null));
        assertFalse(TrinityNativeJobAccess.isTrinityPlan(legacy));
        assertEquals(Map.of(), TrinityNativeJobAccess.exactPlannedOutputs(legacy));
        assertTrue(TrinityNativeJobAccess.isTrinityPlanClassName(
                "com.fish_dan_.data_energistics.common.crafting.trinity.planning.plan.TrinityCraftingPlan"));
        assertFalse(TrinityNativeJobAccess.isTrinityPlanClassName("example.LegacyPlan"));
    }
}
