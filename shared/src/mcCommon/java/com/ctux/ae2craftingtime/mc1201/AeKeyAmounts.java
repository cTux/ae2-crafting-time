package com.ctux.ae2craftingtime.mc1201;

import appeng.api.stacks.AEKey;
import com.ctux.ae2craftingtime.core.ProfileAmounts;
import com.ctux.ae2craftingtime.core.ProfileUnit;

public final class AeKeyAmounts {
    public static long normalize(AEKey key, long amount) {
        return ProfileAmounts.normalize(key.getId().toString(), key.getAmountPerUnit(), amount);
    }

    public static ProfileUnit unit(AEKey key) {
        return ProfileAmounts.unit(key.getId().toString(), key.getAmountPerUnit());
    }

    private AeKeyAmounts() {
    }
}
