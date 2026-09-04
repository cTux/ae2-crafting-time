package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side only. Reads the stuck-output rows of a crafting CPU logic
 * through the AE2-mirrored status methods every CPU logic in the mod
 * supports, without naming addon-owned logic classes: a CPU that cannot
 * store items reports the stored outputs it no longer waits for, mirroring
 * the client's NO SPACE row predicate.
 */
public final class NoSpaceProbe {
    private static final Map<Class<?>, Methods> CACHE = new ConcurrentHashMap<>();

    private record Methods(Method cantStore, Method allWaiting, Method stored, Method waitingFor) {
    }

    public static List<AEKey> stuckKeys(Object logic) {
        var methods = methods(logic);
        if (methods == null) {
            return List.of();
        }
        try {
            if (!((Boolean) methods.cantStore().invoke(logic)).booleanValue()) {
                return List.of();
            }
            var waiting = new HashSet<AEKey>();
            methods.allWaiting().invoke(logic, waiting);
            var stuck = new ArrayList<AEKey>();
            for (var key : waiting) {
                if (key == null) {
                    continue;
                }
                var stored = ((Number) methods.stored().invoke(logic, key)).longValue();
                var outstanding = ((Number) methods.waitingFor().invoke(logic, key)).longValue();
                if (stored > 0 && outstanding == 0 && !stuck.contains(key)) {
                    stuck.add(key);
                }
            }
            return List.copyOf(stuck);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return List.of();
        }
    }

    private static Methods methods(Object logic) {
        if (logic == null) {
            return null;
        }
        var type = logic.getClass();
        var cached = CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        Methods methods = null;
        try {
            methods = new Methods(type.getMethod("isCantStoreItems"), type.getMethod("getAllWaitingFor", Set.class),
                    type.getMethod("getStored", AEKey.class), type.getMethod("getWaitingFor", AEKey.class));
        } catch (NoSuchMethodException ignored) {
            // Logics without the status methods simply never report NO SPACE.
        }
        if (methods != null) {
            CACHE.put(type, methods);
        }
        return methods;
    }

    private NoSpaceProbe() {
    }
}
