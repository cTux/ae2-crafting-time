package com.ctux.ae2craftingtime.mc1201.mixin;

import appeng.api.stacks.AEKey;
import com.ctux.ae2craftingtime.mc1201.ProfilerBridge;

final class AddonCpuProfilingContext {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    static void enter(String networkId, Object scope, long tick) {
        CURRENT.set(new Context(networkId, scope, tick));
    }

    static void start(AEKey what, long amount) {
        var context = CURRENT.get();
        if (context != null && amount > 0) {
            ProfilerBridge.start(context.networkId(), context.scope(), what, amount, context.tick());
        }
    }

    static void exit() {
        CURRENT.remove();
    }

    private record Context(String networkId, Object scope, long tick) {
    }

    private AddonCpuProfilingContext() {
    }
}
