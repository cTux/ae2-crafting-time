package com.ctux.ae2craftingtime.mc1201;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;

/** Dispatch variants report to the lifecycle mixin's single network-aware state. */
public interface NeoEcoDispatchObserver {
    void ae2craftingtime$dispatched(AEKey key, long amount, Actionable mode);
}
