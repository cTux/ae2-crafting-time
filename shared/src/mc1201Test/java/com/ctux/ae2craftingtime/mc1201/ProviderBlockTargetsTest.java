package com.ctux.ae2craftingtime.mc1201;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

/**
 * Broken-provider validation: replacement block entities and surviving hosts
 * without provider service drop, while unloaded or unreadable grid stays
 * unknown and keeps the highlight so reload never clears intact red.
 */
class ProviderBlockTargetsTest {
    @Test
    void nonProviderBlockEntityDrops() {
        assertFalse(ProviderBlockTargets.keepHost(null));
        assertFalse(ProviderBlockTargets.keepHost(new Object()));
    }

    @Test
    void providerHostKeeps() {
        var provider = stubProvider();
        var node = stubNode(provider);
        assertTrue(ProviderBlockTargets.keepHost(stubHost(node)));
    }

    @Test
    void survivingHostWithoutProviderServiceDrops() {
        var node = stubNode(null);
        assertFalse(ProviderBlockTargets.keepHost(stubHost(node)));
    }

    @Test
    void unknownGridKeepsInsteadOfClearingRed() {
        assertTrue(ProviderBlockTargets.keepHost(stubHost(null)));
        assertTrue(ProviderBlockTargets.keepHost(throwingHost()));
    }

    @Test
    void keepForHighlightRejectsNullsWithoutThrowing() {
        assertFalse(ProviderBlockTargets.keepForHighlight(null, new BlockPos(0, 0, 0)));
        assertFalse(ProviderBlockTargets.keepForHighlight(null, null));
    }

    private static IInWorldGridNodeHost stubHost(IGridNode node) {
        return (IInWorldGridNodeHost) Proxy.newProxyInstance(
                ProviderBlockTargetsTest.class.getClassLoader(),
                new Class<?>[] {IInWorldGridNodeHost.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getGridNode")) {
                        return node;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static IInWorldGridNodeHost throwingHost() {
        return (IInWorldGridNodeHost) Proxy.newProxyInstance(
                ProviderBlockTargetsTest.class.getClassLoader(),
                new Class<?>[] {IInWorldGridNodeHost.class},
                (proxy, method, args) -> {
                    throw new RuntimeException("unreadable grid");
                });
    }

    private static IGridNode stubNode(Object service) {
        return (IGridNode) Proxy.newProxyInstance(
                ProviderBlockTargetsTest.class.getClassLoader(),
                new Class<?>[] {IGridNode.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getService")) {
                        return service;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static ICraftingProvider stubProvider() {
        return (ICraftingProvider) Proxy.newProxyInstance(
                ProviderBlockTargetsTest.class.getClassLoader(),
                new Class<?>[] {ICraftingProvider.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class || type == short.class || type == int.class || type == long.class
                || type == float.class || type == double.class || type == char.class) {
            return 0;
        }
        if (type == void.class) {
            return null;
        }
        return null;
    }
}
