package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import java.util.Set;

public interface PlanRecurrence {
    Set<AEKey> ae2craftingtime$recurrentKeys();
    void ae2craftingtime$recurrentKeys(Set<AEKey> keys);
}
