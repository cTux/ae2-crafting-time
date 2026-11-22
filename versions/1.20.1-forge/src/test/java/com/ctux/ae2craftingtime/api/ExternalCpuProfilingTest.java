package com.ctux.ae2craftingtime.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import java.lang.reflect.Modifier;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class ExternalCpuProfilingTest {
    @Test
    void upstreamReflectionContractResolvesWithoutNeoEco() throws Exception {
        var api = Class.forName("com.ctux.ae2craftingtime.api.ExternalCpuProfiling");
        assertSignature(api, "startJob", void.class, ICraftingCPU.class, IGrid.class, ICraftingPlan.class,
                IActionSource.class, long.class);
        assertSignature(api, "expectOutput", void.class, ICraftingCPU.class, IGrid.class, AEKey.class,
                long.class, long.class);
        assertSignature(api, "acceptOutput", void.class, ICraftingCPU.class, IGrid.class, AEKey.class,
                long.class, long.class);
        assertSignature(api, "finishJob", void.class, ICraftingCPU.class, boolean.class, long.class);
        assertSignature(api, "setSuspended", void.class, ICraftingCPU.class, boolean.class, long.class);
        assertSignature(api, "remainingMillis", OptionalDouble.class, ICraftingCPU.class);
        assertEquals(OptionalDouble.empty(), ExternalCpuProfiling.remainingMillis(null));
    }

    private static void assertSignature(Class<?> api, String name, Class<?> result, Class<?>... parameters)
            throws NoSuchMethodException {
        var method = api.getMethod(name, parameters);
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertEquals(result, method.getReturnType());
    }
}
